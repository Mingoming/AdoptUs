package com.example.adoptus.data.model

data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val city: String = "",
    val whatsapp: String = "",
    val role: String = "user",
    val createdAt: Any? = null,
    val email: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "username" to username,
        "fullName" to fullName,
        "photoUrl" to photoUrl,
        "bio" to bio,
        "city" to city,
        "whatsapp" to whatsapp,
        "role" to role,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(documentId: String, map: Map<String, Any?>): User {
            return User(
                id = documentId,
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
                createdAt = map["createdAt"] ?: map["created_at"],
                email = map["email"] as? String ?: ""
            )
        }

        fun isValidUsername(value: String): Boolean =
            USERNAME_PATTERN.matches(value)

        fun normalizeUsername(value: String, userId: String): String {
            val normalized = value
                .trim()
                .lowercase()
                .replace(Regex("\\s+"), "_")
                .take(30)

            return normalized.takeIf(::isValidUsername)
                ?: "user_${userId.take(8)}"
        }

        private fun firstNonBlank(vararg values: String?): String =
            values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

        private val USERNAME_PATTERN = Regex("^[A-Za-z0-9._]{3,30}$")
    }
}
