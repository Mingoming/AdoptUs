package com.example.adoptus.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.adoptus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UploadedPostImage(
    val path: String,
    val publicUrl: String
)

object PostImageUploadPolicy {
    const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024

    private val supportedMimeTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    fun isSupportedMimeType(mimeType: String): Boolean =
        mimeType in supportedMimeTypes

    fun isAllowedSize(size: Long): Boolean =
        size in 1..MAX_FILE_SIZE_BYTES

    fun buildPath(uid: String, timestamp: Long, mimeType: String): String {
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "posts/$uid/$timestamp.$extension"
    }
}

class PostImageRepository {

    suspend fun uploadImage(
        contentResolver: ContentResolver,
        imageUri: Uri,
        uid: String
    ): Result<UploadedPostImage> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            check(baseUrl.isNotBlank() && publishableKey.isNotBlank()) {
                "Supabase configuration is missing"
            }

            val mimeType = contentResolver.getType(imageUri)
                ?: throw IllegalArgumentException("Unable to detect image type")
            require(PostImageUploadPolicy.isSupportedMimeType(mimeType)) {
                "Only JPEG, PNG, and WebP images are supported"
            }

            val bytes = contentResolver.openInputStream(imageUri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0L

                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    totalBytes += read
                    if (totalBytes > PostImageUploadPolicy.MAX_FILE_SIZE_BYTES) {
                        throw IllegalArgumentException("Image must be 5 MB or smaller")
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            } ?: throw IllegalArgumentException("Unable to read selected image")

            require(PostImageUploadPolicy.isAllowedSize(bytes.size.toLong())) {
                "Image must be 5 MB or smaller"
            }

            val path = PostImageUploadPolicy.buildPath(
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
                connection.requireSuccess("Image upload failed")
            }

            UploadedPostImage(
                path = path,
                publicUrl = "$baseUrl/storage/v1/object/public/$BUCKET_NAME/$path"
            )
        }
    }

    suspend fun deleteImage(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            check(baseUrl.isNotBlank() && publishableKey.isNotBlank()) {
                "Supabase configuration is missing"
            }

            val deleteUrl = "$baseUrl/storage/v1/object/$BUCKET_NAME/$path"
            openConnection(deleteUrl, "DELETE", publishableKey).useConnection { connection ->
                connection.requireSuccess("Image cleanup failed")
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
