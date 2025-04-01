package com.example.coursessupermarche.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.repositories.ShoppingListRepository
import com.example.coursessupermarche.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: ShoppingListRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isNetworkAvailable = MutableLiveData<Boolean>(NetworkMonitor.isOnline)
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent

    private var currentListId: String? = null

    val shoppingItems: LiveData<List<ShoppingItem>> get() = _shoppingItemsFlow.asLiveData()

    private lateinit var _shoppingItemsFlow: StateFlow<List<ShoppingItem>>

    private var _sortType = SortType.BY_CREATION

    init {
        viewModelScope.launch {
            // Surveiller les changements de connectivité
            NetworkMonitor.isOnlineFlow.collect { isOnline ->
                _isNetworkAvailable.value = isOnline

                // Si la connexion est restaurée, synchroniser les données
                if (isOnline) {
                    syncData()
                }
            }
        }
    }

    // Synchroniser les données avec le serveur
    private suspend fun syncData() {
        repository.syncUnsyncedData()
    }

    // Charger la liste principale de l'utilisateur courant
    fun loadCurrentUserList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Obtenir ou créer une liste
                val listId = repository.getCurrentUserMainListId()
                currentListId = listId

                // Observer les changements de la liste
                _shoppingItemsFlow = repository.observeShoppingItems(listId)
                    .map { items ->
                        when (_sortType) {
                            SortType.BY_NAME -> items.sortedBy { it.name }
                            SortType.BY_CATEGORY -> items.sortedBy { it.category }
                            SortType.BY_CREATION -> items
                        }
                    }
                    .stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000),
                        emptyList()
                    )
            } catch (e: Exception) {
                _errorEvent.emit(e.localizedMessage ?: "Une erreur s'est produite")
            } finally {
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
                }
            } catch (e: Exception) {
                _errorEvent.emit(e.localizedMessage ?: "Erreur lors de l'ajout de l'article")
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
                _errorEvent.emit(e.localizedMessage ?: "Erreur lors de la mise à jour de l'article")
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
                _errorEvent.emit(e.localizedMessage ?: "Erreur lors de la suppression de l'article")
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
                _errorEvent.emit(e.localizedMessage ?: "Erreur lors de la récupération de l'article")
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
                _errorEvent.emit(e.localizedMessage ?: "Erreur lors du chargement des suggestions")
            }
        }

        return result
    }

    // Trier par catégorie
    fun sortByCategory() {
        _sortType = SortType.BY_CATEGORY
        refreshSorting()
    }

    // Trier par nom
    fun sortByName() {
        _sortType = SortType.BY_NAME
        refreshSorting()
    }

    // Rafraîchir le tri
    private fun refreshSorting() {
        viewModelScope.launch {
            try {
                currentListId?.let { listId ->
                    val items = _shoppingItemsFlow.value
                    val sortedItems = when (_sortType) {
                        SortType.BY_NAME -> items.sortedBy { it.name }
                        SortType.BY_CATEGORY -> items.sortedBy { it.category }
                        SortType.BY_CREATION -> items
                    }
                    // Force un refresh en créant un nouvel objet MutableLiveData
                    (shoppingItems as? MutableLiveData)?.value = sortedItems
                }
            } catch (e: Exception) {
                _errorEvent.emit(e.localizedMessage ?: "Erreur lors du tri")
            }
        }
    }

    enum class SortType {
        BY_NAME,
        BY_CATEGORY,
        BY_CREATION
    }
}