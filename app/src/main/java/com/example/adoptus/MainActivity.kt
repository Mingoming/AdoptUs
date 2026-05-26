package com.example.adoptus

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.Fragment
import com.example.adoptus.fragment.FeedFragment
import com.example.adoptus.fragment.SearchFragment
import com.example.adoptus.fragment.AddPostFragment
import com.example.adoptus.fragment.ProfileFragment

import com.example.adoptus.ui.auth.AuthViewModel
import com.example.adoptus.ui.auth.LoginActivity

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
class MainActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)


        if (!viewModel.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        //val tvEmail   = findViewById<TextView>(R.id.tvWelcomeEmail)

        //tvEmail.text = FirebaseAuth.getInstance().currentUser?.email ?: ""


        val bottomNav : BottomNavigationView = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            setCurrentFragment(FeedFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val targetFragment: Fragment = when (item.itemId) {
                R.id.menu_feed -> FeedFragment()
                R.id.menu_search -> SearchFragment()
                R.id.menu_add -> AddPostFragment()
                R.id.menu_profile -> ProfileFragment()
                else -> FeedFragment()
            }
            setCurrentFragment(targetFragment)
        }
    }

    private fun setCurrentFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            commit()
        }
        return true
    }
}