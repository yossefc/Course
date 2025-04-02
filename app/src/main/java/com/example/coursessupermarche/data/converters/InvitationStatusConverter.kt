package com.example.coursessupermarche.data.local.converters

import androidx.room.TypeConverter
import com.example.coursessupermarche.models.InvitationStatus

class InvitationStatusConverter {
    @TypeConverter
    fun fromInvitationStatus(status: InvitationStatus): String {
        return status.name
    }

    @TypeConverter
    fun toInvitationStatus(statusName: String): InvitationStatus {
        return try {
            InvitationStatus.valueOf(statusName)
        } catch (e: IllegalArgumentException) {
            InvitationStatus.EXPIRED // Valeur par défaut en cas d'erreur
        }
    }
}