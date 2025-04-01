package com.example.coursessupermarche.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.coursessupermarche.data.local.entities.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY category, name")
    fun getItemsByListId(listId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity)

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND id = :itemId")
    suspend fun deleteByListIdAndItemId(listId: String, itemId: String)

    @Query("UPDATE shopping_items SET isChecked = :isChecked WHERE id = :itemId")
    suspend fun updateCheckStatus(itemId: String, isChecked: Boolean)

    @Query("SELECT * FROM shopping_items WHERE isRemotelySync = 0")
    suspend fun getUnsyncedItems(): List<ShoppingItemEntity>

    @Query("UPDATE shopping_items SET isRemotelySync = 1 WHERE id = :itemId")
    suspend fun markItemAsSynced(itemId: String)
}