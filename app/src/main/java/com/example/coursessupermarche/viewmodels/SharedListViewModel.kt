package com.example.coursessupermarche.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.coursessupermarche.models.ListInvitation
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

    // URL d'invitation générée
    private val _invitationUrl = MutableLiveData<String?>(null)
    val invitationUrl: LiveData<String?> = _invitationUrl

    // ... autres méthodes

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