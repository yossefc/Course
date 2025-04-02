package com.example.coursessupermarche.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.coursessupermarche.data.local.entities.ShoppingListEntity
import com.example.coursessupermarche.data.local.entities.ShoppingListWithMembers
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ShoppingListDao {
    // Méthodes existantes
    @Query("SELECT * FROM shopping_lists WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getListsByUserId(userId: String): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId LIMIT 1")
    suspend fun getListById(listId: String): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ShoppingListEntity)

    @Update
    suspend fun update(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteById(listId: String)

    @Query("UPDATE shopping_lists SET updatedAt = :updatedAt WHERE id = :listId")
    suspend fun updateTimestamp(listId: String, updatedAt: Long)

    @Query("SELECT * FROM shopping_lists WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestListByUserId(userId: String): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists WHERE isRemotelySync = 0")
    suspend fun getUnsyncedLists(): List<ShoppingListEntity>

    @Query("SELECT name FROM shopping_lists WHERE id = :listId")
    suspend fun getListName(listId: String): String?

    @Query("UPDATE shopping_lists SET isRemotelySync = 1 WHERE id = :listId")
    suspend fun markListAsSynced(listId: String)

    // Nouvelles méthodes pour le partage de listes
    @Query("SELECT * FROM shopping_lists WHERE userId = :userId OR id IN (SELECT listId FROM list_members WHERE userId = :userId) ORDER BY updatedAt DESC")
    fun getAccessibleListsByUserId(userId: String): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE ownerId = :userId ORDER BY updatedAt DESC")
    fun getOwnedListsByUserId(userId: String): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId AND (userId = :userId OR id IN (SELECT listId FROM list_members WHERE userId = :userId)) LIMIT 1")
    suspend fun getAccessibleListById(listId: String, userId: String): ShoppingListEntity?

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getListWithMembers(listId: String): ShoppingListWithMembers?

    @Query("UPDATE shopping_lists SET isShared = :isShared WHERE id = :listId")
    suspend fun updateSharedStatus(listId: String, isShared: Boolean)

    @Query("UPDATE shopping_lists SET name = :name WHERE id = :listId")
    suspend fun updateListName(listId: String, name: String)

    @Query("SELECT COUNT(*) FROM shopping_lists WHERE ownerId = :userId AND id = :listId")
    suspend fun isOwner(userId: String, listId: String): Int


}