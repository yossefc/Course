package com.example.coursessupermarche.models

import com.example.coursessupermarche.R

/**
 * Classe utilitaire pour les icônes de catégories
 * Utilisation temporaire jusqu'à ce que les icônes réelles soient créées
 */
object CategoryIcons {
    // Définition d'une ressource par défaut pour éviter les erreurs de références
    // À remplacer par des vraies icônes quand elles seront disponibles

    // Cette classe ajoute un niveau d'indirection pour éviter les références
    // directes aux drawables qui n'existent pas encore
    object R {
        // Ici nous utilisons ic_notification comme fallback pour toutes les catégories
        val ic_dairy = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_vegetables = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_meat = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_fish = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_bakery = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_frozen = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_drinks = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_cleaning = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_hygiene = com.example.coursessupermarche.R.drawable.ic_notification
        val ic_other = com.example.coursessupermarche.R.drawable.ic_notification
    }
}