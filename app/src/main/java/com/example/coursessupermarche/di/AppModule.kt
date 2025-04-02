package com.example.coursessupermarche.di

import android.content.Context
import com.example.coursessupermarche.data.local.AppDatabase
import com.example.coursessupermarche.data.local.dao.ListInvitationDao
import com.example.coursessupermarche.data.local.dao.ListMemberDao
import com.example.coursessupermarche.data.local.dao.ShoppingItemDao
import com.example.coursessupermarche.data.local.dao.ShoppingListDao
import com.example.coursessupermarche.data.local.dao.UserDao
import com.example.coursessupermarche.repositories.SharedListRepository
import com.example.coursessupermarche.repositories.ShoppingListRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideShoppingItemDao(database: AppDatabase): ShoppingItemDao {
        return database.shoppingItemDao()
    }

    @Provides
    fun provideShoppingListDao(database: AppDatabase): ShoppingListDao {
        return database.shoppingListDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    // Nouvelles méthodes pour fournir les DAOs manquants
    @Provides
    fun provideListMemberDao(database: AppDatabase): ListMemberDao {
        return database.listMemberDao()
    }

    @Provides
    fun provideListInvitationDao(database: AppDatabase): ListInvitationDao {
        return database.listInvitationDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideShoppingListRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        shoppingListDao: ShoppingListDao,
        shoppingItemDao: ShoppingItemDao,
        userDao: UserDao
    ): ShoppingListRepository {
        return ShoppingListRepository(firestore, auth, shoppingListDao, shoppingItemDao, userDao)
    }

    @Provides
    @Singleton
    fun provideSharedListRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        shoppingListDao: ShoppingListDao,
        shoppingItemDao: ShoppingItemDao,
        listMemberDao: ListMemberDao,
        listInvitationDao: ListInvitationDao,
        @ApplicationContext context: Context
    ): SharedListRepository {
        return SharedListRepository(
            firestore,
            auth,
            shoppingListDao,
            shoppingItemDao,
            listMemberDao,
            listInvitationDao,
            context
        )
    }
}