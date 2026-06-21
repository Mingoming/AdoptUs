package com.example.adoptus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.adoptus.MainActivity
import com.example.adoptus.data.model.User
import com.example.adoptus.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar putih dengan ikon kontras
        window.statusBarColor = android.graphics.Color.WHITE
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString()
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validasi input — pesan error tampil di field, bukan toast
            if (fullName.isEmpty()) {
                binding.tilFullName.error = "Full name is required."
                return@setOnClickListener
            } else if (fullName.length > 80) {
                binding.tilFullName.error = "Full name must be 80 characters or fewer."
                return@setOnClickListener
            } else {
                binding.tilFullName.error = null
            }

            if (username.isEmpty()) {
                binding.tilUsername.error = "Username is required."
                return@setOnClickListener
            } else if (!User.isValidUsername(username)) {
                binding.tilUsername.error = "Username must be 3-30 characters without whitespace."
                return@setOnClickListener
            } else {
                binding.tilUsername.error = null
            }

            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required."
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required."
                return@setOnClickListener
            } else if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters."
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            viewModel.register(email, password, fullName, username)
        }

        binding.tvGoToLogin.setOnClickListener { finish() }

        viewModel.state.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.btnRegister.text = "Loading..."
                }
                is AuthViewModel.AuthState.Success -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "CREATE ACCOUNT"
                    Toast.makeText(this, mapFirebaseError(state.message), Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }

    private fun mapFirebaseError(raw: String): String {
        return when {
            raw.contains("email address is already",  ignoreCase = true) -> "This email is already registered."
            raw.contains("badly formatted",           ignoreCase = true) -> "Please enter a valid email address."
            raw.contains("network",                   ignoreCase = true) -> "No internet connection. Please check your network."
            raw.contains("weak password",             ignoreCase = true) -> "Password is too weak. Use at least 6 characters."
            else -> "Something went wrong. Please try again."
        }
    }
}
