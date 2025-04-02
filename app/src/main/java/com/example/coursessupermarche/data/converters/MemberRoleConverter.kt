package com.example.coursessupermarche.data.local.converters

import androidx.room.TypeConverter
import com.example.coursessupermarche.models.MemberRole

class MemberRoleConverter {
    @TypeConverter
    fun fromMemberRole(role: MemberRole): String {
        return role.name
    }

    @TypeConverter
    fun toMemberRole(roleName: String): MemberRole {
        return try {
            MemberRole.valueOf(roleName)
        } catch (e: IllegalArgumentException) {
            MemberRole.MEMBER // Valeur par défaut en cas d'erreur
        }
    }
}