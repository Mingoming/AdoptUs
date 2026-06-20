package com.example.adoptus.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    @Test
    fun fromMapReadsCanonicalFields() {
        val user = User.fromMap(
            documentId = "u1",
            map = mapOf(
                "uid" to "u1",
                "username" to "milo_owner",
                "fullName" to "Milo Owner",
                "photoUrl" to "https://example.com/avatar.jpg",
                "bio" to "Pet foster",
                "city" to "Mataram",
                "whatsapp" to "628123456789",
                "role" to "user"
            )
        )

        assertEquals("u1", user.uid)
        assertEquals("Milo Owner", user.fullName)
        assertEquals("https://example.com/avatar.jpg", user.photoUrl)
        assertEquals("Mataram", user.city)
        assertFalse(user.needsMigration)
    }

    @Test
    fun fromMapFallsBackToLegacyFields() {
        val user = User.fromMap(
            documentId = "u1",
            map = mapOf(
                "id" to "u1",
                "username" to "legacy_owner",
                "full_name" to "Legacy Owner",
                "photo_url" to "legacy.jpg",
                "role" to "user"
            )
        )

        assertEquals("u1", user.uid)
        assertEquals("Legacy Owner", user.fullName)
        assertEquals("legacy.jpg", user.photoUrl)
        assertTrue(user.needsMigration)
    }

    @Test
    fun fromMapUsesDocumentIdAsUidSourceOfTruth() {
        val user = User.fromMap(
            documentId = "correct-uid",
            map = mapOf(
                "uid" to "spoofed-uid",
                "username" to "owner",
                "fullName" to "Owner"
            )
        )

        assertEquals("correct-uid", user.uid)
    }

    @Test
    fun fromMapUsesNonblankLegacyFallbackWhenCanonicalValueIsBlank() {
        val user = User.fromMap(
            documentId = "u1",
            map = mapOf(
                "username" to "legacy_owner",
                "fullName" to " ",
                "full_name" to "Legacy Owner",
                "photoUrl" to "",
                "photo_url" to "legacy.jpg"
            )
        )

        assertEquals("Legacy Owner", user.fullName)
        assertEquals("legacy.jpg", user.photoUrl)
    }

    @Test
    fun newDocumentMapWritesOnlyCanonicalSchema() {
        val createdAt = Any()
        val updatedAt = Any()

        val document = User.newDocumentMap(
            uid = "u1",
            username = "owner",
            fullName = "Owner Name",
            photoUrl = "",
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        assertEquals(
            setOf(
                "uid",
                "username",
                "fullName",
                "photoUrl",
                "bio",
                "city",
                "whatsapp",
                "role",
                "createdAt",
                "updatedAt"
            ),
            document.keys
        )
        assertEquals("u1", document["uid"])
        assertEquals(createdAt, document["createdAt"])
        assertFalse(document.containsKey("email"))
        assertFalse(document.containsKey("full_name"))
    }

    @Test
    fun normalizeUsernameProducesRuleCompatibleValue() {
        assertEquals("john_doe", User.normalizeUsername(" John Doe! ", "u12345678"))
        assertEquals("user_u1234567", User.normalizeUsername("@@", "u12345678"))
        assertFalse(User.isValidUsername("john\tdoe"))
        assertFalse(User.isValidUsername("john\ndoe"))
        assertTrue(User.isValidUsername("john_doe"))
    }

    @Test
    fun profileUpdateMapContainsOnlyFieldsEditedBySettings() {
        val updatedAt = Any()
        val document = User.profileUpdateMap(
            username = "owner",
            fullName = "Owner Name",
            bio = "Bio",
            city = "Mataram",
            whatsapp = "628123",
            updatedAt = updatedAt
        )

        assertEquals(
            setOf(
                "username",
                "fullName",
                "bio",
                "city",
                "whatsapp",
                "updatedAt"
            ),
            document.keys
        )
        assertEquals(updatedAt, document["updatedAt"])
        assertFalse(document.containsKey("uid"))
        assertFalse(document.containsKey("role"))
        assertFalse(document.containsKey("photoUrl"))
    }
}
