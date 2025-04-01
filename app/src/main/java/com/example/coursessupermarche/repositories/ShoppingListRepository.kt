package com.example.coursessupermarche.repositories

import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.models.ShoppingList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

class ShoppingListRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Collection pour les listes de courses
    private val listsCollection = firestore.collection("lists")

    // Collection pour les suggestions de produits
    private val suggestionsCollection = firestore.collection("product_suggestions")

    // Obtenir l'ID de l'utilisateur courant
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Aucun utilisateur connecté")
    }

    // Obtenir ou créer une liste principale pour l'utilisateur courant
    suspend fun getCurrentUserMainListId(): String {
        val userId = getCurrentUserId()

        // Chercher une liste existante
        val querySnapshot = listsCollection
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()

        // Si une liste existe, la retourner
        if (!querySnapshot.isEmpty) {
            return querySnapshot.documents[0].id
        }

        // Sinon, créer une nouvelle liste
        val newList = hashMapOf(
            "userId" to userId,
            "name" to "Ma liste de courses",
            "createdAt" to Date(),
            "updatedAt" to Date()
        )

        val result = listsCollection.add(newList).await()
        return result.id
    }

    // Observer les changements dans la liste d'articles
    fun observeShoppingItems(
        listId: String,
        callback: (List<ShoppingItem>, Exception?) -> Unit
    ) {
        listsCollection.document(listId)
            .collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    callback(emptyList(), exception)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject(ShoppingItem::class.java)
                    item?.copy(id = doc.id)
                } ?: emptyList()

                callback(items, null)
            }
    }

    // Ajouter un nouvel article à la liste
    suspend fun addItem(listId: String, item: ShoppingItem) {
        // Mettre à jour la date de modification de la liste
        listsCollection.document(listId)
            .update("updatedAt", Date())
            .await()

        // Ajouter l'article
        listsCollection.document(listId)
            .collection("items")
            .document(item.id)
            .set(item)
            .await()
    }

    // Mettre à jour un article existant
    suspend fun updateItem(listId: String, item: ShoppingItem) {
        // Mettre à jour la date de modification de la liste
        listsCollection.document(listId)
            .update("updatedAt", Date())
            .await()

        // Mettre à jour l'article
        listsCollection.document(listId)
            .collection("items")
            .document(item.id)
            .set(item)
            .await()
    }

    // Supprimer un article
    suspend fun deleteItem(listId: String, itemId: String) {
        // Mettre à jour la date de modification de la liste
        listsCollection.document(listId)
            .update("updatedAt", Date())
            .await()

        // Supprimer l'article
        listsCollection.document(listId)
            .collection("items")
            .document(itemId)
            .delete()
            .await()
    }

    // Récupérer un article par son ID
    suspend fun getItemById(listId: String, itemId: String): ShoppingItem? {
        val documentSnapshot = listsCollection.document(listId)
            .collection("items")
            .document(itemId)
            .get()
            .await()

        return if (documentSnapshot.exists()) {
            val item = documentSnapshot.toObject(ShoppingItem::class.java)
            item?.copy(id = documentSnapshot.id)
        } else {
            null
        }
    }

    // Ajouter un produit à l'historique des suggestions
    suspend fun addToProductSuggestions(productName: String) {
        val userId = getCurrentUserId()

        // Vérifier si ce produit existe déjà
        val querySnapshot = suggestionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("name", productName)
            .get()
            .await()

        // Si le produit existe déjà, mettre à jour sa date
        if (!querySnapshot.isEmpty) {
            val docId = querySnapshot.documents[0].id
            suggestionsCollection.document(docId)
                .update("updatedAt", Date())
                .await()
            return
        }

        // Sinon, ajouter un nouveau produit
        val suggestion = hashMapOf(
            "userId" to userId,
            "name" to productName,
            "createdAt" to Date(),
            "updatedAt" to Date()
        )

        suggestionsCollection.add(suggestion).await()
    }

    // Récupérer les suggestions de produits
    suspend fun getProductSuggestions(): List<String> {
        val userId = getCurrentUserId()

        val querySnapshot = suggestionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        return querySnapshot.documents.mapNotNull { doc ->
            doc.getString("name")
        }
    }
}