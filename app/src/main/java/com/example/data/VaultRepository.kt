package com.example.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(private val dao: VaultDao) {
    // Clones
    fun getAllClones(): Flow<List<CloneEntity>> = dao.getAllClones()
    suspend fun insertClone(clone: CloneEntity) = dao.insertClone(clone)
    suspend fun deleteClone(clone: CloneEntity) = dao.deleteClone(clone)
    suspend fun updateCloneState(id: String, isRunning: Boolean) = dao.updateCloneState(id, isRunning)

    // Identities
    fun getAllIdentities(): Flow<List<IdentityEntity>> = dao.getAllIdentities()
    suspend fun insertIdentity(identity: IdentityEntity) = dao.insertIdentity(identity)
    suspend fun deleteIdentity(identity: IdentityEntity) = dao.deleteIdentity(identity)

    // Workspaces / Instances
    fun getAllWorkspacesWithIdentities(): Flow<List<WorkspaceWithIdentity>> = dao.getAllWorkspacesWithIdentities()
    
    suspend fun createWorkspaceAndIdentityTransaction(
        identity: IdentityEntity,
        instanceConfig: InstanceConfigEntity
    ): InstanceConfigEntity {
        return dao.createWorkspaceAndIdentityTransaction(identity, instanceConfig)
    }

    suspend fun deleteWorkspace(workspaceId: String) {
        dao.deleteInstanceConfig(workspaceId)
    }

}

