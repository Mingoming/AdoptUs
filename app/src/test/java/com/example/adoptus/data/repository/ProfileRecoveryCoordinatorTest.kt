package com.example.adoptus.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRecoveryCoordinatorTest {

    @Test
    fun recoveryCreatesDefaultsOnlyWhenDocumentIsMissing() {
        val profile = profileForRecovery(
            documentExists = false,
            profileFactory = { mapOf("role" to "user") }
        )

        assertEquals(mapOf("role" to "user"), profile)
    }

    @Test
    fun recoveryPreservesDocumentThatAlreadyExists() {
        val profile = profileForRecovery(
            documentExists = true,
            profileFactory = { error("existing profile must not be replaced") }
        )

        assertNull(profile)
    }
}
