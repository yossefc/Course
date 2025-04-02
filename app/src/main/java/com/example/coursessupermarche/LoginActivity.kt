package com.example.coursessupermarche

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coursessupermarche.databinding.ActivityLoginBinding
import com.example.coursessupermarche.repositories.ShoppingListRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
        Log.d(TAG, "Got sign-in result: ${result.resultCode}")
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sign-in result", e)
            showLoading(false)
            binding.root.showSnackbar("Erreur lors de la connexion. Veuillez réessayer.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "LoginActivity created")

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Vérifier si l'utilisateur est déjà connecté
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: ${currentUser.uid}")
            // L'utilisateur est déjà connecté, naviguer vers l'activité principale
            navigateToMainActivity()
            return
        }

        // Configurer Google Sign In avec un try-catch
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                // Utiliser directement l'ID client Web depuis google-services.json
                .requestIdToken("39354262123-8r63010jtmhg5ao2dvc6ekqkvvjbkf60.apps.googleusercontent.com")
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
            Log.d(TAG, "Google Sign-In configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Google Sign-In", e)
            binding.root.showSnackbar("Erreur de configuration. Veuillez réessayer.")
        }

        setupListeners()
        setupAnimations()
    }

    private fun setupListeners() {
        binding.signInButton.setOnClickListener {
            Log.d(TAG, "Sign-in button clicked")
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
        Log.d(TAG, "Starting sign-in flow")
        showLoading(true)
        try {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching sign-in", e)
            showLoading(false)
            binding.root.showSnackbar("Erreur lors du démarrage de la connexion. Veuillez réessayer.")
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
            account.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            showLoading(false)
            binding.root.showSnackbar("Erreur de connexion Google: ${e.statusCode}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            showLoading(false)
            binding.root.showSnackbar("Erreur inattendue. Veuillez réessayer.")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "Authenticating with Firebase")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    // Connexion réussie
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    // Sauvegarder l'utilisateur en local avec un timeout
                    lifecycleScope.launch {
                        val saveResult = withTimeoutOrNull(5000) { // 5 secondes maximum
                            try {
                                repository.saveCurrentUser()
                                true
                            } catch (e: Exception) {
                                Log.e(TAG, "Error saving user", e)
                                false
                            }
                        } ?: false

                        if (saveResult) {
                            // Afficher un message de bienvenue
                            binding.root.showSnackbar("Bienvenue ${user?.displayName}")

                            // Attendre un peu pour que l'utilisateur voie le message
                            delay(1000)
                        }

                        // Naviguer vers l'activité principale même si la sauvegarde échoue
                        navigateToMainActivity()
                    }
                } else {
                    // Échec de la connexion
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    binding.root.showSnackbar("Échec de l'authentification: ${task.exception?.message}")
                }
            }
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        Log.d(TAG, "Navigation to MainActivity complete")
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !isLoading
    }
}