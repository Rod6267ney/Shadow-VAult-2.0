package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CloneEntity::class, IdentityEntity::class, SessionLogEntity::class, ProfileConfigEntity::class, InstanceConfigEntity::class, ClipboardEntity::class, NoteEntity::class], version = 9, exportSchema = false)
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

                val db = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    dbName
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_8_9)
                .fallbackToDestructiveMigration(false)
                .build()

                INSTANCE = db
                db
            }
        }
    }
}

