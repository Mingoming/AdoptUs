package com.example.adoptus.data.model

import com.google.firebase.Timestamp

data class Post(
    val postId: String = "",
    val userId: String = "",

    // Info hewan
    val petName: String = "",
    val petType: String = "",
    val breed: String = "",
    val age: Int = 0,
    val ageUnit: String = "Months",     // "Months" atau "Years"
    val city: String = "",
    val description: String = "",

    // Media
    val mediaUrl: String = "",
    val mediaType: String = "image",    // "image" atau "video"

    // Detail tambahan
    val isVaccinated: Boolean = false,
    val hasHealthPassport: Boolean = false,
    val adoptionFee: Int = 0,           // 0 = gratis

    // Status adopsi
    val status: String = "available",   // "available" | "pending" | "adopted"

    // Engagement
    val likesCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,

    // Embedded Owner info (untuk optimasi N+1 query)
    val ownerUsername: String = "",
    val ownerPhotoUrl: String = "",
    val ownerWhatsapp: String = "",

    val createdAt: Timestamp? = null
) {
    // Konversi dari Firestore document ke Post object
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Post {
            return Post(
                postId            = id,
                userId            = map["userId"] as? String ?: "",
                petName           = map["petName"] as? String ?: "",
                petType           = map["petType"] as? String ?: "",
                breed             = map["breed"] as? String ?: "",
                age               = (map["age"] as? Long)?.toInt() ?: 0,
                ageUnit           = map["ageUnit"] as? String ?: "Months",
                city              = map["city"] as? String ?: "",
                description       = map["description"] as? String ?: "",
                mediaUrl          = map["mediaUrl"] as? String ?: "",
                mediaType         = map["mediaType"] as? String ?: "image",
                isVaccinated      = map["isVaccinated"] as? Boolean ?: false,
                hasHealthPassport = map["hasHealthPassport"] as? Boolean ?: false,
                adoptionFee       = (map["adoptionFee"] as? Long)?.toInt() ?: 0,
                status            = map["status"] as? String ?: "available",
                likesCount        = (map["likesCount"] as? Long)?.toInt() ?: 0,
                isLikedByCurrentUser = map["isLikedByCurrentUser"] as? Boolean ?: false,
                ownerUsername     = map["ownerUsername"] as? String ?: "",
                ownerPhotoUrl     = map["ownerPhotoUrl"] as? String ?: "",
                ownerWhatsapp     = map["ownerWhatsapp"] as? String ?: "",
                createdAt         = map["createdAt"] as? Timestamp
            )
        }
    }

    // Konversi Post ke Map untuk disimpan ke Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "postId"            to postId,
        "userId"            to userId,
        "petName"           to petName,
        "petType"           to petType,
        "breed"             to breed,
        "age"               to age,
        "ageUnit"           to ageUnit,
        "city"              to city,
        "description"       to description,
        "mediaUrl"          to mediaUrl,
        "mediaType"         to mediaType,
        "isVaccinated"      to isVaccinated,
        "hasHealthPassport" to hasHealthPassport,
        "adoptionFee"       to adoptionFee,
        "status"            to status,
        "likesCount"        to likesCount,
        "isLikedByCurrentUser" to isLikedByCurrentUser,
        "ownerUsername"     to ownerUsername,
        "ownerPhotoUrl"     to ownerPhotoUrl,
        "ownerWhatsapp"     to ownerWhatsapp,
        "createdAt"         to createdAt
    )

    // Helper display
    val ageDisplay: String get() = "$age $ageUnit"
    val isFree: Boolean get() = adoptionFee == 0
    val isAvailable: Boolean get() = status == "available"
}