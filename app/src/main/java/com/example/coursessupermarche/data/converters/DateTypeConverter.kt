package com.example.coursessupermarche.data.local.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Convertisseur pour gérer les dates dans Room
 */
class DateTypeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}