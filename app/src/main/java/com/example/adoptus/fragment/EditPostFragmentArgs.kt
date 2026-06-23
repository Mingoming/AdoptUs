package com.example.adoptus.fragment

import android.os.Bundle
import androidx.navigation.NavArgs

data class EditPostFragmentArgs(val postId: String) : NavArgs {
    fun toBundle(): Bundle {
        val result = Bundle()
        result.putString("postId", postId)
        return result
    }

    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): EditPostFragmentArgs {
            return EditPostFragmentArgs(
                postId = bundle.getString("postId") ?: ""
            )
        }
    }
}
