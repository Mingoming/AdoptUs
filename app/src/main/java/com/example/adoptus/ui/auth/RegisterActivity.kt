package com.example.adoptus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.adoptus.MainActivity
import com.example.adoptus.R
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail    = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnReg     = findViewById<Button>(R.id.btnRegister)
        val tvLogin    = findViewById<TextView>(R.id.tvGoToLogin)

        btnReg.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(email, password, fullName)
        }

        tvLogin.setOnClickListener { finish() }

        viewModel.state.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    btnReg.isEnabled = false
                    btnReg.text = "Loading..."
                }
                is AuthViewModel.AuthState.Success -> {
                    Toast.makeText(this, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    btnReg.isEnabled = true
                    btnReg.text = "Create Account"
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }
}