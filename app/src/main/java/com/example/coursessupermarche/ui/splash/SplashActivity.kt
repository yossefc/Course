package com.example.coursessupermarche.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursessupermarche.R
import com.example.coursessupermarche.databinding.ActivitySplashBinding
import com.example.coursessupermarche.ui.auth.LoginActivity
import com.example.coursessupermarche.ui.main.MainActivity
import com.example.coursessupermarche.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Attendre un peu avant de naviguer
        lifecycleScope.launch {
            delay(1500)
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // L'utilisateur est déjà connecté
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // L'utilisateur doit se connecter
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Animation de transition
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}