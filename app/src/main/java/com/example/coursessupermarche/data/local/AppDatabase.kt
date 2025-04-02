package com.example.coursessupermarche.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.coursessupermarche.data.local.converters.DateTypeConverter
import com.example.coursessupermarche.data.local.converters.InvitationStatusConverter
import com.example.coursessupermarche.data.local.converters.MemberRoleConverter
import com.example.coursessupermarche.data.local.dao.ListInvitationDao
import com.example.coursessupermarche.data.local.dao.ListMemberDao
import com.example.coursessupermarche.data.local.dao.ShoppingItemDao
import com.example.coursessupermarche.data.local.dao.ShoppingListDao
import com.example.coursessupermarche.data.local.dao.UserDao
import com.example.coursessupermarche.data.local.entities.ShoppingItemEntity
import com.example.coursessupermarche.data.local.entities.UserEntity

@Database(
    entities = [
        ShoppingItemEntity::class,
        ShoppingListEntity::class,
        UserEntity::class,
        ProductSuggestionEntity::class,
        ListMemberEntity::class,
        ListInvitationEntity::class
    ],
    version = 2, // Augmenter la version de la base de données
    exportSchema = false
)
@TypeConverters(
    DateTypeConverter::class,
    MemberRoleConverter::class,
    InvitationStatusConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun userDao(): UserDao
    abstract fun listMemberDao(): ListMemberDao
    abstract fun listInvitationDao(): ListInvitationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_database"
                )
                    .fallbackToDestructiveMigration() // Permet de recréer la BD si nécessaire
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}