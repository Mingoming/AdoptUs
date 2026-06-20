package com.example.adoptus.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val city: String = "",
    val whatsapp: String = "",
    val role: String = "user",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val needsMigration: Boolean = false
) {
    companion object {
        private val invalidUsernameCharacters = Regex("[^a-z0-9_]")
        private val usernameWhitespace = Regex("\\s+")

        fun fromMap(documentId: String, map: Map<String, Any?>): User {
            val hasLegacyFields = LEGACY_FIELDS.any(map::containsKey)

            return User(
                uid = documentId,
                username = map["username"] as? String ?: "",
                fullName = firstNonBlank(
                    map["fullName"] as? String,
                    map["full_name"] as? String
                ),
                photoUrl = firstNonBlank(
                    map["photoUrl"] as? String,
                    map["photo_url"] as? String
                ),
                bio = map["bio"] as? String ?: "",
                city = map["city"] as? String ?: "",
                whatsapp = map["whatsapp"] as? String ?: "",
                role = map["role"] as? String ?: "user",
                createdAt = map["createdAt"] as? Timestamp
                    ?: map["created_at"] as? Timestamp,
                updatedAt = map["updatedAt"] as? Timestamp,
                needsMigration = hasLegacyFields
            )
        }

        fun newDocumentMap(
            uid: String,
            username: String,
            fullName: String,
            photoUrl: String,
            createdAt: Any,
            updatedAt: Any
        ): Map<String, Any> = mapOf(
            "uid" to uid,
            "username" to username,
            "fullName" to fullName,
            "photoUrl" to photoUrl,
            "bio" to "",
            "city" to "",
            "whatsapp" to "",
            "role" to "user",
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )

        fun profileUpdateMap(
            username: String,
            fullName: String,
            bio: String,
            city: String,
            whatsapp: String,
            updatedAt: Any
        ): Map<String, Any> = mapOf(
            "username" to username,
            "fullName" to fullName,
            "bio" to bio,
            "city" to city,
            "whatsapp" to whatsapp,
            "updatedAt" to updatedAt
        )

        fun normalizeUsername(value: String, uid: String): String {
            val normalized = value
                .trim()
                .lowercase()
                .replace(usernameWhitespace, "_")
                .replace(invalidUsernameCharacters, "")
                .take(30)

            return normalized.takeIf { it.length >= 3 }
                ?: "user_${uid.take(8)}"
        }

        fun isValidUsername(value: String): Boolean =
            value.length in 3..30 && value.none(Char::isWhitespace)

        fun validatedUsernameInput(value: String): String? =
            value.takeIf(::isValidUsername)

        private fun firstNonBlank(vararg values: String?): String =
            values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

        private val LEGACY_FIELDS = setOf(
            "id",
            "email",
            "full_name",
            "photo_url",
            "created_at"
        )
    }
}
