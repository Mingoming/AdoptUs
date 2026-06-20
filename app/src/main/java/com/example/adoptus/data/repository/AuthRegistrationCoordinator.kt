package com.example.adoptus.data.repository

internal suspend fun <T> registerWithProfile(
    createAuthUser: suspend () -> T,
    writeProfile: suspend (T) -> Unit,
    rollbackAuthUser: suspend (T) -> Unit
): Result<T> {
    return try {
        val user = createAuthUser()
        try {
            writeProfile(user)
            Result.success(user)
        } catch (profileError: Exception) {
            try {
                rollbackAuthUser(user)
            } catch (rollbackError: Exception) {
                profileError.addSuppressed(rollbackError)
            }
            Result.failure(profileError)
        }
    } catch (authError: Exception) {
        Result.failure(authError)
    }
}
