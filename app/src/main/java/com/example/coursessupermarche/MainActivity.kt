package com.example.coursessupermarche

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coursessupermarche.adapters.ShoppingItemAdapter
import com.example.coursessupermarche.databinding.ActivityMainBinding
import com.example.coursessupermarche.utils.SwipeToDeleteCallback
import com.example.coursessupermarche.viewmodels.ShoppingListViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ShoppingItemAdapter
    private val viewModel: ShoppingListViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

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
    }

    private fun checkAuth() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Rediriger vers l'écran de connexion
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Charger la liste principale de l'utilisateur
        viewModel.loadCurrentUserList()
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

    private fun setupObservers() {
        viewModel.shoppingItems.observe(this) { items ->
            adapter.submitList(items)

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
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddItem.setOnClickListener {
            startActivity(Intent(this, AddItemActivity::class.java))
        }
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