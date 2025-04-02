package com.example.coursessupermarche.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import com.example.coursessupermarche.data.local.entities.ProductSuggestionEntity

/**
 * Cette entité représente une suggestion de produit pour l'autocomplétion
 */
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