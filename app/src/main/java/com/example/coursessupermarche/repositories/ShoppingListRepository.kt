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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao,
    private val userDao: UserDao
) {

    private val TAG = "ShoppingListRepository"

    // Collection pour les listes de courses
    private val listsCollection = firestore.collection("lists")

    // Collection pour les suggestions de produits
    private val suggestionsCollection = firestore.collection("product_suggestions")

    // Obtenir l'ID de l'utilisateur courant
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Aucun utilisateur connecté")
    }

    // Sauvegarder l'utilisateur courant en local
    suspend fun saveCurrentUser() {
        val firebaseUser = auth.currentUser ?: return

        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString()
        )

        userDao.insert(UserEntity.fromModel(user))
    }

    // Obtenir ou créer une liste principale pour l'utilisateur courant
    suspend fun getCurrentUserMainListId(): String {
        val userId = getCurrentUserId()

        // Essayer d'abord d'obtenir la liste locale
        val localList = shoppingListDao.getLatestListByUserId(userId)
        if (localList != null) {
            return localList.id
        }

        // Si online, essayer d'obtenir la liste du serveur
        if (NetworkMonitor.isOnline) {
            try {
                // Chercher une liste existante
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
                        isRemotelySync = true
                    )

                    shoppingListDao.insert(list)
                    return listId
                }

                // Sinon, créer une nouvelle liste
                val newList = hashMapOf(
                    "userId" to userId,
                    "name" to "Ma liste de courses",
                    "createdAt" to Date(),
                    "updatedAt" to Date()
                )

                val result = listsCollection.add(newList).await()
                val listId = result.id

                // Sauvegarder en local
                val list = ShoppingListEntity(
                    id = listId,
                    userId = userId,
                    name = "Ma liste de courses",
                    createdAt = Date(),
                    updatedAt = Date(),
                    isRemotelySync = true
                )

                shoppingListDao.insert(list)
                return listId
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de la liste", e)
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
            isRemotelySync = false
        )

        shoppingListDao.insert(list)
        return listId
    }

    // Observer les changements dans la liste d'articles
    fun observeShoppingItems(listId: String): Flow<List<ShoppingItem>> {
        return shoppingItemDao.getItemsByListId(listId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    // Ajouter un nouvel article à la liste
    suspend fun addItem(listId: String, item: ShoppingItem) {
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
            try {
                listsCollection.document(listId)
                    .update("updatedAt", Date())
                    .await()

                listsCollection.document(listId)
                    .collection("items")
                    .document(item.id)
                    .set(item)
                    .await()

                // Marquer comme synchronisé
                shoppingItemDao.markItemAsSynced(item.id)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'ajout de l'article", e)
            }
        }

        // Ajouter en historique pour les suggestions futures
        addToProductSuggestions(item.name)
    }

    // Mettre à jour un article existant
    suspend fun updateItem(listId: String, item: ShoppingItem) {
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

                listsCollection.document(listId)
                    .collection("items")
                    .document(item.id)
                    .set(item)
                    .await()

                // Marquer comme synchronisé
                shoppingItemDao.markItemAsSynced(item.id)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la mise à jour de l'article", e)
            }
        }
    }

    // Supprimer un article
    suspend fun deleteItem(listId: String, itemId: String) {
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
                Log.e(TAG, "Erreur lors de la suppression de l'article", e)
            }
        }
    }

    // Récupérer un article par son ID
    suspend fun getItemById(listId: String, itemId: String): ShoppingItem? {
        return shoppingItemDao.getItemById(itemId)?.toModel()
    }

    // Ajouter un produit à l'historique des suggestions
    suspend fun addToProductSuggestions(productName: String) {
        val userId = getCurrentUserId()

        // Vérifier si ce produit existe déjà localement
        val exists = userDao.productSuggestionExists(userId, productName)

        // Si le produit existe déjà, mettre à jour sa date
        if (exists) {
            val suggestion = ProductSuggestionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = productName,
                updatedAt = Date()
            )
            userDao.insertProductSuggestion(suggestion)

            // Mettre à jour sur Firebase si en ligne
            if (NetworkMonitor.isOnline) {
                try {
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
                        val suggestion = hashMapOf(
                            "userId" to userId,
                            "name" to productName,
                            "createdAt" to Date(),
                            "updatedAt" to Date()
                        )
                        suggestionsCollection.add(suggestion).await()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la mise à jour de la suggestion", e)
                }
            }
            return
        }

        // Sinon, ajouter un nouveau produit localement
        val suggestion = ProductSuggestionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = productName,
            updatedAt = Date()
        )
        userDao.insertProductSuggestion(suggestion)

        // Ajouter sur Firebase si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                val suggestionData = hashMapOf(
                    "userId" to userId,
                    "name" to productName,
                    "createdAt" to Date(),
                    "updatedAt" to Date()
                )
                suggestionsCollection.add(suggestionData).await()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'ajout de la suggestion", e)
            }
        }
    }

    // Récupérer les suggestions de produits
    suspend fun getProductSuggestions(): List<String> {
        val userId = getCurrentUserId()
        return userDao.getProductSuggestionNames(userId, 20)
    }

    // Synchroniser les données non synchronisées
    suspend fun syncUnsyncedData() {
        if (!NetworkMonitor.isOnline) return

        try {
            // Synchroniser les listes
            val unsyncedLists = shoppingListDao.getUnsyncedLists()
            for (list in unsyncedLists) {
                val listData = hashMapOf(
                    "userId" to list.userId,
                    "name" to list.name,
                    "createdAt" to list.createdAt,
                    "updatedAt" to list.updatedAt
                )

                listsCollection.document(list.id)
                    .set(listData)
                    .await()

                shoppingListDao.markListAsSynced(list.id)
            }

            // Synchroniser les articles
            val unsyncedItems = shoppingItemDao.getUnsyncedItems()
            for (item in unsyncedItems) {
                val itemModel = item.toModel()

                listsCollection.document(item.listId)
                    .collection("items")
                    .document(item.id)
                    .set(itemModel)
                    .await()

                shoppingItemDao.markItemAsSynced(item.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la synchronisation", e)
        }
    }
}