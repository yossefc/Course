package com.example.coursessupermarche.models

object Categories {
    const val DAIRY = "Produits laitiers"
    const val FRUITS_VEGETABLES = "Fruits et légumes"
    const val MEAT = "Viandes"
    const val FISH = "Poissons"
    const val BAKERY = "Boulangerie"
    const val FROZEN = "Surgelés"
    const val DRINKS = "Boissons"
    const val CLEANING = "Produits ménagers"
    const val HYGIENE = "Hygiène"
    const val OTHER = "Autres"

    fun getAllCategories(): List<String> {
        return listOf(
            DAIRY, FRUITS_VEGETABLES, MEAT, FISH,
            BAKERY, FROZEN, DRINKS, CLEANING, HYGIENE, OTHER
        )
    }
}