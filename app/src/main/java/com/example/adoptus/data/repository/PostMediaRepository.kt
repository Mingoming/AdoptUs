package com.example.adoptus.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.adoptus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UploadedPostMedia(
    val path: String,
    val publicUrl: String,
    val mediaType: String
)

object PostMediaUploadPolicy {
    const val MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024
    const val MAX_VIDEO_SIZE_BYTES = 20L * 1024 * 1024

    private val supportedMimeTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "video/mp4"
    )

    fun isSupportedMimeType(mimeType: String): Boolean =
        mimeType in supportedMimeTypes

    fun isAllowedSize(mimeType: String, size: Long): Boolean {
        val maxSize = if (mimeType == "video/mp4") {
            MAX_VIDEO_SIZE_BYTES
        } else {
            MAX_IMAGE_SIZE_BYTES
        }
        return size in 1..maxSize
    }

    fun buildPath(uid: String, timestamp: Long, mimeType: String): String {
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            else -> "jpg"
        }
        return "posts/$uid/$timestamp.$extension"
    }

    fun mediaTypeFor(mimeType: String): String =
        if (mimeType == "video/mp4") "video" else "image"
}

class PostMediaRepository {

    suspend fun uploadMedia(
        contentResolver: ContentResolver,
        mediaUri: Uri,
        uid: String
    ): Result<UploadedPostMedia> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            check(baseUrl.isNotBlank() && publishableKey.isNotBlank()) {
                "Supabase configuration is missing"
            }

            val mimeType = contentResolver.getType(mediaUri)
                ?: throw IllegalArgumentException("Unable to detect media type")
            require(PostMediaUploadPolicy.isSupportedMimeType(mimeType)) {
                "Only JPEG, PNG, WebP, and MP4 files are supported"
            }

            val maxSize = if (mimeType == "video/mp4") {
                PostMediaUploadPolicy.MAX_VIDEO_SIZE_BYTES
            } else {
                PostMediaUploadPolicy.MAX_IMAGE_SIZE_BYTES
            }

            val bytes = contentResolver.openInputStream(mediaUri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0L

                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    totalBytes += read
                    if (totalBytes > maxSize) {
                        val limit = if (mimeType == "video/mp4") "20 MB" else "5 MB"
                        throw IllegalArgumentException("Media must be $limit or smaller")
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            } ?: throw IllegalArgumentException("Unable to read selected media")

            require(PostMediaUploadPolicy.isAllowedSize(mimeType, bytes.size.toLong())) {
                "Selected media has an invalid size"
            }

            val path = PostMediaUploadPolicy.buildPath(
                uid = uid,
                timestamp = System.currentTimeMillis(),
                mimeType = mimeType
            )
            val uploadUrl = "$baseUrl/storage/v1/object/$BUCKET_NAME/$path"

            openConnection(uploadUrl, "POST", publishableKey).useConnection { connection ->
                connection.setRequestProperty("Content-Type", mimeType)
                connection.setRequestProperty("x-upsert", "false")
                connection.doOutput = true
                connection.outputStream.use { it.write(bytes) }
                connection.requireSuccess("Media upload failed")
            }

            UploadedPostMedia(
                path = path,
                publicUrl = "$baseUrl/storage/v1/object/public/$BUCKET_NAME/$path",
                mediaType = PostMediaUploadPolicy.mediaTypeFor(mimeType)
            )
        }
    }

    suspend fun deleteMedia(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            check(baseUrl.isNotBlank() && publishableKey.isNotBlank()) {
                "Supabase configuration is missing"
            }

            val deleteUrl = "$baseUrl/storage/v1/object/$BUCKET_NAME/$path"
            openConnection(deleteUrl, "DELETE", publishableKey).useConnection { connection ->
                connection.requireSuccess("Media cleanup failed")
            }
        }
    }

    private fun openConnection(
        url: String,
        method: String,
        publishableKey: String
    ): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("apikey", publishableKey)
        }

    private fun HttpURLConnection.requireSuccess(message: String) {
        val responseBody = (if (responseCode in 200..299) inputStream else errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (responseCode !in 200..299) {
            throw IllegalStateException("$message ($responseCode): $responseBody")
        }
    }

    private inline fun <T> HttpURLConnection.useConnection(
        block: (HttpURLConnection) -> T
    ): T = try {
        block(this)
    } finally {
        disconnect()
    }

    private companion object {
        const val BUCKET_NAME = "adoptus-post-images"
    }
}
