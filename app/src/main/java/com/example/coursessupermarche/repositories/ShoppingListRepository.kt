package com.example.coursessupermarche.repositories

import android.util.Log
import com.example.coursessupermarche.data.local.dao.ShoppingItemDao
import com.example.coursessupermarche.data.local.dao.ShoppingListDao
import com.example.coursessupermarche.data.local.dao.UserDao
import com.example.coursessupermarche.data.local.entities.ProductSuggestionEntity
import com.example.coursessupermarche.data.local.entities.ShoppingItemEntity
import com.example.coursessupermarche.data.local.entities.ShoppingListEntity
import com.example.coursessupermarche.data.local.entities.UserEntity
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.models.ShoppingList
import com.example.coursessupermarche.models.User
import com.example.coursessupermarche.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour gérer les listes de courses et leurs articles.
 * Gère la persistance locale (Room) et la synchronisation avec Firebase.
 */
@Singleton
class ShoppingListRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao,
    private val userDao: UserDao
) {
    private val TAG = "ShoppingListRepository"

    // Collections Firestore
    private val listsCollection = firestore.collection("lists")
    private val suggestionsCollection = firestore.collection("product_suggestions")

    // Obtenir l'ID de l'utilisateur courant
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Aucun utilisateur connecté")
    }

    /**
     * Sauvegarde l'utilisateur courant en local
     */
    suspend fun saveCurrentUser() {
        val firebaseUser = auth.currentUser ?: return

        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString()
        )

        userDao.insert(UserEntity.fromModel(user))
        Log.d(TAG, "Utilisateur sauvegardé: ${user.id}")
    }

    /**
     * Observer les changements dans la liste d'articles
     */
    fun observeShoppingItems(listId: String): Flow<List<ShoppingItem>> {
        Log.d(TAG, "Observation démarrée pour la liste: $listId")
        return shoppingItemDao.getItemsByListId(listId)
            .map { entities -> entities.map { it.toModel() } }
            .distinctUntilChanged()
            .catch { exception ->
                Log.e(TAG, "Erreur dans le flux d'observation", exception)
                emit(emptyList())
            }
    }

    /**
     * Obtenir ou créer une liste principale pour l'utilisateur courant
     */
    suspend fun getCurrentUserMainListId(): String {
        val userId = getCurrentUserId()

        // Essayer d'abord d'obtenir la liste locale
        val localList = shoppingListDao.getLatestListByUserId(userId)
        if (localList != null) {
            Log.d(TAG, "Liste existante trouvée: ${localList.id}")
            return localList.id
        }

        return createNewList(userId)
    }

    /**
     * Crée une nouvelle liste pour l'utilisateur
     */
    private suspend fun createNewList(userId: String): String {
        // Si online, essayer d'obtenir la liste du serveur
        if (NetworkMonitor.isOnline) {
            try {
                // Chercher une liste existante dans Firestore
                val querySnapshot = listsCollection
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .await()

                // Si une liste existe, la retourner et la sauvegarder localement
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val listId = doc.id
                    val name = doc.getString("name") ?: "Ma liste de courses"
                    val createdAt = doc.getDate("createdAt") ?: Date()
                    val updatedAt = doc.getDate("updatedAt") ?: Date()

                    val list = ShoppingListEntity(
                        id = listId,
                        userId = userId,
                        name = name,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        isRemotelySync = true,
                        isShared = false,
                        ownerId = userId
                    )

                    shoppingListDao.insert(list)
                    Log.d(TAG, "Liste récupérée depuis Firestore: $listId")
                    return listId
                }

                // Sinon, créer une nouvelle liste sur Firestore
                val listId = UUID.randomUUID().toString()
                val newList = hashMapOf(
                    "id" to listId,
                    "userId" to userId,
                    "name" to "Ma liste de courses",
                    "createdAt" to Date(),
                    "updatedAt" to Date(),
                    "isShared" to false,
                    "ownerId" to userId
                )

                listsCollection.document(listId).set(newList).await()

                // Sauvegarder en local
                val list = ShoppingListEntity(
                    id = listId,
                    userId = userId,
                    name = "Ma liste de courses",
                    createdAt = Date(),
                    updatedAt = Date(),
                    isRemotelySync = true,
                    isShared = false,
                    ownerId = userId
                )

                shoppingListDao.insert(list)
                Log.d(TAG, "Nouvelle liste créée sur Firestore: $listId")
                return listId
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de la liste sur Firestore", e)
                // Continuer avec la création locale
            }
        }

        // Créer une liste locale si tout échoue
        val listId = UUID.randomUUID().toString()
        val list = ShoppingListEntity(
            id = listId,
            userId = userId,
            name = "Ma liste de courses",
            createdAt = Date(),
            updatedAt = Date(),
            isRemotelySync = false,
            isShared = false,
            ownerId = userId
        )

        shoppingListDao.insert(list)
        Log.d(TAG, "Nouvelle liste créée localement: $listId")
        return listId
    }

    /**
     * Ajouter un nouvel article à la liste
     */
    suspend fun addItem(listId: String, item: ShoppingItem) {
        try {
            // Mettre à jour la date de modification de la liste
            shoppingListDao.updateTimestamp(listId, Date().time)

            // Ajouter l'article localement
            val entity = ShoppingItemEntity.fromModel(
                model = item,
                listId = listId,
                isRemotelySync = NetworkMonitor.isOnline
            )
            shoppingItemDao.insert(entity)

            // Ajouter à Firebase si en ligne
            if (NetworkMonitor.isOnline) {
                syncItemToFirebase(listId, item)
            }

            // Ajouter aux suggestions pour l'autocomplétion
            addToProductSuggestions(item.name)

            Log.d(TAG, "Article ajouté avec succès: ${item.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'ajout de l'article", e)
            throw e
        }
    }

    /**
     * Synchronise un article avec Firebase
     */
    private suspend fun syncItemToFirebase(listId: String, item: ShoppingItem) {
        try {
            auth.currentUser?.let { user ->
                // Vérifier que la liste existe dans Firestore
                ensureListExistsInFirestore(listId, user.uid)

                // Mettre à jour le timestamp de la liste
                listsCollection.document(listId)
                    .update("updatedAt", Date())
                    .await()

                // Ajouter l'élément dans la sous-collection items
                val itemMap = mapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "quantity" to item.quantity,
                    "category" to item.category,
                    "isChecked" to item.isChecked,
                    "createdAt" to item.createdAt
                )

                listsCollection.document(listId)
                    .collection("items")
                    .document(item.id)
                    .set(itemMap)
                    .await()

                // Marquer comme synchronisé
                shoppingItemDao.markItemAsSynced(item.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la synchronisation avec Firebase", e)
            // Ne pas propager l'erreur - l'article est sauvegardé localement
        }
    }

    /**
     * S'assure qu'une liste existe dans Firestore
     */
    private suspend fun ensureListExistsInFirestore(listId: String, userId: String) {
        try {
            val listDoc = listsCollection.document(listId).get().await()
            if (!listDoc.exists()) {
                val listData = hashMapOf(
                    "id" to listId,
                    "userId" to userId,
                    "name" to "Ma liste de courses",
                    "createdAt" to Date(),
                    "updatedAt" to Date(),
                    "isShared" to false,
                    "ownerId" to userId
                )
                listsCollection.document(listId).set(listData).await()
                Log.d(TAG, "Liste créée dans Firestore: $listId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification/création de liste dans Firestore", e)
            throw e
        }
    }

    /**
     * Mettre à jour un article existant
     */
    suspend fun updateItem(listId: String, item: ShoppingItem) {
        try {
            // Mettre à jour la date de modification de la liste
            shoppingListDao.updateTimestamp(listId, Date().time)

            // Mettre à jour l'article localement
            val entity = ShoppingItemEntity.fromModel(
                model = item,
                listId = listId,
                isRemotelySync = NetworkMonitor.isOnline
            )
            shoppingItemDao.update(entity)

            // Mettre à jour sur Firebase si en ligne
            if (NetworkMonitor.isOnline) {
                try {
                    listsCollection.document(listId)
                        .update("updatedAt", Date())
                        .await()

                    val itemMap = mapOf(
                        "id" to item.id,
                        "name" to item.name,
                        "quantity" to item.quantity,
                        "category" to item.category,
                        "isChecked" to item.isChecked,
                        "createdAt" to item.createdAt
                    )

                    listsCollection.document(listId)
                        .collection("items")
                        .document(item.id)
                        .set(itemMap)
                        .await()

                    // Marquer comme synchronisé
                    shoppingItemDao.markItemAsSynced(item.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la mise à jour sur Firebase", e)
                }
            }

            Log.d(TAG, "Article mis à jour avec succès: ${item.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour de l'article", e)
            throw e
        }
    }

    /**
     * Supprimer un article
     */
    suspend fun deleteItem(listId: String, itemId: String) {
        try {
            // Mettre à jour la date de modification de la liste
            shoppingListDao.updateTimestamp(listId, Date().time)

            // Supprimer l'article localement
            shoppingItemDao.deleteByListIdAndItemId(listId, itemId)

            // Supprimer sur Firebase si en ligne
            if (NetworkMonitor.isOnline) {
                try {
                    listsCollection.document(listId)
                        .update("updatedAt", Date())
                        .await()

                    listsCollection.document(listId)
                        .collection("items")
                        .document(itemId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la suppression sur Firebase", e)
                }
            }

            Log.d(TAG, "Article supprimé avec succès: $itemId")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression de l'article", e)
            throw e
        }
    }

    /**
     * Récupérer un article par son ID
     */
    suspend fun getItemById(listId: String, itemId: String): ShoppingItem? {
        return shoppingItemDao.getItemById(itemId)?.toModel()
    }

    /**
     * Récupérer le nom d'une liste
     */
    suspend fun getListName(listId: String): String? {
        return shoppingListDao.getListName(listId)
    }

    /**
     * Récupérer une liste par son ID
     */
    suspend fun getListById(listId: String): ShoppingList? {
        val listEntity = shoppingListDao.getListById(listId) ?: return null
        return listEntity.toModel()
    }

    /**
     * Ajouter un produit à l'historique des suggestions
     */
    suspend fun addToProductSuggestions(productName: String) {
        if (productName.isBlank()) return

        val userId = getCurrentUserId()
        val exists = userDao.productSuggestionExists(userId, productName)
        val suggestion = ProductSuggestionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = productName,
            updatedAt = Date()
        )

        // Sauvegarder localement
        userDao.insertProductSuggestion(suggestion)

        // Synchroniser avec Firebase si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                if (exists) {
                    val querySnapshot = suggestionsCollection
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("name", productName)
                        .get()
                        .await()

                    if (!querySnapshot.isEmpty) {
                        val docId = querySnapshot.documents[0].id
                        suggestionsCollection.document(docId)
                            .update("updatedAt", Date())
                            .await()
                    } else {
                        addSuggestionToFirebase(userId, productName)
                    }
                } else {
                    addSuggestionToFirebase(userId, productName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation de la suggestion", e)
            }
        }
    }

    /**
     * Ajoute une suggestion à Firebase
     */
    private suspend fun addSuggestionToFirebase(userId: String, productName: String) {
        val suggestionData = hashMapOf(
            "userId" to userId,
            "name" to productName,
            "createdAt" to Date(),
            "updatedAt" to Date()
        )
        suggestionsCollection.add(suggestionData).await()
    }

    /**
     * Récupérer les suggestions de produits
     */
    suspend fun getProductSuggestions(): List<String> {
        val userId = getCurrentUserId()
        return userDao.getProductSuggestionNames(userId, 20)
    }

    /**
     * Synchroniser les données non synchronisées
     */
    suspend fun syncData() {
        if (!NetworkMonitor.isOnline) return

        try {
            // Vérifier si l'utilisateur est authentifié
            val currentUser = auth.currentUser ?: return
            Log.d(TAG, "Synchronisation des données pour: ${currentUser.uid}")

            syncUnsyncedLists(currentUser.uid)
            syncUnsyncedItems(currentUser.uid)

            Log.d(TAG, "Synchronisation terminée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la synchronisation", e)
        }
    }

    /**
     * Synchronise les listes non synchronisées
     */
    private suspend fun syncUnsyncedLists(userId: String) {
        val unsyncedLists = shoppingListDao.getUnsyncedLists()
        for (list in unsyncedLists) {
            try {
                // Vérifier que l'utilisateur est propriétaire de la liste
                if (list.userId != userId) {
                    Log.w(TAG, "Utilisateur non autorisé à synchroniser la liste: ${list.id}")
                    continue
                }

                val listData = hashMapOf(
                    "id" to list.id,
                    "userId" to list.userId,
                    "name" to list.name,
                    "createdAt" to list.createdAt,
                    "updatedAt" to list.updatedAt,
                    "isShared" to list.isShared,
                    "ownerId" to list.ownerId
                )

                listsCollection.document(list.id)
                    .set(listData)
                    .await()

                shoppingListDao.markListAsSynced(list.id)
                Log.d(TAG, "Liste synchronisée: ${list.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation de la liste ${list.id}", e)
            }
        }
    }

    /**
     * Synchronise les articles non synchronisés
     */
    private suspend fun syncUnsyncedItems(userId: String) {
        val unsyncedItems = shoppingItemDao.getUnsyncedItems()
        for (item in unsyncedItems) {
            try {
                // Vérifier si l'utilisateur a le droit d'accéder à cette liste
                val listEntity = shoppingListDao.getListById(item.listId)
                if (listEntity == null || listEntity.userId != userId) {
                    Log.w(TAG, "Utilisateur non autorisé à synchroniser l'article: ${item.id}")
                    continue
                }

                val itemModel = item.toModel()
                val itemMap = mapOf(
                    "id" to itemModel.id,
                    "name" to itemModel.name,
                    "quantity" to itemModel.quantity,
                    "category" to itemModel.category,
                    "isChecked" to itemModel.isChecked,
                    "createdAt" to itemModel.createdAt
                )

                listsCollection.document(item.listId)
                    .collection("items")
                    .document(item.id)
                    .set(itemMap)
                    .await()

                shoppingItemDao.markItemAsSynced(item.id)
                Log.d(TAG, "Article synchronisé: ${item.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation de l'article ${item.id}", e)
            }
        }
    }
}