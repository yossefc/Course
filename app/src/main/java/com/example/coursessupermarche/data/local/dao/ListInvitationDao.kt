package com.example.coursessupermarche.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.coursessupermarche.models.InvitationStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

// DAO pour les invitations à rejoindre une liste
@Dao
interface ListInvitationDao {
    @Query("SELECT * FROM list_invitations WHERE status = :status ORDER BY createdAt DESC")
    fun getInvitationsByStatus(status: InvitationStatus): Flow<List<ListInvitationEntity>>

    @Query("SELECT * FROM list_invitations WHERE listId = :listId")
    fun getInvitationsByListId(listId: String): Flow<List<ListInvitationEntity>>

    @Query("SELECT * FROM list_invitations WHERE token = :token LIMIT 1")
    suspend fun getInvitationByToken(token: String): ListInvitationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invitation: ListInvitationEntity)

    @Update
    suspend fun update(invitation: ListInvitationEntity)

    @Query("UPDATE list_invitations SET status = :status WHERE id = :invitationId")
    suspend fun updateStatus(invitationId: String, status: InvitationStatus)

    @Query("UPDATE list_invitations SET status = :status WHERE expiresAt < :currentDate AND status = :pendingStatus")
    suspend fun expireOldInvitations(currentDate: Date, status: InvitationStatus = InvitationStatus.EXPIRED, pendingStatus: InvitationStatus = InvitationStatus.PENDING)

    @Query("DELETE FROM list_invitations WHERE id = :invitationId")
    suspend fun delete(invitationId: String)

    @Query("SELECT * FROM list_invitations WHERE isRemotelySync = 0")
    suspend fun getUnsyncedInvitations(): List<ListInvitationEntity>

    @Query("UPDATE list_invitations SET isRemotelySync = 1 WHERE id = :id")
    suspend fun markInvitationAsSynced(id: String)
}