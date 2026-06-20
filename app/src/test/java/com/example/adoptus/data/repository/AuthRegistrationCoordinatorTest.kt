package com.example.adoptus.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRegistrationCoordinatorTest {

    @Test
    fun registerRollsBackAuthUserWhenProfileWriteFails() = runBlocking {
        var rolledBackUser: String? = null

        val result = registerWithProfile(
            createAuthUser = { "uid-1" },
            writeProfile = { throw IllegalStateException("profile write failed") },
            rollbackAuthUser = { rolledBackUser = it }
        )

        assertTrue(result.isFailure)
        assertEquals("uid-1", rolledBackUser)
    }

    @Test
    fun registerReturnsUserWhenProfileWriteSucceeds() = runBlocking {
        var rolledBack = false

        val result = registerWithProfile(
            createAuthUser = { "uid-1" },
            writeProfile = {},
            rollbackAuthUser = { rolledBack = true }
        )

        assertEquals("uid-1", result.getOrThrow())
        assertEquals(false, rolledBack)
    }
}
