package com.example.coursessupermarche

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursessupermarche.databinding.ActivitySplashBinding
import com.example.coursessupermarche.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var auth: FirebaseAuth

    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "SplashActivity created")

        // Initialiser le moniteur réseau
        NetworkMonitor.init(applicationContext)

        // Animation du logo
        binding.imageViewLogo.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(1000)
            .withEndAction {
                binding.imageViewLogo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .start()
            }
            .start()

        // Attendre un peu avant de naviguer, avec un timeout de sécurité
        lifecycleScope.launch {
            delay(1500)
            withTimeoutOrNull(3000) { // Timeout de 3 secondes maximum
                try {
                    navigateToNextScreen()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating: ${e.message}")
                    // En cas d'erreur, aller à LoginActivity
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            } ?: run {
                // Si timeout, aller à LoginActivity
                Log.w(TAG, "Navigation timeout - going to login")
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun navigateToNextScreen() {
        Log.d(TAG, "Navigating to next screen")
        val currentUser = auth.currentUser
        val intent = if (currentUser != null) {
            Log.d(TAG, "User already logged in, going to MainActivity")
            Intent(this, MainActivity::class.java)
        } else {
            Log.d(TAG, "User not logged in, going to LoginActivity")
            Intent(this, LoginActivity::class.java)
        }

        // Animation de transition
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
        Log.d(TAG, "Navigation complete")
    }
}