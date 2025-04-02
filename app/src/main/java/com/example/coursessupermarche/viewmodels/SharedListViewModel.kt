package com.example.coursessupermarche.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.coursessupermarche.models.ListInvitation
import com.example.coursessupermarche.models.ListMember
import com.example.coursessupermarche.models.ShoppingList
import com.example.coursessupermarche.repositories.SharedListRepository
import com.example.coursessupermarche.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedListViewModel @Inject constructor(
    private val sharedListRepository: SharedListRepository
) : ViewModel() {

    private val TAG = "SharedListViewModel"

    // État du chargement
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Messages d'erreur
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // Message de succès
    private val _successMessage = MutableLiveData<String?>(null)
    val successMessage: LiveData<String?> = _successMessage

    // État du réseau
    private val _isNetworkAvailable = MutableLiveData<Boolean>(NetworkMonitor.isOnline)
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable

    // Utiliser un MutableStateFlow pour les listes partagées
    private val _sharedListsFlow = MutableStateFlow<List<ShoppingList>>(emptyList())

    // Listes partagées de l'utilisateur
    val sharedLists: LiveData<List<ShoppingList>> = sharedListRepository.getAccessibleLists()
        .catch { e ->
            Log.e(TAG, "Erreur lors de la récupération des listes partagées", e)
            _error.postValue("Impossible de charger vos listes: ${e.message}")
        }
        .asLiveData()

    // Liste sélectionnée actuellement
    private val _selectedList = MutableLiveData<ShoppingList?>(null)
    val selectedList: LiveData<ShoppingList?> = _selectedList

    // Liste ID sélectionnée
    private var selectedListId: String? = null

    // URL d'invitation générée
    private val _invitationUrl = MutableLiveData<String?>(null)
    val invitationUrl: LiveData<String?> = _invitationUrl

    // Sélectionner une liste
    fun selectList(listId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                selectedListId = listId
                val list = sharedListRepository.getListById(listId)
                _selectedList.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la sélection de la liste", e)
                _error.value = "Impossible de charger la liste: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Créer une nouvelle liste
    fun createShoppingList(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newList = sharedListRepository.createShoppingList(name)
                _successMessage.value = "Liste \"${name}\" créée avec succès"

                // Mettre à jour la liste des listes (si nécessaire)
                val currentLists = _sharedListsFlow.value.toMutableList()
                currentLists.add(newList)
                _sharedListsFlow.value = currentLists
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de la liste", e)
                _error.value = "Erreur lors de la création de la liste: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Créer une invitation pour une liste
    fun createInvitation(listId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val url = sharedListRepository.createInvitation(listId)
                _invitationUrl.value = url
                _successMessage.value = "Invitation créée avec succès"
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de l'invitation", e)
                _error.value = "Erreur lors de la création de l'invitation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Partager l'invitation via WhatsApp
    fun shareInvitationViaWhatsApp(listId: String) {
        viewModelScope.launch {
            try {
                val listName = selectedList.value?.name ?: "Liste de courses"
                _invitationUrl.value?.let { url ->
                    sharedListRepository.shareInvitationViaWhatsApp(url, listName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du partage de l'invitation", e)
                _error.value = "Erreur lors du partage: ${e.message}"
            }
        }
    }

    // Quitter une liste partagée
    fun leaveSharedList(listId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = sharedListRepository.leaveSharedList(listId)
                if (result) {
                    _successMessage.value = "Vous avez quitté la liste"

                    // Mettre à jour la liste des listes
                    val currentLists = _sharedListsFlow.value.toMutableList()
                    currentLists.removeIf { it.id == listId }
                    _sharedListsFlow.value = currentLists
                } else {
                    _error.value = "Impossible de quitter la liste"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la sortie de la liste", e)
                _error.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Supprimer une liste
    fun deleteList(listId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = sharedListRepository.deleteList(listId)
                if (result) {
                    _successMessage.value = "Liste supprimée avec succès"

                    // Mettre à jour la liste des listes
                    val currentLists = _sharedListsFlow.value.toMutableList()
                    currentLists.removeIf { it.id == listId }
                    _sharedListsFlow.value = currentLists
                } else {
                    _error.value = "Impossible de supprimer la liste"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression de la liste", e)
                _error.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Traiter une invitation depuis une URL
    fun processInvitationFromUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = sharedListRepository.processInvitationFromUrl(url)
                if (result) {
                    _successMessage.value = "Vous avez rejoint la liste avec succès"
                } else {
                    _error.value = "Invitation invalide ou expirée"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement de l'invitation", e)
                _error.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Effacer les messages d'erreur
    fun clearError() {
        _error.value = null
    }

    // Effacer les messages de succès
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // Effacer l'URL d'invitation
    fun clearInvitationUrl() {
        _invitationUrl.value = null
    }

    // Trier les listes par nom
    fun sortListsByName() {
        viewModelScope.launch {
            val currentLists = sharedLists.value ?: emptyList()
            _sharedListsFlow.value = currentLists.sortedBy { it.name }
        }
    }

    // Trier les listes par date de mise à jour
    fun sortListsByUpdated() {
        viewModelScope.launch {
            val currentLists = sharedLists.value ?: emptyList()
            _sharedListsFlow.value = currentLists.sortedByDescending { it.updatedAt }
        }
    }

    // Vérifier si l'utilisateur est propriétaire d'une liste
    fun isListOwner(listId: String): Boolean {
        val currentList = sharedLists.value?.find { it.id == listId }
        return currentList?.ownerId == FirebaseAuth.getInstance().currentUser?.uid
    }
}