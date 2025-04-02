package com.example.coursessupermarche.models

// Statut d'une invitation
enum class InvitationStatus {
    PENDING,   // En attente d'acceptation
    ACCEPTED,  // Acceptée par l'utilisateur invité
    EXPIRED,   // Expirée (7 jours écoulés)
    REVOKED    // Révoquée par le créateur
}