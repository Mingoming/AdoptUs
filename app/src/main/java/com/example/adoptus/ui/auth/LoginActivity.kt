package com.example.adoptus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.adoptus.MainActivity
import com.example.adoptus.R
import com.example.adoptus.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { viewModel.loginWithGoogle(it) }
        } catch (e: ApiException) {
            showToast("Google Sign-In failed. Please try again.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar putih dengan ikon kontras
        window.statusBarColor = android.graphics.Color.WHITE
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Kalau sudah login, skip halaman ini langsung ke Feed
        val tempViewModel: AuthViewModel by viewModels()
        if (tempViewModel.isLoggedIn()) {
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please fill in all fields.")
                return@setOnClickListener
            }
            viewModel.login(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(client.signInIntent)
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = "Loading..."
                    binding.btnGoogle.isEnabled = false
                }
                is AuthViewModel.AuthState.Success -> {
                    goToMain()
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "LOGIN"
                    binding.btnGoogle.isEnabled = true
                    showToast(mapFirebaseError(state.message))
                }
                else -> Unit
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Terjemahkan pesan error Firebase yang teknis jadi pesan yang lebih bersih
    private fun mapFirebaseError(raw: String): String {
        return when {
            raw.contains("no user record",        ignoreCase = true) -> "No account found with this email."
            raw.contains("password is invalid",   ignoreCase = true) -> "Invalid email or password."
            raw.contains("badly formatted",       ignoreCase = true) -> "Please enter a valid email address."
            raw.contains("blocked",               ignoreCase = true) -> "Too many attempts. Please try again later."
            raw.contains("network",               ignoreCase = true) -> "No internet connection. Please check your network."
            raw.contains("credential is incorrect",ignoreCase = true) -> "Invalid email or password."
            raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) -> "Invalid email or password."
            else -> "Invalid email or password."
        }
    }

    private fun showForgotPasswordDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter your email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val container = android.widget.FrameLayout(this).apply {
            addView(input)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("We will send a password reset link to your email address.")
            .setView(container)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty()) {
                    showToast("Email cannot be empty.")
                } else {
                    com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showToast("Reset link sent successfully.")
                            } else {
                                showToast("Failed to send reset link: ${task.exception?.message}")
                            }
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

