package com.example.coursessupermarche.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation

data class ShoppingListWithMembers(
    @Embedded val list: ShoppingListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val members: List<ListMemberEntity>
)