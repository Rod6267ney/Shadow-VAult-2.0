package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vault_settings")

class SettingsManager(private val context: Context) {
    companion object {
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val REAL_PIN = stringPreferencesKey("real_pin")
        val DECOY_PIN = stringPreferencesKey("decoy_pin")
        val PANIC_PIN = stringPreferencesKey("panic_pin")
        val IS_STEALTH_MODE = booleanPreferencesKey("is_stealth_mode")
        
        val BLOCK_CAMERA = booleanPreferencesKey("block_camera")
        val BLOCK_MIC = booleanPreferencesKey("block_mic")
        val BLOCK_GPS = booleanPreferencesKey("block_gps")

        val BYPASS_PHANTOM_PROCS = booleanPreferencesKey("bypass_phantom_procs")
        val BYPASS_BATTERY_SAVER = booleanPreferencesKey("bypass_battery_saver")
        val BYPASS_BG_LAUNCHES = booleanPreferencesKey("bypass_bg_launches")
        val BYPASS_LIMITS = booleanPreferencesKey("bypass_limits")

        // Network Configs
        val GLOBAL_VPN_ENABLED = booleanPreferencesKey("global_vpn_enabled")
        val GLOBAL_PROXY_REGION = stringPreferencesKey("global_proxy_region")
        val KILL_SWITCH_ENABLED = booleanPreferencesKey("kill_switch_enabled")
        val DNS_LEAK_PROTECTION = booleanPreferencesKey("dns_leak_protection")
        val RANDOMIZE_MAC = booleanPreferencesKey("randomize_mac")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[IS_ONBOARDED] ?: false }
    val realPin: Flow<String?> = context.dataStore.data.map { it[REAL_PIN] }
    val decoyPin: Flow<String?> = context.dataStore.data.map { it[DECOY_PIN] }
    val panicPin: Flow<String?> = context.dataStore.data.map { it[PANIC_PIN] }
    val isStealthMode: Flow<Boolean> = context.dataStore.data.map { it[IS_STEALTH_MODE] ?: false }

    val isCameraBlocked = context.dataStore.data.map { it[BLOCK_CAMERA] ?: false }
    val isMicBlocked = context.dataStore.data.map { it[BLOCK_MIC] ?: false }
    val isGpsBlocked = context.dataStore.data.map { it[BLOCK_GPS] ?: false }

    val isBypassPhantomProcs = context.dataStore.data.map { it[BYPASS_PHANTOM_PROCS] ?: false }
    val isBypassBatterySaver = context.dataStore.data.map { it[BYPASS_BATTERY_SAVER] ?: false }
    val isBypassBgLaunches = context.dataStore.data.map { it[BYPASS_BG_LAUNCHES] ?: false }
    val isBypassLimits = context.dataStore.data.map { it[BYPASS_LIMITS] ?: false }

    val globalVpnEnabled = context.dataStore.data.map { it[GLOBAL_VPN_ENABLED] ?: false }
    val globalProxyRegion = context.dataStore.data.map { it[GLOBAL_PROXY_REGION] ?: "US - Nova York" }
    val killSwitchEnabled = context.dataStore.data.map { it[KILL_SWITCH_ENABLED] ?: false }
    val dnsLeakProtection = context.dataStore.data.map { it[DNS_LEAK_PROTECTION] ?: true }
    val randomizeMac = context.dataStore.data.map { it[RANDOMIZE_MAC] ?: true }

    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[IS_ONBOARDED] = value }
    suspend fun setRealPin(pin: String) = context.dataStore.edit { it[REAL_PIN] = pin }
    suspend fun setDecoyPin(pin: String) = context.dataStore.edit { it[DECOY_PIN] = pin }
    suspend fun setPanicPin(pin: String) = context.dataStore.edit { it[PANIC_PIN] = pin }
    suspend fun setStealthMode(isStealth: Boolean) = context.dataStore.edit { it[IS_STEALTH_MODE] = isStealth }

    suspend fun setCameraBlocked(blocked: Boolean) = context.dataStore.edit { it[BLOCK_CAMERA] = blocked }
    suspend fun setMicBlocked(blocked: Boolean) = context.dataStore.edit { it[BLOCK_MIC] = blocked }
    suspend fun setGpsBlocked(blocked: Boolean) = context.dataStore.edit { it[BLOCK_GPS] = blocked }

    suspend fun setBypassPhantomProcs(enabled: Boolean) = context.dataStore.edit { it[BYPASS_PHANTOM_PROCS] = enabled }
    suspend fun setBypassBatterySaver(enabled: Boolean) = context.dataStore.edit { it[BYPASS_BATTERY_SAVER] = enabled }
    suspend fun setBypassBgLaunches(enabled: Boolean) = context.dataStore.edit { it[BYPASS_BG_LAUNCHES] = enabled }
    suspend fun setBypassLimits(enabled: Boolean) = context.dataStore.edit { it[BYPASS_LIMITS] = enabled }

    suspend fun setGlobalVpnEnabled(enabled: Boolean) = context.dataStore.edit { it[GLOBAL_VPN_ENABLED] = enabled }
    suspend fun setGlobalProxyRegion(region: String) = context.dataStore.edit { it[GLOBAL_PROXY_REGION] = region }
    suspend fun setKillSwitchEnabled(enabled: Boolean) = context.dataStore.edit { it[KILL_SWITCH_ENABLED] = enabled }
    suspend fun setDnsLeakProtection(enabled: Boolean) = context.dataStore.edit { it[DNS_LEAK_PROTECTION] = enabled }
    suspend fun setRandomizeMac(enabled: Boolean) = context.dataStore.edit { it[RANDOMIZE_MAC] = enabled }
}
