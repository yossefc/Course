package com.example.coursessupermarche.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.models.ShoppingList
import com.example.coursessupermarche.models.User
import java.util.Date

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val createdAt: Date,
    val updatedAt: Date,
    val isRemotelySync: Boolean = true
) {
    fun toModel(items: List<ShoppingItem> = emptyList()): ShoppingList {
        return ShoppingList(
            id = id,
            name = name,
            items = items,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromModel(model: ShoppingList, userId: String, isRemotelySync: Boolean = true): ShoppingListEntity {
            return ShoppingListEntity(
                id = model.id,
                userId = userId,
                name = model.name,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                isRemotelySync = isRemotelySync
            )
        }
    }
}

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val name: String,
    val quantity: Int,
    val category: String,
    val isChecked: Boolean,
    val createdAt: Date,
    val isRemotelySync: Boolean = true
) {
    fun toModel(): ShoppingItem {
        return ShoppingItem(
            id = id,
            name = name,
            quantity = quantity,
            category = category,
            isChecked = isChecked,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromModel(model: ShoppingItem, listId: String, isRemotelySync: Boolean = true): ShoppingItemEntity {
            return ShoppingItemEntity(
                id = model.id,
                listId = listId,
                name = model.name,
                quantity = model.quantity,
                category = model.category,
                isChecked = model.isChecked,
                createdAt = model.createdAt,
                isRemotelySync = isRemotelySync
            )
        }
    }
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String
) {
    fun toModel(): User {
        return User(
            id = id,
            email = email,
            displayName = displayName
        )
    }

    companion object {
        fun fromModel(model: User): UserEntity {
            return UserEntity(
                id = model.id,
                email = model.email,
                displayName = model.displayName
            )
        }
    }
}

@Entity(tableName = "product_suggestions")
data class ProductSuggestionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val updatedAt: Date
)