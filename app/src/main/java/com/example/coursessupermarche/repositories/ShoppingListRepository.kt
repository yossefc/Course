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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // Observer les changements dans la liste d'articles
    fun observeShoppingItems(listId: String): Flow<List<ShoppingItem>> {
        Log.d(TAG, "Observation démarrée pour la liste: $listId")
        return shoppingItemDao.getItemsByListId(listId)
            .map { entities ->
                Log.d(TAG, "Nouvelles données: ${entities.size} éléments")
                entities.map { it.toModel() }
            }
            .distinctUntilChanged() // Ne déclenche que lorsque les données changent vraiment
            .catch { exception ->
                Log.e(TAG, "Erreur dans le flux d'observation", exception)
                emit(emptyList()) // Émettre une liste vide en cas d'erreur
            }
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

    // Ajouter un nouvel article à la liste avec journalisation détaillée
    suspend fun addItem(listId: String, item: ShoppingItem) {
        Log.d(TAG, "==== DÉBUT AJOUT PRODUIT ====")
        Log.d(TAG, "Produit à ajouter: ID=${item.id}, Nom=${item.name}, Catégorie=${item.category}")
        Log.d(TAG, "Liste cible: ID=$listId")
        Log.d(TAG, "Statut réseau: ${if (NetworkMonitor.isOnline) "EN LIGNE" else "HORS LIGNE"}")
        Log.d(TAG, "Utilisateur connecté: ${auth.currentUser?.uid ?: "NON CONNECTÉ"}")

        try {
            // Mettre à jour la date de modification de la liste
            shoppingListDao.updateTimestamp(listId, Date().time)
            Log.d(TAG, "✓ Timestamp de liste mis à jour dans la base locale")

            // Ajouter l'article localement
            val entity = ShoppingItemEntity.fromModel(
                model = item,
                listId = listId,
                isRemotelySync = false // Commencer avec non-synchronisé
            )
            shoppingItemDao.insert(entity)
            Log.d(TAG, "✓ Article ajouté dans la base locale")

            // Ajouter à Firebase si en ligne
            if (NetworkMonitor.isOnline) {
                try {
                    auth.currentUser?.let { user ->
                        Log.d(TAG, "Tentative de synchronisation avec Firebase...")

                        // Vérifier que la liste existe dans Firestore
                        val listDoc = listsCollection.document(listId).get().await()
                        if (!listDoc.exists()) {
                            Log.w(TAG, "⚠️ La liste n'existe pas dans Firestore, création...")
                            val listData = hashMapOf(
                                "userId" to user.uid,
                                "name" to "Ma liste de courses",
                                "createdAt" to Date(),
                                "updatedAt" to Date()
                            )
                            listsCollection.document(listId).set(listData).await()
                            Log.d(TAG, "✓ Liste créée dans Firestore")
                        }

                        // Mettre à jour le timestamp de la liste
                        listsCollection.document(listId)
                            .update("updatedAt", Date())
                            .await()
                        Log.d(TAG, "✓ Timestamp de liste mis à jour dans Firestore")

                        // Ajouter l'élément dans la sous-collection items
                        val itemCollection = listsCollection.document(listId).collection("items")

                        // Convertir en Map pour le débogage
                        val itemMap = mapOf(
                            "id" to item.id,
                            "name" to item.name,
                            "quantity" to item.quantity,
                            "category" to item.category,
                            "isChecked" to item.isChecked,
                            "createdAt" to item.createdAt
                        )
                        Log.d(TAG, "Données à envoyer à Firestore: $itemMap")

                        // Ajouter le document
                        itemCollection.document(item.id)
                            .set(itemMap)
                            .await()
                        Log.d(TAG, "✓ Article ajouté dans Firestore - document ID: ${item.id}")

                        // Marquer comme synchronisé
                        shoppingItemDao.markItemAsSynced(item.id)
                        Log.d(TAG, "✓ Article marqué comme synchronisé dans la base locale")
                    } ?: run {
                        Log.w(TAG, "⚠️ Pas d'utilisateur connecté pour la synchronisation")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ ERREUR lors de l'ajout à Firebase: ${e.javaClass.simpleName} - ${e.message}", e)
                    // Continuer quand même - l'article est sauvegardé localement
                }
            } else {
                Log.d(TAG, "ℹ️ Mode hors ligne: ajout seulement local")
            }

            // Ajouter en historique pour les suggestions futures
            try {
                addToProductSuggestions(item.name)
                Log.d(TAG, "✓ Produit ajouté aux suggestions")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERREUR lors de l'ajout aux suggestions: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERREUR CRITIQUE lors de l'ajout du produit: ${e.message}", e)
            throw e  // Relancer pour que le ViewModel puisse gérer l'erreur
        } finally {
            Log.d(TAG, "==== FIN AJOUT PRODUIT ====")
        }
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
    suspend fun syncData() {
        if (!NetworkMonitor.isOnline) return

        try {
            // Vérifier si l'utilisateur est authentifié
            val currentUser = auth.currentUser ?: return
            Log.d(TAG, "Début de la synchronisation pour l'utilisateur: ${currentUser.uid}")

            // Synchroniser les listes
            val unsyncedLists = shoppingListDao.getUnsyncedLists()
            for (list in unsyncedLists) {
                try {
                    // Vérifier que l'utilisateur est propriétaire de la liste
                    if (list.userId != currentUser.uid) {
                        Log.w(TAG, "Skipping list sync - permission denied")
                        continue
                    }

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
                    Log.d(TAG, "Liste synchronisée: ${list.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la synchronisation de la liste ${list.id}", e)
                }
            }

            // Synchroniser les articles
            val unsyncedItems = shoppingItemDao.getUnsyncedItems()
            for (item in unsyncedItems) {
                try {
                    // Vérifier si l'utilisateur a le droit d'accéder à cette liste
                    val listEntity = shoppingListDao.getListById(item.listId)
                    if (listEntity == null || listEntity.userId != currentUser.uid) {
                        Log.w(TAG, "Skipping item sync - permission denied")
                        continue
                    }

                    val itemModel = item.toModel()

                    // Convertir en Map pour le débogage et l'envoi
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
        } catch (e: Exception) {
            Log.e(TAG, "Erreur globale lors de la synchronisation", e)
        }
    }
}