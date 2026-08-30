package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DatabaseUnitTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: VaultDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Initialize Room in-memory for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.vaultDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndReadInstanceConfig() = runBlocking {
        val testConfig = InstanceConfigEntity(
            workspaceId = "test_ws_123",
            workspaceName = "Test Workspace",
            fakeName = "John Doe",
            fakeEmail = "john@example.com",
            fakePhone = "123456789",
            fakeCompany = "Acme Corp",
            vpnEnabled = true,
            proxyRegion = "BR",
            iconName = "default",
            unlimitedClones = false,
            workspaceType = "VIRTUAL_MACHINE"
        )
        
        dao.insertInstanceConfig(testConfig)
        
        val allConfigs = dao.getAllInstanceConfigs().first()
        assertEquals(1, allConfigs.size)
        assertEquals("test_ws_123", allConfigs[0].workspaceId)
        assertEquals("VIRTUAL_MACHINE", allConfigs[0].workspaceType)
    }

    @Test
    fun insertAndReadClone() = runBlocking {
        val clone = CloneEntity(
            appName = "TestApp",
            packageName = "com.test.app",
            userId = "v_123",
            cloneMode = "VIRTUAL_MACHINE"
        )
        
        dao.insertClone(clone)
        
        val allClones = dao.getAllClones().first()
        assertTrue(allClones.isNotEmpty())
        assertEquals("TestApp", allClones[0].appName)
        assertEquals("v_123", allClones[0].userId)
    }
}
