package com.example.adoptus

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.adoptus.databinding.ActivitySplashBinding
import com.example.adoptus.ui.auth.AuthViewModel
import com.example.adoptus.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install native splash screen untuk cold start
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Fetch and cache user profile if logged in
        if (authViewModel.isLoggedIn()) {
            lifecycleScope.launch {
                val repo = com.example.adoptus.data.repository.AuthRepository()
                repo.getCurrentUserProfile().fold(
                    onSuccess = { user ->
                        repo.cacheUserProfile(this@SplashActivity, user)
                    },
                    onFailure = {
                        // ignore
                    }
                )
            }
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Konversi dp ke piksel untuk translasi animasi
        val density = resources.displayMetrics.density
        val translateLogoY = 30f * density
        val translateTextY = 20f * density
        val swipeUpY = -150f * density

        // 1. Set state awal elemen untuk Animasi Masuk (Entry Animation)
        binding.ivSplashLogo.alpha = 0f
        binding.ivSplashLogo.scaleX = 0.9f
        binding.ivSplashLogo.scaleY = 0.9f
        binding.ivSplashLogo.translationY = translateLogoY

        binding.ivSplashText.alpha = 0f
        binding.ivSplashText.translationY = translateTextY

        binding.footerContainer.alpha = 0f

        // 2. Jalankan Animasi Masuk (Entry Animations)
        // Animasi Logo (Bouncing / Overshoot)
        binding.ivSplashLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(1000)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()

        // Animasi Text Logo (Delay 500ms, Bouncing / Overshoot)
        binding.ivSplashText.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(500)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()

        // Animasi Footer Loader (Delay 1200ms, Fade In)
        binding.footerContainer.animate()
            .alpha(1f)
            .setStartDelay(1200)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 3. Jadwalkan Animasi Keluar (Exit Animations) setelah 3000ms (3 detik)
        Handler(Looper.getMainLooper()).postDelayed({
            runExitAnimation(swipeUpY)
        }, 3000)
    }

    private fun runExitAnimation(swipeUpY: Float) {
        // A. Swipe up Logo & Text Logo
        binding.logoContainer.animate()
            .translationY(swipeUpY)
            .alpha(0f)
            .setDuration(700)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                navigateToNextScreen()
            }
            .start()

        // B. Fade out Footer Loader secara terpisah (durasi lebih cepat)
        binding.footerContainer.animate()
            .alpha(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun navigateToNextScreen() {
        val intent = if (authViewModel.isLoggedIn()) {
            Intent(this, MainActivity::class.java).apply {
                data = this@SplashActivity.intent.data
            }
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        // Hindari animasi transisi bawaan OS agar transisi fade-out halus
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
