package com.example.coursessupermarche.models

import com.google.firebase.firestore.DocumentId
import java.util.Date
import java.util.UUID

// Extension du modèle ShoppingList pour inclure les informations de partage
data class ShoppingList(
    @DocumentId val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val items: List<ShoppingItem> = emptyList(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val ownerId: String = "", // ID de l'utilisateur qui a créé la liste
    val members: List<ListMember> = emptyList(), // Membres autorisés à accéder à la liste
    val isShared: Boolean = false // Indique si la liste est partagée
) {
    val totalItems: Int
        get() = items.size

    val completedItems: Int
        get() = items.count { it.isChecked }

    val progress: Float
        get() = if (items.isEmpty()) 0f else completedItems.toFloat() / totalItems
}