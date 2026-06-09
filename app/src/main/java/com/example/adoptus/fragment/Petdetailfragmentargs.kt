package com.example.adoptus.fragment

import android.os.Bundle
import androidx.navigation.NavArgs

data class PetDetailFragmentArgs(val postId: String) : NavArgs {
    fun toBundle(): Bundle {
        val result = Bundle()
        result.putString("postId", postId)
        return result
    }

    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): PetDetailFragmentArgs {
            return PetDetailFragmentArgs(
                postId = bundle.getString("postId") ?: ""
            )
        }
    }
}