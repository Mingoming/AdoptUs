package com.example.adoptus

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.adoptus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                else -> showBottomNav()
            }

            when (destination.id) {
                R.id.feedFragment -> setStatusBarAppearance(
                    statusBarColor = Color.BLACK,
                    useDarkIcons = false
                )
                else -> setStatusBarAppearance(
                    statusBarColor = ContextCompat.getColor(this, R.color.app_background),
                    useDarkIcons = true
                )
            }
        }
    }

    private fun setStatusBarAppearance(statusBarColor: Int, useDarkIcons: Boolean) {
        window.statusBarColor = statusBarColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = useDarkIcons
    }

    fun hideBottomNav() {
        binding.bottomNavigation.visibility = View.GONE
    }

    fun showBottomNav() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }
}
