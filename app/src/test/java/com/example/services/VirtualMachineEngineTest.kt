package com.example.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.WorkspaceConfig
import com.example.data.DeviceSpoofData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VirtualMachineEngineTest {

    private var context: Context? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSpoofingDataIntegrity() {
        val spoof = DeviceSpoofData()
        
        assertNotNull(spoof.brand)
        assertNotNull(spoof.imei)
        assertEquals(15, spoof.imei.length)
        assertTrue(spoof.macAddress.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")))
    }

    @Test
    fun testVmInitialStateIsStopped() {
        assertEquals(VirtualMachineEngine.VmState.STOPPED, VirtualMachineEngine.getCurrentState())
    }

    @Test
    fun testNetworkIsolationConfigParsing() = runBlocking {
        val config = WorkspaceConfig(
            id = "wks_test",
            name = "Test Workspace",
            status = "Ativo",
            fakeName = "John Doe",
            fakeEmail = "john@example.com",
            proxyIp = "192.168.1.1",
            proxyRegion = "US"
        )
        
        // Em um ambiente sem Root (como Robolectric), o setupIsolation deve retornar false rapidamente
        // mas não deve crashar
        val result = NetworkIsolationEngine.setupIsolation(context!!, config)
        assertFalse("Without root, it should fail gracefully", result)
    }
}
