wizard_code = """
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AppSelectionWizard(viewModel: ClonesViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val installedApps = remember { viewModel.loadInstalledApps(context) }
    var selectedApp by remember { mutableStateOf<ApplicationInfo?>(null) }
    
    // Screens: 0 = App Selection, 1 = Config, 2 = Device Privacy, 3 = Location Privacy
    var currentScreen by remember { mutableIntStateOf(0) }
    
    // Config states
    var cloneName by remember { mutableStateOf("") }
    var chaosOsEnabled by remember { mutableStateOf(false) }
    var chaosOsVersion by remember { mutableStateOf("v2.7.1") }
    
    // Device Privacy states
    var enableDevicePrivacy by remember { mutableStateOf(false) }
    var spoofAndroidId by remember { mutableStateOf(false) }
    var spoofImei by remember { mutableStateOf(false) }
    var spoofMac by remember { mutableStateOf(false) }
    var spoofGoogleAdId by remember { mutableStateOf(false) }
    var spoofSimInfo by remember { mutableStateOf(false) }
    var selectedBrandModel by remember { mutableStateOf("Apple - Game phone Black shark 4pro") }
    
    // Location Privacy states
    var spoofLocation by remember { mutableStateOf(false) }
    var fakeLat by remember { mutableStateOf("0.0") }
    var fakeLng by remember { mutableStateOf("0.0") }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VaultBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentScreen > 1) {
                            currentScreen = 1
                        } else if (currentScreen == 1) {
                            currentScreen = 0
                            selectedApp = null
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = when (currentScreen) {
                            0 -> "Select App to Clone"
                            1 -> "Clone Application"
                            2 -> "Device Privacy"
                            3 -> "Location Privacy"
                            else -> ""
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentScreen) {
                        0 -> {
                            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                items(installedApps) { appInfo ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { 
                                                selectedApp = appInfo
                                                cloneName = appInfo.loadLabel(context.packageManager).toString() + " Clone"
                                                currentScreen = 1
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = ElectricPurple)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(appInfo.loadLabel(context.packageManager).toString(), color = Color.White)
                                            Text(appInfo.packageName, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Divider(color = Color.White.copy(alpha=0.05f))
                                }
                            }
                        }
                        1 -> {
                            // Main Config Screen (Clone App Pro Style)
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("The cloned application runs in a virtual machine and will generate an installation file (apk) for the virtual machine to run according to the configuration", 
                                    color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))
                                
                                OutlinedTextField(
                                    value = cloneName,
                                    onValueChange = { cloneName = it },
                                    label = { Text("Name", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedBorderColor = NeonCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Settings list
                                SettingRow(title = "Chaos OS", subtitle = "If the cloned application doesn't work with this Chaos OS, you can try switching to another version", value = chaosOsVersion) {
                                    chaosOsEnabled = !chaosOsEnabled
                                }
                                SettingRow(title = "General settings", subtitle = "Set up a virtual SD card, Google services, etc.") {}
                                SettingRow(title = "Device privacy", subtitle = "Set brand model, change device ID, etc.", icon = Icons.Filled.Security) {
                                    currentScreen = 2
                                }
                                SettingRow(title = "Location privacy", subtitle = "Set fake GPS location, virtual address book, etc.", icon = Icons.Filled.LocationOn) {
                                    currentScreen = 3
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    FrostedButton(
                                        onClick = {
                                            // Trigger installation with Internal VM
                                            viewModel.installCloneToWorkspace(
                                                context = context, 
                                                appToClone = selectedApp!!, 
                                                targetUserId = null, 
                                                isVirtual = true, 
                                                fakeAndroidId = if(spoofAndroidId) "dummy_android_id" else null,
                                                fakeBrand = if(enableDevicePrivacy) selectedBrandModel else null,
                                                fakeGps = if(spoofLocation) "$fakeLat,$fakeLng" else null,
                                                customName = cloneName
                                            )
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        color = NeonCyan
                                    ) {
                                        Text("Internal installation")
                                    }
                                    
                                    FrostedButton(
                                        onClick = {
                                            // Trigger installation with Native Work Profile
                                            viewModel.installCloneToWorkspace(
                                                context = context, 
                                                appToClone = selectedApp!!, 
                                                targetUserId = null, 
                                                isVirtual = false,
                                                fakeAndroidId = if(spoofAndroidId) "dummy_android_id" else null,
                                                fakeBrand = if(enableDevicePrivacy) selectedBrandModel else null,
                                                fakeGps = if(spoofLocation) "$fakeLat,$fakeLng" else null,
                                                customName = cloneName
                                            )
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        color = ElectricPurple
                                    ) {
                                        Text("Standalone installation")
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        2 -> {
                            // Device Privacy Screen
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom=16.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("brand/model", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Choose another brand model, the clone application will not get the real device brand model logo", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Switch(checked = enableDevicePrivacy, onCheckedChange = { enableDevicePrivacy = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
                                    }
                                    Divider(color = Color.White.copy(alpha=0.05f))
                                }
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical=16.dp)) {
                                        Text("Open a new identity with one click", color = Color.White)
                                        Switch(checked = enableDevicePrivacy, onCheckedChange = { enableDevicePrivacy = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
                                    }
                                    Divider(color = Color.White.copy(alpha=0.05f))
                                }
                                item { ToggleSettingRow("Android ID", "Report the changed Android ID to the cloned application to prevent obtaining the native Android ID", spoofAndroidId) { spoofAndroidId = it } }
                                item { ToggleSettingRow("IMEI/IMSI", "Change the IMEI/IMSI number of the phone in the clone application", spoofImei) { spoofImei = it } }
                                item { ToggleSettingRow("WiFi/Bluetooth MAC address", "Change your WiFi/Bluetooth mac address", spoofMac) { spoofMac = it } }
                                item { ToggleSettingRow("Google Advertising ID", "Report a random Google advertising ID to the cloned application to prevent it from being tracked by advertising", spoofGoogleAdId) { spoofGoogleAdId = it } }
                                item { ToggleSettingRow("SIM card and carrier information", "Hide local SIM card serial number, phone number, phone location, carrier identifier/name/country and other information", spoofSimInfo) { spoofSimInfo = it } }
                            }
                        }
                        3 -> {
                            // Location Privacy
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom=16.dp)) {
                                    Text("Location simulation", color = Color.White, fontWeight = FontWeight.Bold)
                                    Switch(checked = spoofLocation, onCheckedChange = { spoofLocation = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
                                }
                                if (spoofLocation) {
                                    OutlinedTextField(
                                        value = fakeLat,
                                        onValueChange = { fakeLat = it },
                                        label = { Text("Latitude", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)
                                    )
                                    OutlinedTextField(
                                        value = fakeLng,
                                        onValueChange = { fakeLng = it },
                                        label = { Text("Longitude", color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text("Location simulation is not enabled", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top=32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, value: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ElectricPurple, modifier = Modifier.padding(end=16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (value != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(value, color = NeonCyan, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun ToggleSettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha=0.3f)))
    }
}
"""

with open('app/src/main/java/com/example/ui/screens/ClonesScreen.kt', 'a') as f:
    f.write(wizard_code)

