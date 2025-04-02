package com.example.coursessupermarche

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.example.coursessupermarche.models.Categories
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.viewmodels.ShoppingListViewModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.UUID

@AndroidEntryPoint  // Cette annotation est nécessaire
class AddItemActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var textInputLayoutItemName: TextInputLayout
    private lateinit var autoCompleteItemName: MaterialAutoCompleteTextView
    private lateinit var autoCompleteCategory: AutoCompleteTextView
    private lateinit var editTextQuantity: TextInputEditText
    private lateinit var buttonAddItem: Button

    // Utiliser viewModels() au lieu de ViewModelProvider
    private val viewModel: ShoppingListViewModel by viewModels()
    private var editingItemId: String? = null

    // Dans AddItemActivity.kt, ajoutez cette vérification dans onCreate() juste après l'initialisation du ViewModel :

    // Dans AddItemActivity.kt, modifiez onCreate() comme suit :

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        // Initialiser les vues
        toolbar = findViewById(R.id.toolbar)
        textInputLayoutItemName = findViewById(R.id.textInputLayoutItemName)
        autoCompleteItemName = findViewById(R.id.autoCompleteItemName)
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        buttonAddItem = findViewById(R.id.buttonAddItem)

        // Remarque: viewModel est déjà initialisé comme une propriété val
        // Ne pas réassigner viewModel

        // AJOUTEZ CE BLOC :
        // Vérifier si une liste est chargée
        if (viewModel.getListId() == null) {
            Log.d("AddItemActivity", "Aucune liste n'est chargée, initialisation...")
            viewModel.forceLoadCurrentUserList()

            // Observer les messages d'erreur/info pour informer l'utilisateur
            viewModel.error.observe(this) { message ->
                message?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
        // FIN DU BLOC AJOUTÉ

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupCategoryDropdown()
        setupAutoComplete()
        setupListeners()

        // Vérifier si on est en mode édition
        editingItemId = intent.getStringExtra("ITEM_ID")
        if (editingItemId != null) {
            loadItemDetails(editingItemId!!)
            toolbar.title = getString(R.string.edit_item)
            buttonAddItem.text = getString(R.string.update_item)
        }
    }
    private fun setupCategoryDropdown() {
        val categories = Categories.getAllCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        autoCompleteCategory.setAdapter(adapter)

        // Sélectionner la première catégorie par défaut
        autoCompleteCategory.setText(categories[0], false)
    }

    private fun setupAutoComplete() {
        // Charger les suggestions des noms de produits récemment utilisés
        viewModel.loadProductSuggestions().observe(this) { suggestions ->
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
            )
            autoCompleteItemName.setAdapter(adapter)
        }
    }

    // Dans AddItemActivity.kt, modifiez le bouton d'ajout comme suit :

    private fun setupListeners() {
        val TAG = "AddItemActivity" // Ajoutez cette constante en haut de la classe

        toolbar.setNavigationOnClickListener {
            finish()
        }

        buttonAddItem.setOnClickListener {
            Log.d(TAG, "==== DÉBUT TRAITEMENT BOUTON AJOUTER ====")
            val itemName = autoCompleteItemName.text.toString().trim()
            val category = autoCompleteCategory.text.toString()
            val quantityStr = editTextQuantity.text.toString()

            Log.d(TAG, "Données saisies: nom='$itemName', catégorie='$category', quantité='$quantityStr'")

            if (itemName.isEmpty()) {
                Log.d(TAG, "Validation: nom vide, affichage erreur")
                textInputLayoutItemName.error = getString(R.string.field_required)
                return@setOnClickListener
            } else {
                textInputLayoutItemName.error = null
            }

            val quantity = if (quantityStr.isNotEmpty()) quantityStr.toInt() else 1
            Log.d(TAG, "Quantité finale: $quantity")

            if (editingItemId != null) {
                // Mode édition
                Log.d(TAG, "Mode: ÉDITION d'un article existant (ID: $editingItemId)")
                val updatedItem = ShoppingItem(
                    id = editingItemId!!,
                    name = itemName,
                    category = category,
                    quantity = quantity,
                    createdAt = Date()
                )
                Log.d(TAG, "Appel à viewModel.updateItem")
                viewModel.updateItem(updatedItem)
            } else {
                // Mode ajout
                val newItemId = UUID.randomUUID().toString()
                Log.d(TAG, "Mode: AJOUT d'un nouvel article (ID généré: $newItemId)")
                val newItem = ShoppingItem(
                    id = newItemId,
                    name = itemName,
                    category = category,
                    quantity = quantity,
                    createdAt = Date()
                )
                Log.d(TAG, "Appel à viewModel.addItem")
                viewModel.addItem(newItem)
            }

            Log.d(TAG, "Fermeture de l'activité")
            Log.d(TAG, "==== FIN TRAITEMENT BOUTON AJOUTER ====")
            finish()
        }
    }

    private fun loadItemDetails(itemId: String) {
        viewModel.getItemById(itemId).observe(this) { item ->
            item?.let {
                autoCompleteItemName.setText(it.name)
                autoCompleteCategory.setText(it.category)
                editTextQuantity.setText(it.quantity.toString())
            }
        }
    }
}