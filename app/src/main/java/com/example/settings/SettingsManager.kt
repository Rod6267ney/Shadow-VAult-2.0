package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        
        val TOR_ROUTING_ENABLED = booleanPreferencesKey("tor_routing_enabled")
        val DPI_BYPASS_ENABLED = booleanPreferencesKey("dpi_bypass_enabled")
        val DOH_PROVIDER = stringPreferencesKey("doh_provider")
        val ADBLOCK_ENABLED = booleanPreferencesKey("adblock_enabled")
        
        val SPOOF_IMEI_ENABLED = booleanPreferencesKey("spoof_imei_enabled")
        val SPOOF_SERIAL_ENABLED = booleanPreferencesKey("spoof_serial_enabled")
        val SPOOF_MODEL_ENABLED = booleanPreferencesKey("spoof_model_enabled")
        val BASEBAND_ISOLATION_ENABLED = booleanPreferencesKey("baseband_isolation_enabled")

        // Shield Configs
        val BLOCK_MOTION_SENSORS = booleanPreferencesKey("block_motion_sensors")
        val BLOCK_CLIPBOARD = booleanPreferencesKey("block_clipboard")
        val BLOCK_ENV_SENSORS = booleanPreferencesKey("block_env_sensors")
        val FORCE_SECURE_FLAG = booleanPreferencesKey("force_secure_flag")
        val DISABLE_TELEMETRY = booleanPreferencesKey("disable_telemetry")
        val ANTI_DOZE_MODE = booleanPreferencesKey("anti_doze_mode")
        val FORCE_BG_APPOPS = booleanPreferencesKey("force_bg_appops")
        val BLOCK_LOGCAT = booleanPreferencesKey("block_logcat")
        val SHUFFLE_KEYPAD = booleanPreferencesKey("shuffle_keypad")
        val COERCION_PIN = stringPreferencesKey("coercion_pin")
        val DYNAMIC_STEALTH_MODE = booleanPreferencesKey("dynamic_stealth_mode")
        val CAMOUFLAGE_NOTIFICATIONS = booleanPreferencesKey("camouflage_notifications")

        val MAX_FAILED_ATTEMPTS = intPreferencesKey("max_failed_attempts")
        val LOCKOUT_DURATION = longPreferencesKey("lockout_duration")
        val WIPE_ON_MAX_ATTEMPTS = booleanPreferencesKey("wipe_on_max_attempts")
        val REQUIRE_BIOMETRICS_DESTRUCTIVE = booleanPreferencesKey("require_biometrics_destructive")
        val AUTO_CLEANUP_ENABLED = booleanPreferencesKey("auto_cleanup_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

        val BIO_OPEN_APP = booleanPreferencesKey("bio_open_app")
        val BIO_SWITCH_WORKSPACE = booleanPreferencesKey("bio_switch_workspace")
        val BIO_EXECUTE_CLONE = booleanPreferencesKey("bio_execute_clone")
        val CHAOS_OS_ACTIVE_FEATURES = stringSetPreferencesKey("chaos_os_active_features")
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

    val torRoutingEnabled = context.dataStore.data.map { it[TOR_ROUTING_ENABLED] ?: false }
    val dpiBypassEnabled = context.dataStore.data.map { it[DPI_BYPASS_ENABLED] ?: false }
    val dohProvider = context.dataStore.data.map { it[DOH_PROVIDER] ?: "Cloudflare" }
    val adBlockEnabled = context.dataStore.data.map { it[ADBLOCK_ENABLED] ?: false }

    val spoofImeiEnabled = context.dataStore.data.map { it[SPOOF_IMEI_ENABLED] ?: false }
    val spoofSerialEnabled = context.dataStore.data.map { it[SPOOF_SERIAL_ENABLED] ?: false }
    val spoofModelEnabled = context.dataStore.data.map { it[SPOOF_MODEL_ENABLED] ?: false }
    val basebandIsolationEnabled = context.dataStore.data.map { it[BASEBAND_ISOLATION_ENABLED] ?: false }

    val blockMotionSensors = context.dataStore.data.map { it[BLOCK_MOTION_SENSORS] ?: false }
    val blockClipboard = context.dataStore.data.map { it[BLOCK_CLIPBOARD] ?: false }
    val blockEnvSensors = context.dataStore.data.map { it[BLOCK_ENV_SENSORS] ?: false }
    val forceSecureFlag = context.dataStore.data.map { it[FORCE_SECURE_FLAG] ?: false }
    val disableTelemetry = context.dataStore.data.map { it[DISABLE_TELEMETRY] ?: false }
    val antiDozeMode = context.dataStore.data.map { it[ANTI_DOZE_MODE] ?: false }
    val forceBgAppops = context.dataStore.data.map { it[FORCE_BG_APPOPS] ?: false }
    val blockLogcat = context.dataStore.data.map { it[BLOCK_LOGCAT] ?: false }
    val shuffleKeypad = context.dataStore.data.map { it[SHUFFLE_KEYPAD] ?: false }
    val coercionPin = context.dataStore.data.map { it[COERCION_PIN] }
    val dynamicStealthMode = context.dataStore.data.map { it[DYNAMIC_STEALTH_MODE] ?: false }
    val camouflageNotifications = context.dataStore.data.map { it[CAMOUFLAGE_NOTIFICATIONS] ?: false }

    val maxFailedAttempts = context.dataStore.data.map { it[MAX_FAILED_ATTEMPTS] ?: 3 }
    val lockoutDuration = context.dataStore.data.map { it[LOCKOUT_DURATION] ?: 30000L }
    val wipeOnMaxAttempts = context.dataStore.data.map { it[WIPE_ON_MAX_ATTEMPTS] ?: false }
    val requireBiometricsDestructive = context.dataStore.data.map { it[REQUIRE_BIOMETRICS_DESTRUCTIVE] ?: true }
    val autoCleanupEnabled = context.dataStore.data.map { it[AUTO_CLEANUP_ENABLED] ?: false }
    val notificationsEnabled = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }

    val bioOpenApp = context.dataStore.data.map { it[BIO_OPEN_APP] ?: true }
    val bioSwitchWorkspace = context.dataStore.data.map { it[BIO_SWITCH_WORKSPACE] ?: true }
    val bioExecuteClone = context.dataStore.data.map { it[BIO_EXECUTE_CLONE] ?: true }

    val chaosOsActiveFeatures: Flow<Set<String>> = context.dataStore.data.map { it[CHAOS_OS_ACTIVE_FEATURES] ?: emptySet() }

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

    suspend fun setTorRoutingEnabled(enabled: Boolean) = context.dataStore.edit { it[TOR_ROUTING_ENABLED] = enabled }
    suspend fun setDpiBypassEnabled(enabled: Boolean) = context.dataStore.edit { it[DPI_BYPASS_ENABLED] = enabled }
    suspend fun setDohProvider(provider: String) = context.dataStore.edit { it[DOH_PROVIDER] = provider }
    suspend fun setAdBlockEnabled(enabled: Boolean) = context.dataStore.edit { it[ADBLOCK_ENABLED] = enabled }

    suspend fun setSpoofImeiEnabled(enabled: Boolean) = context.dataStore.edit { it[SPOOF_IMEI_ENABLED] = enabled }
    suspend fun setSpoofSerialEnabled(enabled: Boolean) = context.dataStore.edit { it[SPOOF_SERIAL_ENABLED] = enabled }
    suspend fun setSpoofModelEnabled(enabled: Boolean) = context.dataStore.edit { it[SPOOF_MODEL_ENABLED] = enabled }
    suspend fun setBasebandIsolationEnabled(enabled: Boolean) = context.dataStore.edit { it[BASEBAND_ISOLATION_ENABLED] = enabled }

    suspend fun setBlockMotionSensors(enabled: Boolean) = context.dataStore.edit { it[BLOCK_MOTION_SENSORS] = enabled }
    suspend fun setBlockClipboard(enabled: Boolean) = context.dataStore.edit { it[BLOCK_CLIPBOARD] = enabled }
    suspend fun setBlockEnvSensors(enabled: Boolean) = context.dataStore.edit { it[BLOCK_ENV_SENSORS] = enabled }
    suspend fun setForceSecureFlag(enabled: Boolean) = context.dataStore.edit { it[FORCE_SECURE_FLAG] = enabled }
    suspend fun setDisableTelemetry(enabled: Boolean) = context.dataStore.edit { it[DISABLE_TELEMETRY] = enabled }
    suspend fun setAntiDozeMode(enabled: Boolean) = context.dataStore.edit { it[ANTI_DOZE_MODE] = enabled }
    suspend fun setForceBgAppops(enabled: Boolean) = context.dataStore.edit { it[FORCE_BG_APPOPS] = enabled }
    suspend fun setBlockLogcat(enabled: Boolean) = context.dataStore.edit { it[BLOCK_LOGCAT] = enabled }
    suspend fun setShuffleKeypad(enabled: Boolean) = context.dataStore.edit { it[SHUFFLE_KEYPAD] = enabled }
    suspend fun setCoercionPin(pin: String) = context.dataStore.edit { it[COERCION_PIN] = pin }
    suspend fun setDynamicStealthMode(enabled: Boolean) = context.dataStore.edit { it[DYNAMIC_STEALTH_MODE] = enabled }
    suspend fun setCamouflageNotifications(enabled: Boolean) = context.dataStore.edit { it[CAMOUFLAGE_NOTIFICATIONS] = enabled }

    suspend fun setMaxFailedAttempts(attempts: Int) = context.dataStore.edit { it[MAX_FAILED_ATTEMPTS] = attempts }
    suspend fun setLockoutDuration(duration: Long) = context.dataStore.edit { it[LOCKOUT_DURATION] = duration }
    suspend fun setWipeOnMaxAttempts(wipe: Boolean) = context.dataStore.edit { it[WIPE_ON_MAX_ATTEMPTS] = wipe }
    suspend fun setRequireBiometricsDestructive(require: Boolean) = context.dataStore.edit { it[REQUIRE_BIOMETRICS_DESTRUCTIVE] = require }
    suspend fun setAutoCleanupEnabled(enabled: Boolean) = context.dataStore.edit { it[AUTO_CLEANUP_ENABLED] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }

    suspend fun setBioOpenApp(require: Boolean) = context.dataStore.edit { it[BIO_OPEN_APP] = require }
    suspend fun setBioSwitchWorkspace(require: Boolean) = context.dataStore.edit { it[BIO_SWITCH_WORKSPACE] = require }
    suspend fun setBioExecuteClone(require: Boolean) = context.dataStore.edit { it[BIO_EXECUTE_CLONE] = require }
    suspend fun setChaosOsActiveFeatures(features: Set<String>) = context.dataStore.edit { it[CHAOS_OS_ACTIVE_FEATURES] = features }
}
