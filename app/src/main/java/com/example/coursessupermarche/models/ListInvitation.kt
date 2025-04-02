package com.example.coursessupermarche.models

import java.util.Date
import java.util.UUID

// Modèle pour les invitations à rejoindre une liste
data class ListInvitation(
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val listName: String,
    val inviterId: String,
    val inviterName: String,
    val createdAt: Date = Date(),
    val expiresAt: Date, // Date d'expiration (7 jours après création)
    val token: String, // Token unique pour le lien d'invitation
    val status: InvitationStatus = InvitationStatus.PENDING
)