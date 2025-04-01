package com.example.coursessupermarche.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.coursessupermarche.data.local.entities.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

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

    @Query("UPDATE shopping_lists SET isRemotelySync = 1 WHERE id = :listId")
    suspend fun markListAsSynced(listId: String)
}