package com.example.coursessupermarche.ui.auth

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Global.getString
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursessupermarche.MainActivity
import com.example.coursessupermarche.R
import com.example.coursessupermarche.databinding.ActivityLoginBinding
import com.example.coursessupermarche.repositories.ShoppingListRepository
import com.example.coursessupermarche.ui.main.MainActivity
import com.example.coursessupermarche.utils.showSnackbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var repository: ShoppingListRepository

    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Vérifier si l'utilisateur est déjà connecté
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // L'utilisateur est déjà connecté, naviguer vers l'activité principale
            navigateToMainActivity()
        }

        // Configurer Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupListeners()
        setupAnimations()
    }

    private fun setupListeners() {
        binding.signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun setupAnimations() {
        // Animer l'apparition des éléments
        binding.imageViewLogo.alpha = 0f
        binding.textViewTitle.alpha = 0f
        binding.textViewSubtitle.alpha = 0f
        binding.signInButton.alpha = 0f

        binding.imageViewLogo.animate()
            .alpha(1f)
            .setDuration(500)
            .start()

        binding.textViewTitle.animate()
            .alpha(1f)
            .setStartDelay(300)
            .setDuration(500)
            .start()

        binding.textViewSubtitle.animate()
            .alpha(1f)
            .setStartDelay(500)
            .setDuration(500)
            .start()

        binding.signInButton.animate()
            .alpha(1f)
            .setStartDelay(700)
            .setDuration(500)
            .start()
    }

    private fun signIn() {
        showLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
            account.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            showLoading(false)
            binding.root.showSnackbar(getString(R.string.error_google_signin, e.statusCode))
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    // Connexion réussie
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    // Sauvegarder l'utilisateur en local
                    lifecycleScope.launch {
                        repository.saveCurrentUser()

                        // Afficher un message de bienvenue
                        binding.root.showSnackbar(getString(R.string.welcome_message, user?.displayName))

                        // Attendre un peu pour que l'utilisateur voie le message
                        kotlinx.coroutines.delay(1000)

                        // Naviguer vers l'activité principale
                        navigateToMainActivity()
                    }
                } else {
                    // Échec de la connexion
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    binding.root.showSnackbar(
                        getString(R.string.error_authentication, task.exception?.localizedMessage)
                    )
                }
            }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !isLoading
    }
}