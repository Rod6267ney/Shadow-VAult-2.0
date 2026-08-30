package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CloneEntity::class, IdentityEntity::class, SessionLogEntity::class, ProfileConfigEntity::class, InstanceConfigEntity::class, ClipboardEntity::class, NoteEntity::class], version = 14, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE != null) return INSTANCE!!
                
                val appContext = context.applicationContext
                val dbName = "shadow_vault_database"

                val passphrase = CryptoManager.getPassphrase(appContext)
                val factory = SupportFactory(passphrase)

                val MIGRATION_8_9 = object : Migration(8, 9) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE clones ADD COLUMN colorHex TEXT")
                    }
                }

                val MIGRATION_9_10 = object : Migration(9, 10) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE clones ADD COLUMN linkedIdentityId TEXT")
                    }
                }

                val MIGRATION_10_11 = object : Migration(10, 11) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE clones ADD COLUMN cloneMode TEXT NOT NULL DEFAULT 'WORK_PROFILE'")
                        db.execSQL("ALTER TABLE clones ADD COLUMN firewallEnabled INTEGER NOT NULL DEFAULT 0")
                        db.execSQL("ALTER TABLE clones ADD COLUMN spoofProfile TEXT")
                    }
                }

                val MIGRATION_11_12 = object : Migration(11, 12) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE instance_configs ADD COLUMN workspaceType TEXT NOT NULL DEFAULT 'NATIVE_WORK_PROFILE'")
                    }
                }

                val MIGRATION_12_13 = object : Migration(12, 13) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `virtual_machine_states` (`id` TEXT NOT NULL, `workspaceId` TEXT NOT NULL, `cpuCores` INTEGER NOT NULL, `memoryMb` INTEGER NOT NULL, `rootfsPath` TEXT NOT NULL, `state` TEXT NOT NULL, `diskUsageBytes` INTEGER NOT NULL, `lastBootTime` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    }
                }
                
                val MIGRATION_13_14 = object : Migration(13, 14) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("DROP TABLE IF EXISTS `virtual_machine_states`")
                    }
                }

                val db = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    dbName
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .fallbackToDestructiveMigration(false)
                .build()

                INSTANCE = db
                db
            }
        }
    }
}

