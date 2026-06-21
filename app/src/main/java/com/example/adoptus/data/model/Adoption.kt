package com.example.adoptus.data.model

import com.google.firebase.Timestamp

data class Adoption(
    val adoptionId: String = "",
    val postId: String = "",
    val petName: String = "",
    val adopterId: String = "",
    val adopterName: String = "",
    val ownerId: String = "",
    val status: String = "pending", // "pending" | "approved" | "rejected" | "cancelled"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Adoption {
            return Adoption(
                adoptionId   = id,
                postId       = map["postId"] as? String ?: "",
                petName      = map["petName"] as? String ?: "",
                adopterId    = map["adopterId"] as? String ?: "",
                adopterName  = map["adopterName"] as? String ?: "",
                ownerId      = map["ownerId"] as? String ?: "",
                status       = map["status"] as? String ?: "pending",
                createdAt    = map["createdAt"] as? Timestamp,
                updatedAt    = map["updatedAt"] as? Timestamp
            )
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "adoptionId"   to adoptionId,
        "postId"       to postId,
        "petName"      to petName,
        "adopterId"    to adopterId,
        "adopterName"  to adopterName,
        "ownerId"      to ownerId,
        "status"       to status,
        "createdAt"    to createdAt,
        "updatedAt"    to updatedAt
    )
}
