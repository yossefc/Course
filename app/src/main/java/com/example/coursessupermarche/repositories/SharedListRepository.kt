package com.example.coursessupermarche.repositories

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.coursessupermarche.data.local.dao.ListInvitationDao
import com.example.coursessupermarche.data.local.dao.ListMemberDao
import com.example.coursessupermarche.data.local.dao.ShoppingItemDao
import com.example.coursessupermarche.data.local.dao.ShoppingListDao
import com.example.coursessupermarche.data.local.entities.ListInvitationEntity
import com.example.coursessupermarche.data.local.entities.ListMemberEntity
import com.example.coursessupermarche.data.local.entities.ShoppingListEntity
import com.example.coursessupermarche.models.InvitationStatus
import com.example.coursessupermarche.models.ListInvitation
import com.example.coursessupermarche.models.ListMember
import com.example.coursessupermarche.models.MemberRole
import com.example.coursessupermarche.models.ShoppingItem
import com.example.coursessupermarche.models.ShoppingList
import com.example.coursessupermarche.models.User
import com.example.coursessupermarche.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SharedListRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao,
    private val listMemberDao: ListMemberDao,
    private val listInvitationDao: ListInvitationDao,
    private val context: Context
) {
    private val TAG = "SharedListRepository"

    // Collections Firestore
    private val listsCollection = firestore.collection("lists")
    private val invitationsCollection = firestore.collection("invitations")
    private val baseAppUrl = "https://coursessupermarche.example.com/invite/"

    // Obtenir l'utilisateur courant
    private fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: firebaseUser.email ?: "Utilisateur"
        )
    }

    // Obtenir toutes les listes accessibles par l'utilisateur courant (créées ou partagées)
    fun getAccessibleLists(): Flow<List<ShoppingList>> {
        val userId = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return shoppingListDao.getAccessibleListsByUserId(userId)
            .map { listEntities ->
                listEntities.map { it.toModel() }
            }
    }

    // Récupérer une liste par son ID
    suspend fun getListById(listId: String): ShoppingList? {
        val listEntity = shoppingListDao.getListById(listId) ?: return null

        // Récupérer les items de la liste
        val items = try {
            // Collecter le premier résultat du flow ou utiliser une liste vide s'il n'y a pas de résultat
            val itemEntities = shoppingItemDao.getItemsByListId(listId).first()
            itemEntities.map { it.toModel() }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des items: ${e.message}", e)
            emptyList()
        }

        return listEntity.toModel(items = items)
    }

    // Récupérer les membres d'une liste
    fun getMembersForList(listId: String): Flow<List<ListMember>> {
        return listMemberDao.getMembersByListId(listId)
            .map { entities -> entities.map { it.toModel() } }
    }

    // Créer une nouvelle liste de courses
    suspend fun createShoppingList(name: String): ShoppingList {
        val currentUser = getCurrentUser() ?: throw IllegalStateException("Utilisateur non connecté")

        // Créer un ID unique pour la liste
        val listId = UUID.randomUUID().toString()

        // Créer le modèle de liste
        val newList = ShoppingList(
            id = listId,
            name = name,
            createdAt = Date(),
            updatedAt = Date(),
            ownerId = currentUser.id,
            isShared = false,
            items = emptyList(),
            members = listOf(
                ListMember(
                    userId = currentUser.id,
                    email = currentUser.email,
                    displayName = currentUser.displayName,
                    joinedAt = Date(),
                    role = MemberRole.OWNER
                )
            )
        )

        // Sauvegarder localement
        val listEntity = ShoppingListEntity.fromModel(
            model = newList,
            userId = currentUser.id,
            isRemotelySync = NetworkMonitor.isOnline
        )
        shoppingListDao.insert(listEntity)

        // Ajouter le propriétaire comme membre
        val ownerMember = ListMemberEntity.fromModel(
            listId = listId,
            model = ListMember(
                userId = currentUser.id,
                email = currentUser.email,
                displayName = currentUser.displayName,
                joinedAt = Date(),
                role = MemberRole.OWNER
            ),
            isRemotelySync = NetworkMonitor.isOnline
        )
        listMemberDao.insert(ownerMember)

        // Sauvegarder dans Firestore si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                // Créer document de liste
                val listData = hashMapOf(
                    "name" to name,
                    "ownerId" to currentUser.id,
                    "isShared" to false,
                    "createdAt" to newList.createdAt,
                    "updatedAt" to newList.updatedAt
                )

                listsCollection.document(listId).set(listData).await()

                // Ajouter le propriétaire comme membre
                val memberData = hashMapOf(
                    "userId" to currentUser.id,
                    "email" to currentUser.email,
                    "displayName" to currentUser.displayName,
                    "joinedAt" to Date(),
                    "role" to MemberRole.OWNER.name
                )

                listsCollection.document(listId)
                    .collection("members")
                    .document(currentUser.id)
                    .set(memberData)
                    .await()

                // Marquer comme synchronisées
                shoppingListDao.markListAsSynced(listId)
                listMemberDao.markMemberAsSynced("${currentUser.id}_${listId}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de la liste dans Firestore", e)
            }
        }

        return newList
    }

    // Partager une liste via invitation
    suspend fun createInvitation(listId: String): String {
        val currentUser = getCurrentUser() ?: throw IllegalStateException("Utilisateur non connecté")

        // Vérifier que l'utilisateur a accès à cette liste
        val list = shoppingListDao.getAccessibleListById(listId, currentUser.id)
            ?: throw IllegalArgumentException("Liste non trouvée ou accès refusé")

        // Générer un token unique pour l'invitation
        val token = generateInvitationToken(listId, currentUser.id)

        // Calculer la date d'expiration (7 jours)
        val expiresAt = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
        }.time

        // Créer l'invitation
        val invitation = ListInvitation(
            listId = listId,
            listName = list.name,
            inviterId = currentUser.id,
            inviterName = currentUser.displayName,
            createdAt = Date(),
            expiresAt = expiresAt,
            token = token,
            status = InvitationStatus.PENDING
        )

        // Sauvegarder localement
        val invitationEntity = ListInvitationEntity.fromModel(
            model = invitation,
            isRemotelySync = NetworkMonitor.isOnline
        )
        listInvitationDao.insert(invitationEntity)

        // Mettre à jour le statut partagé de la liste
        shoppingListDao.updateSharedStatus(listId, true)

        // Sauvegarder dans Firestore si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                val invitationData = hashMapOf(
                    "listId" to listId,
                    "listName" to list.name,
                    "inviterId" to currentUser.id,
                    "inviterName" to currentUser.displayName,
                    "createdAt" to invitation.createdAt,
                    "expiresAt" to invitation.expiresAt,
                    "token" to token,
                    "status" to InvitationStatus.PENDING.name
                )

                invitationsCollection.document(invitation.id)
                    .set(invitationData)
                    .await()

                // Mettre à jour le statut de la liste
                listsCollection.document(listId)
                    .update("isShared", true)
                    .await()

                // Marquer comme synchronisées
                listInvitationDao.markInvitationAsSynced(invitation.id)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de l'invitation dans Firestore", e)
            }
        }

        // Générer l'URL d'invitation
        return "$baseAppUrl$token"
    }

    // Partager l'invitation via WhatsApp
    fun shareInvitationViaWhatsApp(invitationUrl: String, listName: String) {
        val message = "Je vous invite à rejoindre ma liste de courses \"$listName\". " +
                "Cliquez sur ce lien pour l'ouvrir dans l'application : $invitationUrl"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra("Kdescription", message)

            // Essayer d'abord de cibler WhatsApp spécifiquement
            setPackage("com.whatsapp")
        }

        try {
            // Démarrer l'activité en dehors du contexte d'activité
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Si WhatsApp n'est pas installé, utiliser n'importe quelle application de partage
            val chooserIntent = Intent.createChooser(intent.apply {
                setPackage(null) // Supprimer la restriction à WhatsApp
            }, "Partager via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        }
    }

    // Accepter une invitation
    suspend fun acceptInvitation(token: String): Boolean {
        val currentUser = getCurrentUser() ?: throw IllegalStateException("Utilisateur non connecté")

        // Récupérer l'invitation
        val invitation = listInvitationDao.getInvitationByToken(token)
            ?: throw IllegalArgumentException("Invitation non trouvée")

        // Vérifier si l'invitation n'est pas expirée
        if (invitation.status != InvitationStatus.PENDING || invitation.expiresAt.before(Date())) {
            // Mettre à jour le statut localement
            listInvitationDao.updateStatus(invitation.id, InvitationStatus.EXPIRED)

            // Mettre à jour sur Firestore si en ligne
            if (NetworkMonitor.isOnline) {
                try {
                    invitationsCollection.document(invitation.id)
                        .update("status", InvitationStatus.EXPIRED.name)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la mise à jour de l'invitation expirée", e)
                }
            }

            return false
        }

        // Récupérer les informations de la liste
        val listId = invitation.listId
        val listEntity = shoppingListDao.getListById(listId)

        if (listEntity == null) {
            // La liste n'existe pas localement, essayer de la récupérer de Firestore
            if (NetworkMonitor.isOnline) {
                try {
                    val listDoc = listsCollection.document(listId).get().await()

                    if (listDoc.exists()) {
                        // Créer la liste localement
                        val newListEntity = ShoppingListEntity(
                            id = listId,
                            userId = listDoc.getString("ownerId") ?: "",
                            name = listDoc.getString("name") ?: "Liste partagée",
                            createdAt = listDoc.getDate("createdAt") ?: Date(),
                            updatedAt = listDoc.getDate("updatedAt") ?: Date(),
                            isRemotelySync = true,
                            isShared = true,
                            ownerId = listDoc.getString("ownerId") ?: ""
                        )
                        shoppingListDao.insert(newListEntity)
                    } else {
                        // Liste supprimée sur Firestore
                        listInvitationDao.updateStatus(invitation.id, InvitationStatus.EXPIRED)
                        return false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la récupération de la liste", e)
                    return false
                }
            } else {
                // Pas de connexion, impossible de vérifier
                return false
            }
        }

        // Ajouter l'utilisateur comme membre de la liste
        val newMember = ListMemberEntity(
            id = "${currentUser.id}_${listId}",
            listId = listId,
            userId = currentUser.id,
            email = currentUser.email,
            displayName = currentUser.displayName,
            joinedAt = Date(),
            role = MemberRole.MEMBER,
            isRemotelySync = NetworkMonitor.isOnline
        )

        listMemberDao.insert(newMember)

        // Mettre à jour le statut de l'invitation
        listInvitationDao.updateStatus(invitation.id, InvitationStatus.ACCEPTED)

        // Synchroniser avec Firestore si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                // Ajouter l'utilisateur comme membre
                val memberData = hashMapOf(
                    "userId" to currentUser.id,
                    "email" to currentUser.email,
                    "displayName" to currentUser.displayName,
                    "joinedAt" to Date(),
                    "role" to MemberRole.MEMBER.name
                )

                listsCollection.document(listId)
                    .collection("members")
                    .document(currentUser.id)
                    .set(memberData)
                    .await()

                // Mettre à jour le statut de l'invitation
                invitationsCollection.document(invitation.id)
                    .update("status", InvitationStatus.ACCEPTED.name)
                    .await()

                // Marquer comme synchronisées
                listMemberDao.markMemberAsSynced(newMember.id)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation de l'acceptation", e)
                // Continuer quand même car l'utilisateur est ajouté localement
            }
        }

        return true
    }

    // Quitter une liste partagée
    suspend fun leaveSharedList(listId: String): Boolean {
        val currentUser = getCurrentUser() ?: throw IllegalStateException("Utilisateur non connecté")

        // Vérifier si l'utilisateur est membre de cette liste
        val isMember = listMemberDao.isMemberOfList(listId, currentUser.id)
        if (isMember <= 0) {
            return false
        }

        // Vérifier si l'utilisateur est le propriétaire
        val listEntity = shoppingListDao.getListById(listId)
        if (listEntity?.ownerId == currentUser.id) {
            // Le propriétaire ne peut pas quitter la liste, il doit la supprimer
            return false
        }

        // Supprimer l'utilisateur comme membre
        listMemberDao.removeFromList(listId, currentUser.id)

        // Synchroniser avec Firestore si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                listsCollection.document(listId)
                    .collection("members")
                    .document(currentUser.id)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression du membre dans Firestore", e)
                // Continuer quand même car l'utilisateur est supprimé localement
            }
        }

        return true
    }

    // Supprimer une liste (seulement si l'utilisateur est le propriétaire)
    suspend fun deleteList(listId: String): Boolean {
        val currentUser = getCurrentUser() ?: throw IllegalStateException("Utilisateur non connecté")

        // Récupérer la liste
        val listEntity = shoppingListDao.getListById(listId)
            ?: return false

        // Vérifier si l'utilisateur est le propriétaire
        if (listEntity.ownerId != currentUser.id) {
            return false
        }

        // Supprimer la liste localement
        shoppingListDao.deleteById(listId)

        // Supprimer sur Firestore si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                // Supprimer toutes les invitations liées à cette liste
                invitationsCollection.whereEqualTo("listId", listId)
                    .get()
                    .await()
                    .documents
                    .forEach { doc ->
                        invitationsCollection.document(doc.id).delete().await()
                    }

                // Supprimer la liste elle-même
                listsCollection.document(listId).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la suppression de la liste sur Firestore", e)
                // Continuer quand même car la liste est supprimée localement
            }
        }

        return true
    }

    // Générer un token sécurisé pour les invitations
    private fun generateInvitationToken(listId: String, userId: String): String {
        val seed = "$listId-$userId-${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.substring(0, 32)
    }

    // Traiter une invitation depuis une URL
    suspend fun processInvitationFromUrl(url: String): Boolean {
        try {
            val uri = Uri.parse(url)
            val token = uri.lastPathSegment ?: return false

            return acceptInvitation(token)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du traitement de l'URL d'invitation", e)
            return false
        }
    }

    // Nettoyer les invitations expirées
    suspend fun cleanupExpiredInvitations() {
        val currentDate = Date()

        // Mettre à jour localement
        listInvitationDao.expireOldInvitations(currentDate)

        // Mettre à jour sur Firestore si en ligne
        if (NetworkMonitor.isOnline) {
            try {
                // Obtenir les invitations expirées
                val expiredInvitations = invitationsCollection
                    .whereEqualTo("status", InvitationStatus.PENDING.name)
                    .whereLessThan("expiresAt", currentDate)
                    .get()
                    .await()

                // Mettre à jour chaque invitation
                for (doc in expiredInvitations.documents) {
                    invitationsCollection.document(doc.id)
                        .update("status", InvitationStatus.EXPIRED.name)
                        .await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du nettoyage des invitations expirées sur Firestore", e)
            }
        }
    }
}