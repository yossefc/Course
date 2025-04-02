package com.example.coursessupermarche.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// DAO pour les membres d'une liste partagée
@Dao
interface ListMemberDao {
    @Query("SELECT * FROM list_members WHERE listId = :listId")
    fun getMembersByListId(listId: String): Flow<List<ListMemberEntity>>

    @Query("SELECT * FROM list_members WHERE userId = :userId")
    fun getListsByMemberId(userId: String): Flow<List<ListMemberEntity>>

    @Query("SELECT COUNT(*) FROM list_members WHERE listId = :listId AND userId = :userId")
    suspend fun isMemberOfList(listId: String, userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: ListMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<ListMemberEntity>)

    @Update
    suspend fun update(member: ListMemberEntity)

    @Query("DELETE FROM list_members WHERE listId = :listId AND userId = :userId")
    suspend fun removeFromList(listId: String, userId: String)

    @Query("SELECT * FROM list_members WHERE isRemotelySync = 0")
    suspend fun getUnsyncedMembers(): List<ListMemberEntity>

    @Query("UPDATE list_members SET isRemotelySync = 1 WHERE id = :id")
    suspend fun markMemberAsSynced(id: String)
}