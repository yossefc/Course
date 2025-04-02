package com.example.coursessupermarche.models

import java.util.Date

// Représente un membre d'une liste partagée
data class ListMember(
    val userId: String,
    val email: String = "",
    val displayName: String = "",
    val joinedAt: Date = Date(),
    val role: MemberRole = MemberRole.MEMBER
)