package com.example.coursessupermarche.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.repositories.ShoppingListRepository
import kotlinx.coroutines.launch
import java.util.*

class ShoppingListViewModel : ViewModel() {

    private val repository = ShoppingListRepository()

    private val _shoppingItems = MutableLiveData<List<ShoppingItem>>(emptyList())
    val shoppingItems: LiveData<List<ShoppingItem>> = _shoppingItems

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var currentListId: String? = null

    // Charger la liste principale de l'utilisateur courant
    fun loadCurrentUserList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Obtenir ou créer une liste
                val listId = repository.getCurrentUserMainListId()
                currentListId = listId

                // Observer les changements de la liste
                repository.observeShoppingItems(listId) { items, exception ->
                    if (exception != null) {
                        _error.value = exception.localizedMessage
                    } else {
                        _shoppingItems.value = items
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
                _isLoading.value = false
            }
        }
    }

    // Ajouter un nouvel article
    fun addItem(item: ShoppingItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentListId?.let { listId ->
                    repository.addItem(listId, item)

                    // Sauvegarder en historique pour les suggestions futures
                    repository.addToProductSuggestions(item.name)
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mettre à jour un article existant
    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentListId?.let { listId ->
                    repository.updateItem(listId, item)
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Supprimer un article
    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentListId?.let { listId ->
                    repository.deleteItem(listId, item.id)
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mettre à jour le statut "coché" d'un article
    fun updateItemCheckStatus(item: ShoppingItem, isChecked: Boolean) {
        val updatedItem = item.copy(isChecked = isChecked)
        updateItem(updatedItem)
    }

    // Récupérer un article par son ID
    fun getItemById(itemId: String): LiveData<ShoppingItem?> {
        val result = MutableLiveData<ShoppingItem?>()

        viewModelScope.launch {
            try {
                currentListId?.let { listId ->
                    val item = repository.getItemById(listId, itemId)
                    result.value = item
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }

        return result
    }

    // Charger les suggestions de produits pour l'autocomplétion
    fun loadProductSuggestions(): LiveData<List<String>> {
        val result = MutableLiveData<List<String>>(emptyList())

        viewModelScope.launch {
            try {
                val suggestions = repository.getProductSuggestions()
                result.value = suggestions
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }

        return result
    }

    // Trier par catégorie
    fun sortByCategory() {
        _shoppingItems.value = _shoppingItems.value?.sortedBy { it.category }
    }

    // Trier par nom
    fun sortByName() {
        _shoppingItems.value = _shoppingItems.value?.sortedBy { it.name }
    }

    // Effacer le message d'erreur
    fun clearError() {
        _error.value = null
    }
}