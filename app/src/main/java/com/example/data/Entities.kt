package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

@Entity(tableName = "clones")
data class CloneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val userId: String = "10",
    val isRunning: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    val colorHex: String? = null
)

@Entity(tableName = "identities")
data class IdentityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fakeName: String,
    val jobTitle: String,
    val location: String,
    val profileIdea: String,
    val dob: String = "",
    val address: String = "",
    val email: String = "",
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventType: String, // e.g. "CONNECTION", "ERROR", "PROFILE_CREATED"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_configs")
data class ProfileConfigEntity(
    @PrimaryKey val profileId: String,
    val profileName: String = "",
    val category: String = "Uncategorized",
    val jobTitle: String = "Unknown",
    val encryptedConfigData: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "instance_configs",
    foreignKeys = [
        ForeignKey(
            entity = IdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["identityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["identityId"])]
)
data class InstanceConfigEntity(
    @PrimaryKey val workspaceId: String,
    val workspaceName: String,
    val fakeName: String,
    val fakeEmail: String,
    val fakePhone: String,
    val fakeCompany: String,
    val vpnEnabled: Boolean,
    val proxyRegion: String,
    val iconName: String,
    val unlimitedClones: Boolean,
    val identityId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class WorkspaceWithIdentity(
    @Embedded val instanceConfig: InstanceConfigEntity,
    @Relation(
        parentColumn = "identityId",
        entityColumn = "id"
    )
    val identity: IdentityEntity?
)

@Entity(tableName = "clipboard_vault")
data class ClipboardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val copiedText: String,
    val sourceApp: String = "Chaos OS",
    val workspaceId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "private_notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "Geral",
    val isLocked: Boolean = false,
    val workspaceId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
