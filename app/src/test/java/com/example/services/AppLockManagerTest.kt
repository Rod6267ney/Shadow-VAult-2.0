package com.example.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockLifecycleOwner = mockk<LifecycleOwner>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AppLockManager.unlockApp()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLockApp_changesStateToTrue() = runTest(testDispatcher) {
        assertFalse(AppLockManager.isLocked.value)
        AppLockManager.lockApp()
        assertTrue(AppLockManager.isLocked.value)
    }

    @Test
    fun testUnlockApp_changesStateToFalse() = runTest(testDispatcher) {
        AppLockManager.lockApp()
        assertTrue(AppLockManager.isLocked.value)
        AppLockManager.unlockApp()
        assertFalse(AppLockManager.isLocked.value)
    }

    @Test
    fun testAutoLock_onStop_locksAppAfterTimeout() = runTest(testDispatcher) {
        // App goes to background
        AppLockManager.onStop(mockLifecycleOwner)
        
        assertFalse(AppLockManager.isLocked.value)
        
        // Fast-forward time by 60 seconds (60_000 ms)
        testScheduler.advanceTimeBy(60_001L)
        
        assertTrue(AppLockManager.isLocked.value)
    }

    @Test
    fun testAutoLock_onStart_cancelsTimer() = runTest(testDispatcher) {
        // App goes to background
        AppLockManager.onStop(mockLifecycleOwner)
        
        // Fast-forward time by 30 seconds
        testScheduler.advanceTimeBy(30_000L)
        assertFalse(AppLockManager.isLocked.value)
        
        // App comes back to foreground before timeout
        AppLockManager.onStart(mockLifecycleOwner)
        
        // Fast-forward remaining 30 seconds
        testScheduler.advanceTimeBy(30_001L)
        
        // Should NOT be locked
        assertFalse(AppLockManager.isLocked.value)
    }
}
