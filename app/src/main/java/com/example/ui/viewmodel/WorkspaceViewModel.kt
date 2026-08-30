package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiIdentityGenerator
import com.example.data.AppDatabase
import com.example.data.IdentityEntity
import com.example.data.InstanceConfigEntity
import com.example.data.WorkspaceWithIdentity
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class WorkspaceCreationState {
    object Idle : WorkspaceCreationState()
    data class Loading(val message: String) : WorkspaceCreationState()
    data class Success(val workspaceName: String) : WorkspaceCreationState()
    data class Error(val errorMessage: String) : WorkspaceCreationState()
}

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = com.example.data.VaultRepository(AppDatabase.getDatabase(application).vaultDao())

    private val _creationState = MutableStateFlow<WorkspaceCreationState>(WorkspaceCreationState.Idle)
    val creationState: StateFlow<WorkspaceCreationState> = _creationState.asStateFlow()

    val workspacesWithIdentities: StateFlow<List<WorkspaceWithIdentity>> =
        repository.getAllWorkspacesWithIdentities()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val sysSpaces = ShizukuUtils.getWorkspaces(application)
            val dbSpaces: List<WorkspaceWithIdentity> = repository.getAllWorkspacesWithIdentities().firstOrNull() ?: emptyList()
            val dbSpaceIds = dbSpaces.map { it.instanceConfig.workspaceId }.toSet()
            
            for (sysSpace in sysSpaces) {
                if (sysSpace.id !in dbSpaceIds && sysSpace.id != "0") {
                    val identity = IdentityEntity(
                        fakeName = sysSpace.fakeName,
                        jobTitle = sysSpace.fakeCompany.substringBefore(" • "),
                        location = sysSpace.fakeCompany.substringAfter(" • "),
                        email = sysSpace.fakeEmail,
                        address = "",
                        dob = "",
                        profileIdea = "Auto-synced from Shizuku profile"
                    )
                    val instanceConfig = InstanceConfigEntity(
                        workspaceId = sysSpace.id,
                        workspaceName = sysSpace.name,
                        fakeName = sysSpace.fakeName,
                        fakeEmail = sysSpace.fakeEmail,
                        fakePhone = sysSpace.fakePhone,
                        fakeCompany = sysSpace.fakeCompany,
                        vpnEnabled = false,
                        proxyRegion = sysSpace.proxyRegion,
                        iconName = sysSpace.iconName,
                        unlimitedClones = sysSpace.unlimitedClones,
                        identityId = identity.id,
                        workspaceType = "NATIVE_WORK_PROFILE"
                    )
                    repository.createWorkspaceAndIdentityTransaction(identity, instanceConfig)
                }
            }
        }
    }


    fun hibernateAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbConfigs = com.example.data.AppDatabase.getDatabase(getApplication()).vaultDao().getAllInstanceConfigs().firstOrNull() ?: emptyList()
            for (config in dbConfigs) {
                if (!config.workspaceId.startsWith("v_")) {
                    com.example.utils.ShizukuUtils.executeCommand("am stop-user -f ${config.workspaceId}")
                }
            }
        }
    }

    fun resetState() {
        _creationState.value = WorkspaceCreationState.Idle
    }

    fun provisionWorkspaceWithIdentity(
        workspaceName: String,
        workspaceType: String,
        iconName: String,
        unlimitedClones: Boolean,
        useResidentialProxy: Boolean,
        selectedProxyRegion: String,
        killSwitchEnabled: Boolean = false,
        hardwareSpoofingEnabled: Boolean = false,
        burnerModeEnabled: Boolean = false,
        fakeGpsRegion: String = "",
        personaArchetype: String = "Anônimo",
        generateComplexPasswords: Boolean = false,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _creationState.value = WorkspaceCreationState.Loading("Gerando Identidade Sintética via Gemini AI...")
            try {
                val context = getApplication<Application>()
                val vaultName = workspaceName.ifBlank { "Novo Vault" }
                val prompt = "Identidade sintética para o workspace $vaultName, arquétipo: $personaArchetype"

                // 1. Immediately trigger identity generation via Gemini AI
                val generator = AiIdentityGenerator(context)
                val jsonStr = generator.generateIdentity(prompt)
                val cleanJsonStr = jsonStr.replace("```json", "").replace("```", "").trim()

                var fName = "Persona Anônima"
                var fJob = "Consultor Indoc"
                var fLoc = "São Paulo, BR"
                var fEmail = "shadow.persona@vault.sec"
                var fAddress = "Av. Paulista, 1000"
                var fDob = "1990-01-01"

                try {
                    val jsonObj = JSONObject(cleanJsonStr)
                    fName = jsonObj.optString("fakeName", fName)
                    fJob = jsonObj.optString("jobTitle", fJob)
                    fLoc = jsonObj.optString("location", fLoc)
                    fEmail = jsonObj.optString("email", fEmail)
                    fAddress = jsonObj.optString("address", fAddress)
                    fDob = jsonObj.optString("dob", fDob)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Provision Shizuku user
                _creationState.value = WorkspaceCreationState.Loading("Provisionando Perfil de Trabalho...")
                val result = ShizukuUtils.createWorkProfile(vaultName)
                val workspaceId = result.getOrNull()
                if (workspaceId != null) {
                    ShizukuUtils.executeCommand("am start-user $workspaceId")
                    ShizukuUtils.executeCommand("settings put --user $workspaceId secure chaos_vault_icon '$iconName'")
                    if (unlimitedClones) {
                        ShizukuUtils.executeCommand("settings put --user $workspaceId secure chaos_unlimited_clones 'true'")
                    }
                    if (useResidentialProxy) {
                        ShizukuUtils.executeCommand("settings put --user $workspaceId secure chaos_proxy_region '$selectedProxyRegion'")
                        com.example.vpn.VpnManager.enableWorkspaceVpn(context, workspaceId, selectedProxyRegion)
                    }
                    if (killSwitchEnabled) {
                        ShizukuUtils.executeCommand("settings put --user $workspaceId secure chaos_kill_switch 'true'")
                    }
                    if (hardwareSpoofingEnabled) {
                        ShizukuUtils.executeCommand("settings put --user $workspaceId secure chaos_hardware_spoofing 'true'")
                    }
                    if (fakeGpsRegion.isNotBlank()) {
                        ShizukuUtils.executeCommand("settings put --user $workspaceId secure chaos_fake_gps '$fakeGpsRegion'")
                    }
                } else {
                    _creationState.value = WorkspaceCreationState.Error("Falha ao criar Perfil via Shizuku.")
                    return@launch
                }

                _creationState.value = WorkspaceCreationState.Loading("Persistindo Workspace e Identidade em Transação Única...")

                // 3. Construct IdentityEntity and InstanceConfigEntity
                val identity = IdentityEntity(
                    fakeName = fName,
                    jobTitle = fJob,
                    location = fLoc,
                    profileIdea = prompt,
                    dob = fDob,
                    address = fAddress,
                    email = fEmail
                )

                val instanceConfig = InstanceConfigEntity(
                    workspaceId = workspaceId,
                    workspaceName = vaultName,
                    fakeName = fName,
                    fakeEmail = fEmail,
                    fakePhone = "+55 11 9${(10000000..99999999).random()}",
                    fakeCompany = "$fJob • $fLoc",
                    vpnEnabled = useResidentialProxy,
                    proxyRegion = selectedProxyRegion,
                    iconName = iconName,
                    unlimitedClones = unlimitedClones,
                    identityId = identity.id,
                    workspaceType = "NATIVE_WORK_PROFILE"
                )

                // 4. Persist workspace and identity as a SINGLE ATOMIC TRANSACTION in Room database
                repository.createWorkspaceAndIdentityTransaction(identity, instanceConfig)
                
                // 5. Apply settings if work profile
                try {
                    ShizukuUtils.executeCommand("settings put --user $workspaceId secure fake_identity_name '$fName'")
                    ShizukuUtils.executeCommand("settings put --user $workspaceId secure fake_identity_email '$fEmail'")
                    ShizukuUtils.executeCommand("settings put --user $workspaceId secure fake_identity_company '$fJob • $fLoc'")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _creationState.value = WorkspaceCreationState.Success(vaultName)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                _creationState.value = WorkspaceCreationState.Error(e.localizedMessage ?: "Erro ao provisionar workspace")
            }
        }
    }
}
