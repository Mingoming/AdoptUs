package com.example.adoptus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.adoptus.databinding.ActivityMainBinding
import com.example.adoptus.ui.auth.AuthViewModel
import com.example.adoptus.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (!viewModel.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Hubungkan BottomNav ke NavController — otomatis handle semua navigasi tab
        binding.bottomNavigation.setupWithNavController(navController)

        // Sembunyikan BottomNav saat masuk ke halaman yang tidak butuh tab bar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.addPostFragment,
                R.id.petDetailFragment -> hideBottomNav()
                else                   -> showBottomNav()
            }
        }
    }

    fun hideBottomNav() {
        binding.bottomNavigation.visibility = View.GONE
    }

    fun showBottomNav() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }
}