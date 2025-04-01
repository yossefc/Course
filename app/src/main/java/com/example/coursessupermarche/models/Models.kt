package com.example.coursessupermarche.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Représente une liste de courses
 */
data class ShoppingList(
    @DocumentId val id: String = "",
    val name: String = "",
    val items: List<ShoppingItem> = emptyList(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * Représente un article dans la liste de courses
 */
data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val category: String = "",
    val isChecked: Boolean = false,
    val createdAt: Date = Date()
)

/**
 * Catégories prédéfinies pour les articles
 */
object Categories {
    val DAIRY = "Produits laitiers"
    val FRUITS_VEGETABLES = "Fruits et légumes"
    val MEAT = "Viandes"
    val FISH = "Poissons"
    val BAKERY = "Boulangerie"
    val FROZEN = "Surgelés"
    val DRINKS = "Boissons"
    val CLEANING = "Produits ménagers"
    val HYGIENE = "Hygiène"
    val OTHER = "Autres"

    fun getAllCategories(): List<String> {
        return listOf(
            DAIRY, FRUITS_VEGETABLES, MEAT, FISH,
            BAKERY, FROZEN, DRINKS, CLEANING, HYGIENE, OTHER
        )
    }
}

/**
 * Modèle pour l'utilisateur
 */
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = ""
)