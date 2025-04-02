package com.example.coursessupermarche.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.coursessupermarche.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String)

    // Suggestions de produits

    @Query("SELECT * FROM product_suggestions WHERE userId = :userId ORDER BY updatedAt DESC LIMIT :limit")
    fun getProductSuggestionsByUserId(userId: String, limit: Int = 20): Flow<List<ProductSuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductSuggestion(suggestion: ProductSuggestionEntity)

    @Query("SELECT name FROM product_suggestions WHERE userId = :userId ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getProductSuggestionNames(userId: String, limit: Int = 20): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM product_suggestions WHERE userId = :userId AND name = :name LIMIT 1)")
    suspend fun productSuggestionExists(userId: String, name: String): Boolean
}