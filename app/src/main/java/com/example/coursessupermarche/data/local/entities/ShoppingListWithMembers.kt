package com.example.coursessupermarche.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

// Classe pour le résultat de la relation entre liste et membres
data class ShoppingListWithMembers(
    @Embedded val list: ShoppingListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val members: List<ListMemberEntity>
)