package com.example.coursessupermarche

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.example.coursessupermarche.models.Categories
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.viewmodels.ShoppingListViewModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Date
import java.util.UUID

class AddItemActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var textInputLayoutItemName: TextInputLayout
    private lateinit var autoCompleteItemName: MaterialAutoCompleteTextView
    private lateinit var autoCompleteCategory: AutoCompleteTextView
    private lateinit var editTextQuantity: TextInputEditText
    private lateinit var buttonAddItem: Button

    private lateinit var viewModel: ShoppingListViewModel
    private var editingItemId: String? = null

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

        // Initialiser le ViewModel
        viewModel = ViewModelProvider(this).get(ShoppingListViewModel::class.java)

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

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener {
            finish()
        }

        buttonAddItem.setOnClickListener {
            val itemName = autoCompleteItemName.text.toString().trim()
            val category = autoCompleteCategory.text.toString()
            val quantityStr = editTextQuantity.text.toString()

            if (itemName.isEmpty()) {
                textInputLayoutItemName.error = getString(R.string.field_required)
                return@setOnClickListener
            } else {
                textInputLayoutItemName.error = null
            }

            val quantity = if (quantityStr.isNotEmpty()) quantityStr.toInt() else 1

            if (editingItemId != null) {
                // Mode édition
                val updatedItem = ShoppingItem(
                    id = editingItemId!!,
                    name = itemName,
                    category = category,
                    quantity = quantity,
                    createdAt = Date()
                )
                viewModel.updateItem(updatedItem)
            } else {
                // Mode ajout
                val newItem = ShoppingItem(
                    id = UUID.randomUUID().toString(),
                    name = itemName,
                    category = category,
                    quantity = quantity,
                    createdAt = Date()
                )
                viewModel.addItem(newItem)
            }

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