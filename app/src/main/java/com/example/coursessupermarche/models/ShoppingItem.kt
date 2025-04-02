package com.example.coursessupermarche.models

import java.util.Date
import java.util.UUID

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: Int = 1,
    val category: String = Categories.OTHER,
    val isChecked: Boolean = false,
    val createdAt: Date = Date()
)