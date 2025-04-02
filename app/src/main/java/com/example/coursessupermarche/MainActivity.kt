package com.example.coursessupermarche

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coursessupermarche.adapters.ShoppingItemAdapter
import com.example.coursessupermarche.databinding.ActivityMainBinding
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.utils.SwipeToDeleteCallback
import com.example.coursessupermarche.utils.showSnackbar
import com.example.coursessupermarche.viewmodels.ShoppingListViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint  // Ajout de cette annotation essentielle
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ShoppingItemAdapter

    // Utilisation de viewModels() avec Hilt
    private val viewModel: ShoppingListViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Vérifier l'authentification
        checkAuth()
        // Ajouter ce code dans la méthode onCreate de MainActivity.kt juste après le appel à checkAuth():

// Configurer un délai maximum de chargement
        lifecycleScope.launch {
            delay(10000) // Attendre 10 secondes
            if (binding.progressBar.visibility == View.VISIBLE) {
                // Toujours en chargement après 10 secondes, quelque chose ne va pas
                Log.w("MainActivity", "Chargement trop long, forçage de l'interface")
                binding.progressBar.visibility = View.GONE
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewItems.visibility = View.GONE
                binding.root.showSnackbar("Impossible de charger vos articles. Veuillez réessayer plus tard.")
            }
        }
    }

    // Dans MainActivity.kt, modifiez la méthode checkAuth() pour s'assurer que loadCurrentUserList() est appelée :

    private fun checkAuth() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Rediriger vers l'écran de connexion
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Charger la liste principale de l'utilisateur et attendre qu'elle soit prête
        Log.d("MainActivity", "Utilisateur connecté, chargement de la liste...")
        viewModel.loadCurrentUserList()

        // Vérifier si une liste a été chargée après un certain délai
        Handler(Looper.getMainLooper()).postDelayed({
            if (viewModel.getListId() == null) {
                Log.w("MainActivity", "⚠️ Aucune liste n'a été chargée après délai")
                // Forcer un nouveau chargement
                viewModel.forceLoadCurrentUserList()
            } else {
                Log.d("MainActivity", "✓ Liste correctement chargée: ${viewModel.getListId()}")
            }
        }, 1000) // Vérifier après 1 seconde
    }

    private fun setupRecyclerView() {
        adapter = ShoppingItemAdapter(
            onItemChecked = { item, isChecked ->
                viewModel.updateItemCheckStatus(item, isChecked)
            },
            onItemClicked = { item ->
                // Ouvrir l'écran d'édition
                val intent = Intent(this, AddItemActivity::class.java)
                intent.putExtra("ITEM_ID", item.id)
                startActivity(intent)
            }
        )

        binding.recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // Configurer le swipe pour supprimer
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedItem = adapter.getItemAt(position)

                viewModel.deleteItem(deletedItem)

                // Afficher Snackbar avec option d'annulation
                Snackbar.make(
                    binding.root,
                    getString(R.string.item_deleted, deletedItem.name),
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.undo) {
                    viewModel.addItem(deletedItem)
                }.show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewItems)
    }

// Remplacer la méthode setupObservers dans MainActivity.kt par cette version améliorée
private fun testAddItem() {
    val TAG = "MainActivity"
    Log.d(TAG, "==== TEST AJOUT ARTICLE DIRECT ====")

    // Créer un produit de test
    val testItemId = UUID.randomUUID().toString()
    val testItem = ShoppingItem(
        id = testItemId,
        name = "Produit Test " + System.currentTimeMillis(),
        category = "Autres",
        quantity = 1,
        createdAt = Date()
    )

    Log.d(TAG, "Produit test créé: ID=$testItemId, Nom=${testItem.name}")

    // Appeler directement la méthode addItem du ViewModel
    viewModel.addItem(testItem)

    Log.d(TAG, "Méthode addItem appelée")
    Log.d(TAG, "==== FIN TEST AJOUT ARTICLE DIRECT ====")

    // Feedback visuel
    Snackbar.make(
        binding.root,
        "Test d'ajout lancé: ${testItem.name}",
        Snackbar.LENGTH_LONG
    ).show()
}
    private fun setupObservers() {
        // Utiliser un observer qui ne déclenche pas des appels dupliqués
        viewModel.shoppingItems.observe(this) { items ->
            Log.d("MainActivity", "Liste mise à jour: ${items.size} éléments")

            // Utiliser la méthode améliorée de l'adaptateur
            adapter.updateItems(items)

            // Afficher un message si la liste est vide
            if (items.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewItems.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewItems.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Log.e("MainActivity", "Erreur affichée: $it")
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Ajouter un observateur pour le statut réseau si nécessaire
        viewModel.isNetworkAvailable.observe(this) { isOnline ->
            if (!isOnline) {
                Snackbar.make(
                    binding.root,
                    "Mode hors ligne. Les modifications seront synchronisées ultérieurement.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

// Dans MainActivity.kt, remplacez le code de setupListeners() par celui-ci :

    private fun setupListeners() {
        // Suppression de l'ancien onLongClickListener s'il a été ajouté lors des tests
        binding.fabAddItem.setOnLongClickListener(null)

        // Réinitialisation du OnClickListener standard
        binding.fabAddItem.setOnClickListener {
            Log.d("MainActivity", "Bouton FAB cliqué - lancement de AddItemActivity")
            startActivity(Intent(this, AddItemActivity::class.java))
        }

        // S'assurer que le bouton est visible et activé
        binding.fabAddItem.visibility = View.VISIBLE
        binding.fabAddItem.isEnabled = true

        // Ajouter une marge supplémentaire pour éviter qu'il soit caché
        val params = binding.fabAddItem.layoutParams as CoordinatorLayout.LayoutParams
        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.fab_margin) // Assurez-vous que cette dimension existe
        binding.fabAddItem.layoutParams = params
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            R.id.action_sort_by_category -> {
                viewModel.sortByCategory()
                true
            }
            R.id.action_sort_by_name -> {
                viewModel.sortByName()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}