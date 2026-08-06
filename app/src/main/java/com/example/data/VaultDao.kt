package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM clones ORDER BY dateCreated DESC")
    fun getAllClones(): Flow<List<CloneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClone(clone: CloneEntity)

    @androidx.room.Delete
    suspend fun deleteClone(clone: CloneEntity)

    @Query("UPDATE clones SET isRunning = :isRunning WHERE id = :id")
    suspend fun updateCloneState(id: String, isRunning: Boolean)

    @Query("SELECT * FROM identities ORDER BY dateCreated DESC")
    fun getAllIdentities(): Flow<List<IdentityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: IdentityEntity)

    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC")
    fun getAllSessionLogs(): Flow<List<SessionLogEntity>>

    @Insert
    suspend fun insertSessionLog(log: SessionLogEntity)

    @Query("DELETE FROM session_logs")
    suspend fun clearSessionLogs()

    @Query("DELETE FROM clones")
    suspend fun clearAllClones()

    @Query("DELETE FROM identities")
    suspend fun clearAllIdentities()

    @Query("DELETE FROM profile_configs")
    suspend fun clearAllProfileConfigs()

    @Query("SELECT * FROM profile_configs WHERE profileId = :profileId")
    suspend fun getProfileConfig(profileId: String): ProfileConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileConfig(config: ProfileConfigEntity)
    @Query("SELECT * FROM profile_configs")
    suspend fun getAllProfileConfigs(): List<ProfileConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstanceConfig(config: InstanceConfigEntity)

    @Query("SELECT * FROM instance_configs WHERE workspaceId = :workspaceId")
    suspend fun getInstanceConfig(workspaceId: String): InstanceConfigEntity?

    @Query("SELECT * FROM instance_configs")
    fun getAllInstanceConfigs(): kotlinx.coroutines.flow.Flow<List<InstanceConfigEntity>>

    @androidx.room.Transaction
    @Query("SELECT * FROM instance_configs WHERE workspaceId = :workspaceId")
    suspend fun getWorkspaceWithIdentity(workspaceId: String): WorkspaceWithIdentity?

    @androidx.room.Transaction
    @Query("SELECT * FROM instance_configs")
    fun getAllWorkspacesWithIdentities(): Flow<List<WorkspaceWithIdentity>>

    @androidx.room.Transaction
    suspend fun createWorkspaceAndIdentityTransaction(
        identity: IdentityEntity,
        instanceConfig: InstanceConfigEntity
    ): InstanceConfigEntity {
        insertIdentity(identity)
        val configWithRelation = instanceConfig.copy(
            identityId = identity.id,
            fakeName = identity.fakeName,
            fakeEmail = identity.email,
            fakeCompany = "${identity.jobTitle} • ${identity.location}"
        )
        insertInstanceConfig(configWithRelation)
        return configWithRelation
    }

    @Query("DELETE FROM instance_configs WHERE workspaceId = :workspaceId")
    suspend fun deleteInstanceConfig(workspaceId: String)

    @Query("SELECT * FROM clipboard_vault ORDER BY timestamp DESC")
    fun getAllClipboardItems(): Flow<List<ClipboardEntity>>

    @Query("SELECT * FROM clipboard_vault WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    fun getClipboardItemsForWorkspace(workspaceId: String): Flow<List<ClipboardEntity>>

    @Query("SELECT * FROM clipboard_vault WHERE workspaceId IS NULL OR workspaceId = '' ORDER BY timestamp DESC")
    fun getGlobalClipboardItems(): Flow<List<ClipboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboardItem(item: ClipboardEntity)

    @androidx.room.Delete
    suspend fun deleteClipboardItem(item: ClipboardEntity)

    @Query("DELETE FROM clipboard_vault")
    suspend fun clearClipboardVault()

    @Query("DELETE FROM clipboard_vault WHERE workspaceId = :workspaceId")
    suspend fun clearClipboardVaultForWorkspace(workspaceId: String)

    @Query("SELECT * FROM private_notes ORDER BY lastModified DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM private_notes WHERE workspaceId = :workspaceId ORDER BY lastModified DESC")
    fun getNotesForWorkspace(workspaceId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @androidx.room.Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM private_notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM private_notes")
    suspend fun clearAllNotes()
}
