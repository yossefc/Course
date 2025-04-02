package com.example.coursessupermarche.models

import com.example.coursessupermarche.R
import com.google.firebase.firestore.DocumentId
import java.util.Date
import java.util.UUID

/**
 * Représente une liste de courses
 */
data class ShoppingList(
    @DocumentId val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val items: List<ShoppingItem> = emptyList(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val totalItems: Int
        get() = items.size

    val completedItems: Int
        get() = items.count { it.isChecked }

    val progress: Float
        get() = if (items.isEmpty()) 0f else completedItems.toFloat() / totalItems
}

/**
 * Représente un article dans la liste de courses
 */
data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: Int = 1,
    val category: String = Categories.OTHER,
    val isChecked: Boolean = false,
    val createdAt: Date = Date()
)

/**
 * Catégories prédéfinies pour les articles
 */
object Categories {
    const val DAIRY = "Produits laitiers"
    const val FRUITS_VEGETABLES = "Fruits et légumes"
    const val MEAT = "Viandes"
    const val FISH = "Poissons"
    const val BAKERY = "Boulangerie"
    const val FROZEN = "Surgelés"
    const val DRINKS = "Boissons"
    const val CLEANING = "Produits ménagers"
    const val HYGIENE = "Hygiène"
    const val OTHER = "Autres"

    fun getAllCategories(): List<String> {
        return listOf(
            DAIRY, FRUITS_VEGETABLES, MEAT, FISH,
            BAKERY, FROZEN, DRINKS, CLEANING, HYGIENE, OTHER
        )
    }

    fun getCategoryIcon(category: String): Int {
        // Utiliser ic_notification comme placeholder pour toutes les catégories
        // jusqu'à ce que les icônes spécifiques soient créées
        return R.drawable.ic_notification
    }
}

/**
 * Modèle pour l'utilisateur
 */
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null
)