package com.example.coursessupermarche.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.coursessupermarche.models.InvitationStatus
import com.example.coursessupermarche.models.ListInvitation
import com.example.coursessupermarche.models.ListMember
import com.example.coursessupermarche.models.MemberRole
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.models.ShoppingList
import com.example.coursessupermarche.models.User
import java.util.Date

// Entité pour les listes de courses
@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val createdAt: Date,
    val updatedAt: Date,
    val isRemotelySync: Boolean = true,
    val isShared: Boolean = false,
    val ownerId: String
) {
    fun toModel(items: List<ShoppingItem> = emptyList(), members: List<ListMember> = emptyList()): ShoppingList {
        return ShoppingList(
            id = id,
            name = name,
            items = items,
            createdAt = createdAt,
            updatedAt = updatedAt,
            ownerId = ownerId,
            members = members,
            isShared = isShared
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
                isRemotelySync = isRemotelySync,
                isShared = model.isShared,
                ownerId = model.ownerId
            )
        }
    }
}

// Entité pour les articles dans une liste
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

// Entité pour les utilisateurs
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
) {
    fun toModel(): User {
        return User(
            id = id,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl
        )
    }

    companion object {
        fun fromModel(model: User): UserEntity {
            return UserEntity(
                id = model.id,
                email = model.email,
                displayName = model.displayName,
                photoUrl = model.photoUrl
            )
        }
    }
}

// Entité pour les suggestions de produits
@Entity(
    tableName = "product_suggestions",
    indices = [Index("userId")]
)
data class ProductSuggestionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val updatedAt: Date
)

// Entité pour les membres d'une liste
@Entity(
    tableName = "list_members",
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
data class ListMemberEntity(
    @PrimaryKey val id: String,  // userId + listId
    val listId: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val joinedAt: Date,
    val role: MemberRole,
    val isRemotelySync: Boolean = true
) {
    fun toModel(): ListMember {
        return ListMember(
            userId = userId,
            email = email,
            displayName = displayName,
            joinedAt = joinedAt,
            role = role
        )
    }

    companion object {
        fun fromModel(listId: String, model: ListMember, isRemotelySync: Boolean = true): ListMemberEntity {
            return ListMemberEntity(
                id = "${model.userId}_${listId}",
                listId = listId,
                userId = model.userId,
                email = model.email,
                displayName = model.displayName,
                joinedAt = model.joinedAt,
                role = model.role,
                isRemotelySync = isRemotelySync
            )
        }
    }
}

// Entité pour les invitations à rejoindre une liste
@Entity(
    tableName = "list_invitations",
    indices = [Index("listId")]
)
data class ListInvitationEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val listName: String,
    val inviterId: String,
    val inviterName: String,
    val createdAt: Date,
    val expiresAt: Date,
    val token: String,
    val status: InvitationStatus,
    val isRemotelySync: Boolean = true
) {
    fun toModel(): ListInvitation {
        return ListInvitation(
            id = id,
            listId = listId,
            listName = listName,
            inviterId = inviterId,
            inviterName = inviterName,
            createdAt = createdAt,
            expiresAt = expiresAt,
            token = token,
            status = status
        )
    }

    companion object {
        fun fromModel(model: ListInvitation, isRemotelySync: Boolean = true): ListInvitationEntity {
            return ListInvitationEntity(
                id = model.id,
                listId = model.listId,
                listName = model.listName,
                inviterId = model.inviterId,
                inviterName = model.inviterName,
                createdAt = model.createdAt,
                expiresAt = model.expiresAt,
                token = model.token,
                status = model.status,
                isRemotelySync = isRemotelySync
            )
        }
    }
}