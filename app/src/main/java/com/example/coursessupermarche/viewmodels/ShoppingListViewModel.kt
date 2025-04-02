package com.example.coursessupermarche.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.repositories.ShoppingListRepository
import com.example.coursessupermarche.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: ShoppingListRepository
) : ViewModel() {

    private val TAG = "ShoppingListViewModel"

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isNetworkAvailable = MutableLiveData<Boolean>(NetworkMonitor.isOnline)
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var currentListId: String? = null

    // Initialiser avec un StateFlow vide dès la création du ViewModel
    private val _shoppingItemsFlow = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val shoppingItems: LiveData<List<ShoppingItem>> = _shoppingItemsFlow.asLiveData()

    private var _sortType = SortType.BY_CREATION

    init {
        Log.d(TAG, "Initialisation du ViewModel")
        viewModelScope.launch {
            try {
                // Surveiller les changements de connectivité
                NetworkMonitor.isOnlineFlow.collect { isOnline ->
                    Log.d(TAG, "État du réseau changé: ${if (isOnline) "EN LIGNE" else "HORS LIGNE"}")
                    _isNetworkAvailable.value = isOnline

                    // Si la connexion est restaurée, synchroniser les données
                    if (isOnline) {
                        syncData()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la surveillance du réseau: ${e.message}", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Synchroniser les données avec le serveur
    private suspend fun syncData() {
        Log.d(TAG, "Tentative de synchronisation des données")
        try {
            repository.syncData()
            Log.d(TAG, "Synchronisation réussie")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la synchronisation: ${e.message}", e)
        }
    }
    // Ajoutez ces deux méthodes à ShoppingListViewModel.kt :


    // Charger la liste principale de l'utilisateur courant
    fun loadCurrentUserList() {
        // Déjà en cours de chargement, éviter les doubles appels
        if (_isLoading.value == true) {
            Log.d(TAG, "Chargement déjà en cours, ignoré")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Début du chargement de la liste principale")
            _isLoading.value = true
            try {
                // Obtenir ou créer une liste avec un timeout
                val listId = withTimeoutOrNull(5000) { // 5 secondes maximum
                    repository.getCurrentUserMainListId()
                } ?: throw Exception("Timeout lors de la récupération de la liste")

                currentListId = listId
                Log.d(TAG, "Liste chargée avec succès, ID: $listId")

                // Observer les changements de la liste
                try {
                    repository.observeShoppingItems(listId)
                        .catch { e ->
                            Log.e(TAG, "Erreur dans la collecte du flux: ${e.message}", e)
                            _error.postValue("Erreur lors du chargement des produits: ${e.message}")
                        }
                        .collect { items ->
                            val sortedItems = when (_sortType) {
                                SortType.BY_NAME -> items.sortedBy { it.name }
                                SortType.BY_CATEGORY -> items.sortedBy { it.category }
                                SortType.BY_CREATION -> items
                            }
                            Log.d(TAG, "Items mis à jour: ${items.size} articles")
                            _shoppingItemsFlow.value = sortedItems
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la collecte des données: ${e.message}", e)
                    _error.postValue("Erreur lors du suivi des produits: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur globale: ${e.message}", e)
                _error.value = e.localizedMessage ?: "Une erreur s'est produite"
                // En cas d'erreur, initialiser avec une liste vide pour éviter le blocage
                _shoppingItemsFlow.value = emptyList()
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Fin du chargement de la liste")
            }
        }
    }

    // Méthode pour vérifier si une liste est chargée
    fun getListId(): String? {
        return currentListId
    }

    // Méthode pour forcer le chargement d'une liste
    fun forceLoadCurrentUserList() {
        Log.d(TAG, "Forçage du chargement de la liste utilisateur")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val listId = repository.getCurrentUserMainListId()
                Log.d(TAG, "Liste chargée avec succès, ID: $listId")
                currentListId = listId

                // Émettre un message pour indiquer que la liste est prête
                _error.value = "Liste chargée, vous pouvez maintenant ajouter des produits"

                // Effacer le message après 3 secondes
                delay(3000)
                _error.value = null

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du chargement forcé: ${e.message}", e)
                _error.value = "Impossible de charger la liste : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Modification de la méthode addItem pour gérer le cas où currentListId est null :
    fun addItem(item: ShoppingItem) {
        viewModelScope.launch {
            Log.d(TAG, "==== DÉBUT AJOUT ARTICLE VIEWMODEL ====")
            Log.d(TAG, "Article à ajouter: ${item.name}, ID: ${item.id}")

            _isLoading.value = true
            try {
                if (currentListId == null) {
                    Log.d(TAG, "Aucune liste définie, chargement automatique...")
                    currentListId = repository.getCurrentUserMainListId()
                    Log.d(TAG, "Liste chargée automatiquement: $currentListId")
                }

                currentListId?.let { listId ->
                    Log.d(TAG, "Liste cible: $listId")

                    // Appel au repository pour ajouter l'article
                    repository.addItem(listId, item)
                    Log.d(TAG, "✓ Article ajouté via repository")

                    // Forcer une mise à jour de la liste après l'ajout
                    val currentItems = _shoppingItemsFlow.value.toMutableList()
                    currentItems.add(item)

                    // Appliquer le tri actuel
                    val sortedItems = when (_sortType) {
                        SortType.BY_NAME -> currentItems.sortedBy { it.name }
                        SortType.BY_CATEGORY -> currentItems.sortedBy { it.category }
                        SortType.BY_CREATION -> currentItems
                    }

                    Log.d(TAG, "Liste mise à jour manuellement: ${sortedItems.size} articles")
                    _shoppingItemsFlow.value = sortedItems
                } ?: run {
                    Log.e(TAG, "❌ Impossible de charger une liste pour l'ajout")
                    _error.value = "Impossible d'obtenir une liste active"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERREUR lors de l'ajout: ${e.message}", e)
                _error.value = e.localizedMessage ?: "Erreur lors de l'ajout de l'article"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "==== FIN AJOUT ARTICLE VIEWMODEL ====")
            }
        }
    }

    // Mettre à jour un article existant
    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            Log.d(TAG, "Mise à jour de l'article: ${item.id}")
            _isLoading.value = true
            try {
                currentListId?.let { listId ->
                    repository.updateItem(listId, item)
                    Log.d(TAG, "Article mis à jour avec succès")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la mise à jour: ${e.message}", e)
                _error.value = e.localizedMessage ?: "Erreur lors de la mise à jour de l'article"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Supprimer un article
    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            Log.d(TAG, "Suppression de l'article: ${item.id}")
            _isLoading.value = true
            try {
                currentListId?.let { listId ->
                    repository.deleteItem(listId, item.id)
                    Log.d(TAG, "Article supprimé avec succès")

                    // Mise à jour manuelle pour garantir la mise à jour de l'UI
                    val updatedItems = _shoppingItemsFlow.value.toMutableList()
                    updatedItems.removeIf { it.id == item.id }
                    _shoppingItemsFlow.value = updatedItems
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression: ${e.message}", e)
                _error.value = e.localizedMessage ?: "Erreur lors de la suppression de l'article"
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
            Log.d(TAG, "Recherche de l'article: $itemId")
            try {
                currentListId?.let { listId ->
                    val item = repository.getItemById(listId, itemId)
                    result.value = item
                    Log.d(TAG, "Article trouvé: ${item?.name ?: "non trouvé"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération: ${e.message}", e)
                _error.value = e.localizedMessage ?: "Erreur lors de la récupération de l'article"
            }
        }

        return result
    }

    // Charger les suggestions de produits pour l'autocomplétion
    fun loadProductSuggestions(): LiveData<List<String>> {
        val result = MutableLiveData<List<String>>(emptyList())

        viewModelScope.launch {
            Log.d(TAG, "Chargement des suggestions de produits")
            try {
                val suggestions = repository.getProductSuggestions()
                result.value = suggestions
                Log.d(TAG, "${suggestions.size} suggestions chargées")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du chargement des suggestions: ${e.message}", e)
                _error.value = e.localizedMessage ?: "Erreur lors du chargement des suggestions"
            }
        }

        return result
    }

    // Trier par catégorie
    fun sortByCategory() {
        Log.d(TAG, "Tri par catégorie")
        _sortType = SortType.BY_CATEGORY
        refreshSorting()
    }

    // Trier par nom
    fun sortByName() {
        Log.d(TAG, "Tri par nom")
        _sortType = SortType.BY_NAME
        refreshSorting()
    }

    // Rafraîchir le tri
    private fun refreshSorting() {
        viewModelScope.launch {
            try {
                val items = _shoppingItemsFlow.value
                val sortedItems = when (_sortType) {
                    SortType.BY_NAME -> items.sortedBy { it.name }
                    SortType.BY_CATEGORY -> items.sortedBy { it.category }
                    SortType.BY_CREATION -> items
                }
                Log.d(TAG, "Liste triée: ${sortedItems.size} articles")
                _shoppingItemsFlow.value = sortedItems
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du tri: ${e.message}", e)
                _error.value = e.localizedMessage ?: "Erreur lors du tri"
            }
        }
    }

    enum class SortType {
        BY_NAME,
        BY_CATEGORY,
        BY_CREATION
    }
}