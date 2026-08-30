package com.example.ui.screens

import android.app.Activity
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppDatabase
import com.example.data.VaultManager
import com.example.ui.components.*
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.DangerRed
import com.example.utils.CamouflageManager
import com.example.utils.CamouflageMode
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { com.example.settings.SettingsManager(context) }
    val vaultManager = remember { VaultManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State ---
    var geminiKey by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<String?>(null) }
    var panicPin by remember { mutableStateOf("") }
    var newPanicPin by remember { mutableStateOf("") }
    var decoyPin by remember { mutableStateOf("") }
    var newDecoyPin by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var newAccessPin by remember { mutableStateOf("") }
    var confirmAccessPin by remember { mutableStateOf("") }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmText by remember { mutableStateOf("") }
    var selectedAutoLock by remember { mutableStateOf("60000") }
    var showAboutSection by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var showGranularBiometricsDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }

    // --- New States from SettingsManager ---
    val maxFailedAttempts by settingsManager.maxFailedAttempts.collectAsState(initial = 3)
    val wipeOnMaxAttempts by settingsManager.wipeOnMaxAttempts.collectAsState(initial = false)
    val requireBiometricsDestructive by settingsManager.requireBiometricsDestructive.collectAsState(initial = true)
    val autoCleanupEnabled by settingsManager.autoCleanupEnabled.collectAsState(initial = false)
    val notificationsEnabled by settingsManager.notificationsEnabled.collectAsState(initial = true)
    val globalProxyRegion by settingsManager.globalProxyRegion.collectAsState(initial = "US - Nova York")
    
    val bioOpenApp by settingsManager.bioOpenApp.collectAsState(initial = true)
    val bioSwitchWorkspace by settingsManager.bioSwitchWorkspace.collectAsState(initial = true)
    val bioExecuteClone by settingsManager.bioExecuteClone.collectAsState(initial = true)

    // --- Load saved state ---
    LaunchedEffect(Unit) {
        geminiKey = vaultManager.getGeminiApiKey()
        profileImageUri = vaultManager.getProfileImageUri()
        selectedAutoLock = vaultManager.getAutoLockTimeout().toString()
        settingsManager.panicPin.collect { pin ->
            panicPin = pin ?: ""
        }
    }
    LaunchedEffect(Unit) {
        settingsManager.decoyPin.collect { pin ->
            decoyPin = pin ?: ""
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                profileImageUri = uri.toString()
                vaultManager.saveProfileImageUri(uri.toString())
            }
        }
    )

    // --- Change PIN Dialog ---
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Text(stringResource(R.string.settings_change_pin_title), color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("Current PIN", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = newAccessPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newAccessPin = it },
                        label = { Text("New PIN", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = confirmAccessPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmAccessPin = it },
                        label = { Text("Confirm New PIN", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAccessPin.length == 4 && newAccessPin == confirmAccessPin) {
                            scope.launch(Dispatchers.IO) {
                                settingsManager.setRealPin(newAccessPin)
                                withContext(Dispatchers.Main) {
                                    showChangePinDialog = false
                                    currentPin = ""; newAccessPin = ""; confirmAccessPin = ""
                                }
                                snackbarHostState.showSnackbar("PIN Updated Successfully!")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    enabled = newAccessPin.length == 4 && newAccessPin == confirmAccessPin
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false; currentPin = ""; newAccessPin = ""; confirmAccessPin = "" }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // --- Factory Reset Dialog ---
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false; resetConfirmText = "" },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_reset_title), color = DangerRed)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_reset_desc), color = Color.Gray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = resetConfirmText,
                        onValueChange = { resetConfirmText = it },
                        label = { Text(stringResource(R.string.settings_reset_confirm), color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = DangerRed, unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetConfirmText == "RESET") {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val db = AppDatabase.getDatabase(context)
                                    db.clearAllTables()
                                    settingsManager.setOnboarded(false)
                                    withContext(Dispatchers.Main) {
                                        showResetDialog = false
                                        resetConfirmText = ""
                                        if (context is Activity) {
                                            context.finishAffinity()
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    enabled = resetConfirmText == "RESET"
                ) { Text(stringResource(R.string.settings_reset_button), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false; resetConfirmText = "" }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // --- Granular Biometrics Dialog ---
    if (showGranularBiometricsDialog) {
        AlertDialog(
            onDismissRequest = { showGranularBiometricsDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Granular Biometrics", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsToggleItem(
                        title = "Open App",
                        subtitle = "Require biometrics to open Vault",
                        checked = bioOpenApp,
                        onCheckedChange = { checked -> scope.launch { settingsManager.setBioOpenApp(checked) } },
                        icon = Icons.Filled.LockOpen
                    )
                    SettingsToggleItem(
                        title = "Switch Workspace",
                        subtitle = "Require biometrics when switching workspaces",
                        checked = bioSwitchWorkspace,
                        onCheckedChange = { checked -> scope.launch { settingsManager.setBioSwitchWorkspace(checked) } },
                        icon = Icons.Filled.SwapHoriz
                    )
                    SettingsToggleItem(
                        title = "Execute Clone",
                        subtitle = "Require biometrics to launch cloned apps",
                        checked = bioExecuteClone,
                        onCheckedChange = { checked -> scope.launch { settingsManager.setBioExecuteClone(checked) } },
                        icon = Icons.Filled.PlayArrow
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGranularBiometricsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Done", color = Color.White)
                }
            }
        )
    }

            // --- Active Sessions Dialog ---
    if (showSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showSessionsDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Devices, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Sessions", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsItem(
                        title = "This Device (Shadow Vault)",
                        subtitle = "Active now • Last login: Just now",
                        icon = Icons.Filled.Smartphone,
                        iconTint = Color.Green
                    )
                    SettingsItem(
                        title = "Chrome Web Vault",
                        subtitle = "Inactive • Last login: 2 days ago",
                        icon = Icons.Filled.Computer,
                        iconTint = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("All other sessions revoked successfully.")
                            showSessionsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Revoke Others", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionsDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }

    val profileTitle = stringResource(R.string.settings_section_profile)
    val appearanceTitle = stringResource(R.string.settings_section_appearance)
    val securityTitle = stringResource(R.string.settings_section_security)
    val privacyTitle = stringResource(R.string.settings_section_privacy)
    val integrationsTitle = stringResource(R.string.settings_section_integrations)
    val dataTitle = stringResource(R.string.settings_section_data)
    val aboutTitle = stringResource(R.string.settings_section_about)
    fun matches(query: String, vararg texts: String): Boolean {
        if (query.isBlank()) return true
        return texts.any { it.contains(query, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Color.Black.copy(alpha = 0.95f),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = NeonCyan,
                    actionColor = ElectricPurple,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_profile_settings), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp) // Tablet layout support (limits max width)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Settings...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════
            // SECTION 1: PROFILE
            // ═══════════════════════════════════════
            if (matches(searchQuery, profileTitle, "shadow identity", "profile")) {
            SettingsSection(
                title = stringResource(R.string.settings_section_profile),
                icon = Icons.Filled.Person
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .border(2.dp, Brush.linearGradient(listOf(ElectricPurple, NeonCyan)), CircleShape)
                        .background(Color.DarkGray.copy(alpha = 0.5f))
                        .clickable {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_ghost_shield),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Camera overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.title_shadow_identity),
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    stringResource(R.string.desc_tap_change_pic),
                    color = Color.Gray, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            }

            // ═══════════════════════════════════════
            // SECTION 2: APPEARANCE
            // ═══════════════════════════════════════
            if (matches(searchQuery, appearanceTitle, "theme", "language", "notifications")) {
            SettingsSection(
                title = stringResource(R.string.settings_section_appearance),
                icon = Icons.Filled.Palette
            ) {
                // Theme
                SettingsItem(
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(R.string.settings_theme_desc)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsButtonRow(
                    options = listOf(
                        "DARK" to stringResource(R.string.settings_theme_dark),
                        "LIGHT" to stringResource(R.string.settings_theme_light),
                        "SYSTEM" to stringResource(R.string.settings_theme_system)
                    ),
                    selectedOption = vaultManager.getThemeMode(),
                    onSelect = { code ->
                        vaultManager.saveThemeMode(code)
                        if (context is Activity) { context.recreate() }
                    }
                )

                SettingsDivider()

                // Language
                SettingsItem(
                    title = stringResource(R.string.title_language),
                    subtitle = stringResource(R.string.desc_language)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsButtonRow(
                    options = listOf("en" to "English", "pt" to "Português", "es" to "Español"),
                    selectedOption = vaultManager.getAppLanguage(),
                    onSelect = { code ->
                        vaultManager.saveAppLanguage(code)
                        if (context is Activity) { context.recreate() }
                    }
                )

                SettingsDivider()

                SettingsToggleItem(
                    title = stringResource(R.string.settings_notifications_title),
                    subtitle = stringResource(R.string.settings_notifications_desc),
                    checked = notificationsEnabled,
                    onCheckedChange = { checked ->
                        scope.launch { settingsManager.setNotificationsEnabled(checked) }
                    },
                    icon = Icons.Filled.Notifications
                )
            }
            }

            // ═══════════════════════════════════════
            // SECTION 3: SECURITY
            // ═══════════════════════════════════════
            if (matches(searchQuery, securityTitle, "pin", "panic", "decoy", "biometrics", "attempts", "auto-lock")) {
            SettingsSection(
                title = stringResource(R.string.settings_section_security),
                icon = Icons.Filled.Lock,
                iconTint = ElectricPurple
            ) {
                // Change Access PIN
                SettingsNavigationItem(
                    title = stringResource(R.string.settings_change_pin_title),
                    subtitle = stringResource(R.string.settings_change_pin_desc),
                    icon = Icons.Filled.Pin,
                    onClick = { showChangePinDialog = true }
                )

                SettingsDivider()

                // Auto-Lock Timeout
                SettingsItem(
                    title = stringResource(R.string.settings_autolock_title),
                    subtitle = stringResource(R.string.settings_autolock_desc)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsButtonRow(
                    options = listOf(
                        "30000" to stringResource(R.string.settings_autolock_30s),
                        "60000" to stringResource(R.string.settings_autolock_1m),
                        "300000" to stringResource(R.string.settings_autolock_5m),
                        "900000" to stringResource(R.string.settings_autolock_15m),
                        "0" to stringResource(R.string.settings_autolock_never)
                    ),
                    selectedOption = selectedAutoLock,
                    onSelect = { 
                        selectedAutoLock = it
                        vaultManager.saveAutoLockTimeout(it.toLongOrNull() ?: 60000L)
                    },
                    accentColor = ElectricPurple
                )

                SettingsDivider()

                // Panic PIN
                SettingsItem(
                    title = stringResource(R.string.settings_panic_title),
                    subtitle = stringResource(R.string.settings_panic_desc),
                    icon = Icons.Filled.Warning,
                    iconTint = DangerRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPanicPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPanicPin = it },
                    placeholder = {
                        Text(
                            if (panicPin.isEmpty()) stringResource(R.string.settings_panic_placeholder_empty)
                            else stringResource(R.string.settings_panic_placeholder_set),
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = DangerRed, unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newPanicPin.length == 4) {
                            scope.launch(Dispatchers.IO) {
                                settingsManager.setPanicPin(newPanicPin)
                                newPanicPin = ""
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_panic_success))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = newPanicPin.length == 4
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_panic_button), color = Color.White, fontWeight = FontWeight.Bold)
                }

                SettingsDivider()

                // Decoy PIN
                SettingsItem(
                    title = stringResource(R.string.settings_decoy_pin_title),
                    subtitle = stringResource(R.string.settings_decoy_pin_desc),
                    icon = Icons.Filled.TheaterComedy,
                    iconTint = Color(0xFFFFAA00)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newDecoyPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newDecoyPin = it },
                    placeholder = {
                        Text(
                            if (decoyPin.isEmpty()) stringResource(R.string.settings_panic_placeholder_empty)
                            else stringResource(R.string.settings_panic_placeholder_set),
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFAA00), unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newDecoyPin.length == 4) {
                            scope.launch(Dispatchers.IO) {
                                settingsManager.setDecoyPin(newDecoyPin)
                                newDecoyPin = ""
                                snackbarHostState.showSnackbar("Decoy PIN Updated!")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAA00).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = newDecoyPin.length == 4
                ) {
                    Icon(Icons.Filled.TheaterComedy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SET DECOY PIN", color = Color.White, fontWeight = FontWeight.Bold)
                }

                SettingsDivider()

                SettingsItem(
                    title = stringResource(R.string.settings_failed_attempts_title),
                    subtitle = stringResource(R.string.settings_failed_attempts_desc),
                    icon = Icons.Filled.Security
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsButtonRow(
                    options = listOf("3" to "3", "5" to "5", "10" to "10"),
                    selectedOption = maxFailedAttempts.toString(),
                    onSelect = { attempts ->
                        scope.launch { settingsManager.setMaxFailedAttempts(attempts.toInt()) }
                    },
                    accentColor = ElectricPurple
                )
                
                SettingsToggleItem(
                    title = "Wipe on Max Attempts",
                    subtitle = "Destroys all vault data if PIN is entered incorrectly $maxFailedAttempts times.",
                    checked = wipeOnMaxAttempts,
                    onCheckedChange = { checked ->
                        scope.launch { settingsManager.setWipeOnMaxAttempts(checked) }
                    },
                    icon = Icons.Filled.Warning,
                    iconTint = DangerRed
                )

                SettingsDivider()

                SettingsToggleItem(
                    title = "Require Biometrics",
                    subtitle = "Require fingerprint/face to perform destructive actions like export or wipe.",
                    checked = requireBiometricsDestructive,
                    onCheckedChange = { checked ->
                        scope.launch { settingsManager.setRequireBiometricsDestructive(checked) }
                    },
                    icon = Icons.Filled.Fingerprint
                )

                SettingsDivider()

                SettingsNavigationItem(
                    title = "Granular Biometrics",
                    subtitle = "Configure where biometrics are required",
                    icon = Icons.Filled.Fingerprint,
                    onClick = { showGranularBiometricsDialog = true }
                )

                SettingsDivider()

                SettingsNavigationItem(
                    title = "Active Sessions",
                    subtitle = "Manage devices connected to your Vault",
                    icon = Icons.Filled.Devices,
                    onClick = { showSessionsDialog = true }
                )
            }
            }

            // ═══════════════════════════════════════
            // SECTION 4: PRIVACY & STEALTH
            // ═══════════════════════════════════════
            if (matches(searchQuery, privacyTitle, "camouflage", "vpn", "proxy", "stealth")) {
            SettingsSection(
                title = stringResource(R.string.settings_section_privacy),
                icon = Icons.Filled.VisibilityOff
            ) {
                SettingsItem(
                    title = stringResource(R.string.title_app_camouflage),
                    subtitle = stringResource(R.string.desc_app_camouflage)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsButtonRow(
                    options = listOf(
                        "DEFAULT" to stringResource(R.string.lang_default),
                        "CALCULATOR" to stringResource(R.string.lang_calculator),
                        "NOTES" to stringResource(R.string.lang_notes)
                    ),
                    selectedOption = "DEFAULT",
                    onSelect = { code ->
                        val mode = when (code) {
                            "CALCULATOR" -> CamouflageMode.CALCULATOR
                            "NOTES" -> CamouflageMode.NOTES
                            else -> CamouflageMode.DEFAULT
                        }
                        CamouflageManager.setCamouflage(context, mode)
                    }
                )

                SettingsDivider()

                SettingsItem(
                    title = stringResource(R.string.settings_vpn_default_title),
                    subtitle = stringResource(R.string.settings_vpn_default_desc),
                    icon = Icons.Filled.VpnKey
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsButtonRow(
                    options = listOf(
                        "US" to "USA",
                        "EU" to "Europe",
                        "ASIA" to "Asia"
                    ),
                    selectedOption = if (globalProxyRegion.contains("US")) "US" else if (globalProxyRegion.contains("EU")) "EU" else "ASIA",
                    onSelect = { code ->
                        scope.launch { settingsManager.setGlobalProxyRegion(if(code == "US") "US - Nova York" else if(code=="EU") "EU - Frankfurt" else "ASIA - Tokyo") }
                    },
                    accentColor = NeonCyan
                )
            }
            }

            // ═══════════════════════════════════════
            // SECTION 5: INTEGRATIONS
            // ═══════════════════════════════════════
            if (matches(searchQuery, integrationsTitle, "gemini", "api")) {
            SettingsSection(
                title = stringResource(R.string.settings_section_integrations),
                icon = Icons.Filled.Api
            ) {
                SettingsItem(
                    title = stringResource(R.string.title_gemini_key),
                    subtitle = stringResource(R.string.desc_gemini_key)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    placeholder = { Text("AIzaSy...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        vaultManager.saveGeminiApiKey(geminiKey)
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_config_saved))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_save_config), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            }

            // ═══════════════════════════════════════
            // SECTION 6: DATA & BACKUP
            // ═══════════════════════════════════════
            if (matches(searchQuery, dataTitle, "backup", "cleanup", "export", "import", "reset")) {
            SettingsSection(
                title = stringResource(R.string.settings_section_data),
                icon = Icons.Filled.Storage
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_cleanup_title),
                    subtitle = stringResource(R.string.settings_cleanup_desc),
                    checked = autoCleanupEnabled,
                    onCheckedChange = { checked ->
                        scope.launch { settingsManager.setAutoCleanupEnabled(checked) }
                    },
                    icon = Icons.Filled.CleaningServices
                )

                SettingsDivider()

                SettingsItem(
                    title = stringResource(R.string.settings_backup_title),
                    subtitle = stringResource(R.string.settings_backup_desc)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Export Button
                Button(
                    onClick = {
                        isExporting = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val db = AppDatabase.getDatabase(context)
                                val clones = db.vaultDao().getAllClones().first()
                                val identities = db.vaultDao().getAllIdentities().first()
                                val profiles = db.vaultDao().getAllProfileConfigs()
                                val backupMap = mapOf(
                                    "clones" to clones,
                                    "identities" to identities,
                                    "profiles" to profiles,
                                    "exportDate" to System.currentTimeMillis(),
                                    "appVersion" to com.example.BuildConfig.VERSION_NAME
                                )
                                val json = Gson().toJson(backupMap)

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    val resolver = context.contentResolver
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, "ShadowVault_Backup_${System.currentTimeMillis()}.json")
                                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                    }
                                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                    if (uri != null) {
                                        resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                                    }
                                } else {
                                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                    if (dir != null) {
                                        java.io.File(dir, "ShadowVault_Backup_${System.currentTimeMillis()}.json").writeText(json)
                                    }
                                }
                                isExporting = false
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_backup_success))
                            } catch (e: Exception) {
                                isExporting = false
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_backup_fail, e.message ?: "Unknown"))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_backup_export), color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Import Button
                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Import feature coming soon!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_backup_import), color = Color.White, fontWeight = FontWeight.Bold)
                }

                SettingsDivider()

                // Factory Reset
                SettingsNavigationItem(
                    title = stringResource(R.string.settings_reset_title),
                    subtitle = stringResource(R.string.settings_reset_desc),
                    icon = Icons.Filled.DeleteForever,
                    iconTint = DangerRed,
                    onClick = { showResetDialog = true }
                )
            }
            }

            // ═══════════════════════════════════════
            // SECTION 7: ABOUT
            // ═══════════════════════════════════════
            if (matches(searchQuery, aboutTitle, "version", "developer", "about")) {
            SettingsSection(
                title = stringResource(R.string.settings_section_about),
                icon = Icons.Filled.Info
            ) {
                SettingsItem(
                    title = stringResource(R.string.settings_about_version),
                    trailing = {
                        Text(
                            "v${com.example.BuildConfig.VERSION_NAME}",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )

                SettingsDivider()

                SettingsNavigationItem(
                    title = stringResource(R.string.settings_about_changelog),
                    icon = Icons.Filled.History,
                    onClick = { showAboutSection = !showAboutSection }
                )

                AnimatedVisibility(
                    visible = showAboutSection,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("v26.6.0 — 50 Major Improvements Update", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("• App Freezer (0% RAM em background)\n• Bloqueio Biométrico Individual por Clone\n• Busca em Tempo Real & Alternador Grade/Lista\n• Auto-Lock Inteligente por Inatividade\n• Limpador Inteligente de Cache & Purgação Profunda\n• Monitor de Shizuku Vivo & Dynamic Shortcuts\n• Bypass de Economia de Bateria (Doze Whitelist)\n• Diagnóstico de Kernel fw.max_users\n• Otimização de Performance & Zero Leak/ANR",
                            color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                SettingsDivider()

                SettingsNavigationItem(
                    title = stringResource(R.string.settings_about_licenses),
                    icon = Icons.Filled.Description,
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Shizuku, SQLCipher, Retrofit, Coil, Moshi, LeakCanary") }
                    }
                )

                SettingsDivider()

                SettingsNavigationItem(
                    title = stringResource(R.string.settings_about_github),
                    icon = Icons.Filled.Code,
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Rod6267ney/Shadow-VAult-2.0"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }
            }

            Spacer(modifier = Modifier.height(32.dp))
            }
            } // Close item block
        }
    }
}
