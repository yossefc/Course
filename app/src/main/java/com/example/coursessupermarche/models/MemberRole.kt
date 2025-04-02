package com.example.coursessupermarche.models

// Rôles possibles pour les membres d'une liste
enum class MemberRole {
    OWNER, // Créateur de la liste avec droits complets (supprimer la liste)
    MEMBER  // Membre avec droits d'édition (ajouter/modifier/supprimer des éléments)
}