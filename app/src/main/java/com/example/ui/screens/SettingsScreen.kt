package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonCyan
import com.example.utils.CamouflageManager
import com.example.utils.CamouflageMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.VisibilityOff

import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.AppDatabase
import com.google.gson.Gson

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import com.example.data.VaultManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.app.Activity
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Build

import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.filled.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var geminiKey by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<String?>(null) }
    var panicPin by remember { mutableStateOf("") }
    var panicPinSetMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = com.example.settings.SettingsManager(context)
    
    val vaultManager = remember { VaultManager(context) }
    
    LaunchedEffect(Unit) {
        geminiKey = vaultManager.getGeminiApiKey()
        profileImageUri = vaultManager.getProfileImageUri()
        settingsManager.panicPin.collect { pin ->
            panicPin = pin ?: ""
        }
    }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Grant read permission just in case
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                profileImageUri = uri.toString()
                vaultManager.saveProfileImageUri(uri.toString())
            }
        }
    )
    
    Scaffold(
        containerColor = Color.Black.copy(alpha = 0.95f),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.example.R.string.title_profile_settings), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Picture Section
            Box(
                modifier = Modifier
                    .size(120.dp)
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
                        contentDescription = "Profile Pic",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = com.example.R.drawable.ic_ghost_shield),
                        contentDescription = "Profile Pic",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(com.example.R.string.title_shadow_identity), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(com.example.R.string.desc_tap_change_pic), color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Theme Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Build, contentDescription = "Theme", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tema Visual (Material 3)", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Alterne entre os modos Claro e Escuro com paleta dinâmica e superfícies translúcidas.", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val currentTheme = vaultManager.getThemeMode()
                    val themes = listOf("DARK" to "Escuro", "LIGHT" to "Claro", "SYSTEM" to "Sistema")
                    themes.forEach { (code, name) ->
                        Button(
                            onClick = { 
                                vaultManager.saveThemeMode(code)
                                if (context is Activity) {
                                    context.recreate()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentTheme == code) ElectricPurple else Color.DarkGray
                            )
                        ) { Text(name, color = Color.White) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Language Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, contentDescription = "Language", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(com.example.R.string.title_language), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(com.example.R.string.desc_language), color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val currentLang = vaultManager.getAppLanguage()
                    val langs = listOf("en" to "English", "pt" to "Português", "es" to "Español")
                    langs.forEach { (code, name) ->
                        Button(
                            onClick = { 
                                vaultManager.saveAppLanguage(code)
                                if (context is Activity) {
                                    context.recreate()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLang == code) ElectricPurple else Color.DarkGray
                            )
                        ) { Text(name, color = Color.White) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Gemini API Key Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = "API Key", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(com.example.R.string.title_gemini_key), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(com.example.R.string.desc_gemini_key), color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    placeholder = { Text("AIzaSy...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        vaultManager.saveGeminiApiKey(geminiKey)
                        android.widget.Toast.makeText(context, context.getString(com.example.R.string.msg_config_saved), android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(com.example.R.string.action_save_config), color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Camouflage Mode Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VisibilityOff, contentDescription = "Camouflage", tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(com.example.R.string.title_app_camouflage), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(com.example.R.string.desc_app_camouflage), color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { CamouflageManager.setCamouflage(context, CamouflageMode.DEFAULT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) { Text(stringResource(com.example.R.string.lang_default)) }
                        Button(
                            onClick = { CamouflageManager.setCamouflage(context, CamouflageMode.CALCULATOR) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) { Text(stringResource(com.example.R.string.lang_calculator)) }
                        Button(
                            onClick = { CamouflageManager.setCamouflage(context, CamouflageMode.NOTES) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) { Text(stringResource(com.example.R.string.lang_notes)) }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Panic PIN Mode Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = "Panic PIN", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Panic PIN (Self-Destruct)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("If you enter this PIN on the lock screen, it will simulate a system crash and lock out the vault to protect you in an emergency.", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var newPanicPin by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = newPanicPin,
                        onValueChange = { if(it.length <= 4 && it.all { char -> char.isDigit() }) newPanicPin = it },
                        placeholder = { Text(if (panicPin.isEmpty()) "Enter 4-digit PIN" else "PIN is set... Change?", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (panicPinSetMessage.isNotEmpty()) {
                        Text(panicPinSetMessage, color = NeonCyan, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            if (newPanicPin.length == 4) {
                                scope.launch(Dispatchers.IO) {
                                    settingsManager.setPanicPin(newPanicPin)
                                    newPanicPin = ""
                                    panicPinSetMessage = "Panic PIN Updated Successfully!"
                                    kotlinx.coroutines.delay(2000)
                                    panicPinSetMessage = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SET PANIC PIN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))


                
                Spacer(modifier = Modifier.height(48.dp))
                // Backup Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = "Backup", tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vault Backup", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Export your configurations (Clones, Identities, Profiles) to the Downloads folder as a JSON file, or copy it. (Apps themselves are not backed up, only configurations).", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { 
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val db = AppDatabase.getDatabase(context)
                                    val clones = db.vaultDao().getAllClones().first()
                                    val identities = db.vaultDao().getAllIdentities().first()
                                    val profiles = db.vaultDao().getAllProfileConfigs()
                                    
                                    val backupMap = mapOf(
                                        "clones" to clones,
                                        "identities" to identities,
                                        "profiles" to profiles
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
                                            resolver.openOutputStream(uri)?.use {
                                                it.write(json.toByteArray())
                                            }
                                        }
                                    } else {
                                        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                        if (dir != null) {
                                            val file = java.io.File(dir, "ShadowVault_Backup_${System.currentTimeMillis()}.json")
                                            file.writeText(json)
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Backup exported to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } catch(e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("EXPORT BACKUP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                
                // Version Info
                Text(
                    text = "v${com.example.BuildConfig.VERSION_NAME}",
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}
