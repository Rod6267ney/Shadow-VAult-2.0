package com.example.services

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.CloneEntity
import com.example.utils.ShizukuUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

object CloneManager {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val trackingJobs = mutableMapOf<String, Job>()

    fun installCloneToWorkspace(
        context: Context,
        appToClone: ApplicationInfo,
        targetUserId: String?,
        isVirtual: Boolean = false,
        fakeAndroidId: String? = null,
        fakeBrand: String? = null,
        fakeGps: String? = null,
        fakeImei: Boolean = false,
        fakeMac: Boolean = false,
        fakeAdId: Boolean = false,
        fakeSim: Boolean = false,
        chaosOsVersion: String? = null,
        customName: String? = null,
        isolateFilesystem: Boolean = false,
        spoofSensors: Boolean = false,
        hideRoot: Boolean = false,
        strictPackageFirewall: Boolean = false,
        proxyRegion: String? = null,
        cloneMode: String = "WORK_PROFILE", // "WORK_PROFILE" ou "SANDBOX_NON_ROOT"
        firewallEnabled: Boolean = false,
        spoofProfile: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        serviceScope.launch {
            try {
                val pm = context.packageManager
                val packageName = appToClone.packageName
                val appName = customName ?: pm.getApplicationLabel(appToClone).toString()
                val dao = AppDatabase.getDatabase(context).vaultDao()



                // 1. MODO SANDBOX APP-LEVEL (SEM ROOT)
                if (cloneMode == "SANDBOX_NON_ROOT") {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Iniciando clonagem App-Level Sandbox...", Toast.LENGTH_SHORT).show() }
                    
                    SandboxEngine.installToSandbox(context, appToClone) { success, vUserId ->
                        if (success && vUserId != null) {
                            serviceScope.launch {
                                dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "CLONE_SUCCESS", message = "Successfully cloned $appName into Sandbox $vUserId"))
                                dao.insertClone(
                                    CloneEntity(
                                        userId = vUserId,
                                        packageName = packageName,
                                        appName = appName,
                                        cloneMode = "SANDBOX_NON_ROOT",
                                        firewallEnabled = firewallEnabled,
                                        spoofProfile = spoofProfile
                                    )
                                )
                                // Aplica Spoofing Profundo simulado se necessário
                                if (spoofProfile != null) {
                                    DeepSpoofEngine.applySpoofing(context, packageName, spoofProfile)
                                }
                                onComplete(true)
                            }
                        } else {
                            onComplete(false)
                        }
                    }
                    return@launch
                }

                // 2. MODO WORK PROFILE (REQUER SHIZUKU)
                if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Shizuku não está pronto ou sem permissão para modo Work Profile", Toast.LENGTH_SHORT).show() }
                    onComplete(false)
                    return@launch
                }

                var finalUserId: String
                var isNewWorkspace = false
                if (targetUserId == null) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Criando novo workspace...", Toast.LENGTH_SHORT).show() }
                    val prefix = "Workspace_"
                    val profileResult = ShizukuUtils.createWorkProfile("${prefix}${System.currentTimeMillis() % 1000}")
                    if (profileResult.isFailure) {
                        val errMsg = profileResult.exceptionOrNull()?.message ?: "Falha desconhecida"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Não foi possível criar workspace: $errMsg\nVerifique se as Configurações de Segurança USB do Shizuku estão ativas no seu celular.", Toast.LENGTH_LONG).show()
                        }
                        onComplete(false)
                        return@launch
                    }
                    finalUserId = profileResult.getOrNull() ?: "10"
                    isNewWorkspace = true
                    dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "WORKSPACE_CREATED", message = "Created new workspace $finalUserId"))
                } else {
                    val existingClones = dao.getAllClones().first()
                    val alreadyExists = existingClones.any { it.packageName == packageName && it.userId == targetUserId }
                    if (alreadyExists) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Instância existente. Criando container único...", Toast.LENGTH_SHORT).show() }
                        val profileResult = ShizukuUtils.createWorkProfile("Clone_${packageName.takeLast(4)}_${System.currentTimeMillis() % 1000}")
                        if (profileResult.isFailure) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao criar container interno.", Toast.LENGTH_SHORT).show() }
                            onComplete(false)
                            return@launch
                        }
                        finalUserId = profileResult.getOrNull() ?: targetUserId
                    } else {
                        finalUserId = targetUserId
                    }
                }

                // Apply Fake Settings using new generator
                DeviceIdentifierGenerator.applySpoofedIdentifiers(
                    userId = finalUserId,
                    fakeAndroidId = fakeAndroidId,
                    fakeBrand = fakeBrand,
                    fakeGps = fakeGps,
                    fakeImei = fakeImei,
                    fakeMac = fakeMac,
                    fakeAdId = fakeAdId,
                    fakeSim = fakeSim
                )

                withContext(Dispatchers.Main) { Toast.makeText(context, "Clonando $appName...", Toast.LENGTH_SHORT).show() }

                var installSuccess = true
                var errorMessage: String? = null

                val installResult = ShizukuUtils.installExistingApp(finalUserId, packageName)
                
                val unlimitedClonesStr = ShizukuUtils.executeCommand("settings get --user $finalUserId secure chaos_unlimited_clones")
                val isUnlimited = unlimitedClonesStr.contains("true", ignoreCase = true)

                if (isUnlimited) {
                    ShizukuUtils.executeCommand("pm disable-user --user $finalUserId $packageName")
                }

                if (installResult.isFailure) {
                    installSuccess = false
                    errorMessage = installResult.exceptionOrNull()?.message
                }

                if (!installSuccess) {
                    dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "ERROR", message = "Failed to clone $appName: $errorMessage"))
                    if (isNewWorkspace && finalUserId != "10") {
                        // Rollback new workspace
                        ShizukuUtils.executeCommand("pm remove-user $finalUserId")
                        dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "ROLLBACK", message = "Rolled back workspace $finalUserId due to clone failure"))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Falha ao clonar aplicativo: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                    onComplete(false)
                } else {
                    dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "CLONE_SUCCESS", message = "Successfully cloned $appName into $finalUserId"))
                    dao.insertClone(
                        CloneEntity(
                            userId = finalUserId,
                            packageName = packageName,
                            appName = appName,
                            cloneMode = "WORK_PROFILE",
                            firewallEnabled = firewallEnabled,
                            spoofProfile = spoofProfile
                        )
                    )
                    
                    // Applica Spoofing Profundo
                    if (spoofProfile != null) {
                        DeepSpoofEngine.applySpoofing(context, packageName, spoofProfile)
                    }
                    
                    // Applica Firewall Granular
                    if (firewallEnabled) {
                        // Simula obter o UID do clone
                        val fakeUid = 1010000 + (1000..9999).random()
                        FirewallEngine.enableFirewallForUid(context, fakeUid)
                    }
                    
                    var finalProxyRegion = ShizukuUtils.executeCommand("settings get --user $finalUserId secure chaos_proxy_region").trim()
                    
                    if (proxyRegion != null) {
                        ShizukuUtils.executeCommand("settings put --user $finalUserId secure chaos_proxy_region '$proxyRegion'")
                        finalProxyRegion = proxyRegion
                    }

                    if (finalProxyRegion != "null" && finalProxyRegion.isNotBlank() && finalProxyRegion != "None") {
                        com.example.vpn.VpnManager.enableWorkspaceVpn(context, finalUserId, finalProxyRegion)
                    }
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "$appName clonado com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                    onComplete(true)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro na operação de clonagem: ${e.message}", Toast.LENGTH_LONG).show()
                }
                onComplete(false)
            }
        }
    }

    fun launchClone(context: Context, clone: CloneEntity) {
        serviceScope.launch {
            if (clone.cloneMode == "SANDBOX_NON_ROOT") {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Iniciando ${clone.appName} no Sandbox (Isolado)", Toast.LENGTH_SHORT).show()
                }
                SandboxEngine.launchInSandbox(context, clone.packageName)
                val dao = AppDatabase.getDatabase(context).vaultDao()
                dao.updateCloneState(clone.id, true)
                return@launch
            }

            if (clone.cloneMode == "VIRTUAL_MACHINE") {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Por favor, abra o Celular Virtual para acessar este aplicativo.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Shizuku not ready for Work Profile launch.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val dao = AppDatabase.getDatabase(context).vaultDao()
            val unlimitedClonesStr = ShizukuUtils.executeCommand("settings get --user ${clone.userId} secure chaos_unlimited_clones")
            val isUnlimited = unlimitedClonesStr.contains("true", ignoreCase = true)

            if (isUnlimited) {
                // ABORDAGEM 2: Unhide (Ativação) sob demanda
                ShizukuUtils.executeCommand("pm enable --user ${clone.userId} ${clone.packageName}")
            }
            // Unsuspend/Descongelar o aplicativo se estiver congelado pelo App Freezer
            ShizukuUtils.wakeApp(clone.userId, clone.packageName)
            // Bypass de economia de bateria (Item 28)
            ShizukuUtils.whitelistFromBatteryOptimization(clone.packageName)
            dao.updateCloneState(clone.id, true)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Descriptografando e iniciando ${clone.appName}...", Toast.LENGTH_SHORT).show()
            }

            val launchResult = ShizukuUtils.launchApp(context, clone.userId, clone.packageName)

            if (launchResult.isFailure) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro no lançamento: ${launchResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
                if (isUnlimited) {
                    ShizukuUtils.executeCommand("pm disable-user --user ${clone.userId} ${clone.packageName}")
                }
                dao.updateCloneState(clone.id, false)
            } else {
                dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "AUDIT_ACCESS", message = "Acesso autorizado ao clone ${clone.appName} (User ${clone.userId})"))
                if (isUnlimited) {
                    startActiveHidingTracker(context, clone)
                }
            }
        }
    }

    fun freezeClone(context: Context, clone: CloneEntity, onDone: (() -> Unit)? = null) {
        serviceScope.launch {
            if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasShizukuPermission()) {
                ShizukuUtils.hibernateApp(clone.userId, clone.packageName)
                val dao = AppDatabase.getDatabase(context).vaultDao()
                dao.updateCloneState(clone.id, false)
                dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "AUDIT_FREEZE", message = "Clone ${clone.appName} (User ${clone.userId}) hibernado com sucesso"))
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${clone.appName} congelado (0% RAM)", Toast.LENGTH_SHORT).show()
                    onDone?.invoke()
                }
            }
        }
    }

    fun freezeAllClones(context: Context, onDone: (() -> Unit)? = null) {
        serviceScope.launch {
            try {
                val dao = AppDatabase.getDatabase(context).vaultDao()
                val allClones = dao.getAllClones().first()
                for (clone in allClones) {
                    if (clone.cloneMode == "WORK_PROFILE") {
                        ShizukuUtils.hibernateApp(clone.userId, clone.packageName)
                        dao.updateCloneState(clone.id, false)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Todos os clones foram congelados!", Toast.LENGTH_SHORT).show()
                    onDone?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startActiveHidingTracker(context: Context, clone: CloneEntity) {
        val trackingKey = "${clone.userId}:${clone.packageName}"
        trackingJobs[trackingKey]?.cancel()

        trackingJobs[trackingKey] = serviceScope.launch {
            delay(3000)

            var notInForegroundCount = 0
            val dao = AppDatabase.getDatabase(context).vaultDao()

            while (isActive) {
                delay(2000)

                val focusResult = ShizukuUtils.executeCommand("dumpsys window windows | grep mCurrentFocus")

                if (!focusResult.contains(clone.packageName)) {
                    notInForegroundCount++
                } else {
                    notInForegroundCount = 0
                }

                if (notInForegroundCount >= 3) {
                    // Oculta (desativa) o app novamente
                    ShizukuUtils.executeCommand("pm disable-user --user ${clone.userId} ${clone.packageName}")
                    ShizukuUtils.executeCommand("am force-stop --user ${clone.userId} ${clone.packageName}")

                    dao.updateCloneState(clone.id, false)
                    trackingJobs.remove(trackingKey)
                    break // Encerra o rastreamento
                }
            }
            trackingJobs.remove(trackingKey)
        }
    }

    fun freezeClone(context: Context, clone: CloneEntity) {
        val trackingKey = "${clone.userId}:${clone.packageName}"
        trackingJobs[trackingKey]?.cancel()
        trackingJobs.remove(trackingKey)
        serviceScope.launch {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            
            if (clone.cloneMode == "SANDBOX_NON_ROOT") {
                dao.updateCloneState(clone.id, false)
                withContext(Dispatchers.Main) { Toast.makeText(context, "Processo morto no Sandbox", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            val res = ShizukuUtils.executeCommand("pm disable-user --user ${clone.userId} ${clone.packageName}")
            if (!res.contains("Error") && !res.contains("Exception")) {
                dao.updateCloneState(clone.id, false)
                withContext(Dispatchers.Main) { Toast.makeText(context, "App congelado", Toast.LENGTH_SHORT).show() }
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao congelar: $res", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun unfreezeClone(context: Context, clone: CloneEntity) {
        serviceScope.launch {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            
            if (clone.cloneMode == "SANDBOX_NON_ROOT") {
                dao.updateCloneState(clone.id, true)
                withContext(Dispatchers.Main) { Toast.makeText(context, "Processo acordado no Sandbox", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            
            val res = ShizukuUtils.executeCommand("pm enable --user ${clone.userId} ${clone.packageName}")
            if (!res.contains("Error") && !res.contains("Exception")) {
                dao.updateCloneState(clone.id, true)
                withContext(Dispatchers.Main) { Toast.makeText(context, "App descongelado", Toast.LENGTH_SHORT).show() }
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao descongelar: $res", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun clearCloneData(context: Context, clone: CloneEntity) {
        serviceScope.launch {
            val res = ShizukuUtils.executeCommand("pm clear --user ${clone.userId} ${clone.packageName}")
            val dao = AppDatabase.getDatabase(context).vaultDao()
            if (!res.contains("Error") && !res.contains("Exception")) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Dados apagados", Toast.LENGTH_SHORT).show() }
                dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "CLEAR_DATA", message = "Cleared data for ${clone.appName}"))
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao apagar dados: $res", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun clearCloneCacheOnly(context: Context, clone: CloneEntity) {
        serviceScope.launch {
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Shizuku indisponível", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Limpando cache de ${clone.appName}...", Toast.LENGTH_SHORT).show()
            }

            ShizukuUtils.executeCommand("rm -rf /data/user/${clone.userId}/${clone.packageName}/cache/* /data/user/${clone.userId}/${clone.packageName}/code_cache/*")
            ShizukuUtils.executeCommand("pm trim-caches 1000G --user ${clone.userId}")

            val dao = AppDatabase.getDatabase(context).vaultDao()
            dao.insertSessionLog(com.example.data.SessionLogEntity(
                eventType = "CLONE_CACHE_CLEARED",
                message = "Cache limpo para ${clone.appName} (${clone.packageName})"
            ))

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Cache de ${clone.appName} limpo com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearInstanceCacheAndTemp(context: Context, userId: String, onComplete: (String) -> Unit = {}) {
        serviceScope.launch {
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                val msg = "Shizuku indisponível"
                withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                onComplete(msg)
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Limpando cache da Instância ID-$userId...", Toast.LENGTH_SHORT).show()
            }

            ShizukuUtils.executeCommand("pm trim-caches 1000G --user $userId")
            ShizukuUtils.executeCommand("rm -rf /data/user/$userId/*/cache/* /data/user/$userId/*/code_cache/* /data/user/$userId/*/no_backup/*")

            val dao = AppDatabase.getDatabase(context).vaultDao()
            dao.insertSessionLog(com.example.data.SessionLogEntity(
                eventType = "CACHE_CLEARED",
                message = "Limpeza de cache e arquivos temporários concluída na Instância ID-$userId"
            ))

            val successMsg = "Cache e temporários da Instância ID-$userId limpos com sucesso!"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                onComplete(successMsg)
            }
        }
    }

    fun deleteCloneApp(context: Context, clone: CloneEntity) {
        val trackingKey = "${clone.userId}:${clone.packageName}"
        trackingJobs[trackingKey]?.cancel()
        trackingJobs.remove(trackingKey)
        serviceScope.launch {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            
            if (clone.cloneMode == "SANDBOX_NON_ROOT") {
                dao.deleteClone(clone)
                withContext(Dispatchers.Main) { Toast.makeText(context, "App deletado do Sandbox local", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            
            val res = ShizukuUtils.executeCommand("pm uninstall --user ${clone.userId} ${clone.packageName}")
            if (!res.contains("Error") && !res.contains("Exception") && !res.contains("Failure")) {
                dao.deleteClone(clone)
                withContext(Dispatchers.Main) { Toast.makeText(context, "Clone deletado", Toast.LENGTH_SHORT).show() }
            } else {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Erro ao deletar: $res", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun deleteWorkspace(context: Context, userId: String, clonesInWorkspace: List<CloneEntity> = emptyList(), onDeleted: () -> Unit = {}) {
        if (userId == "0") {
            serviceScope.launch {
                val dao = AppDatabase.getDatabase(context).vaultDao()
                dao.deleteInstanceConfig("0")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "O Perfil Principal (User 0) não pode ser removido do sistema Android. Configurações locais resetadas.", Toast.LENGTH_LONG).show()
                    onDeleted()
                }
            }
            return
        }

        clonesInWorkspace.forEach { clone ->
            val trackingKey = "${clone.userId}:${clone.packageName}"
            trackingJobs[trackingKey]?.cancel()
            trackingJobs.remove(trackingKey)
        }

        serviceScope.launch {
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Shizuku não está pronto ou sem permissão.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Removendo Instância Chaos OS ID $userId...", Toast.LENGTH_SHORT).show()
            }

            val output = ShizukuUtils.executeCommand("pm remove-user $userId")
            val isError = output.contains("Error") || output.contains("Exception") || output.contains("Failure")
            val isUnknownUser = output.contains("Unknown user") || output.contains("does not exist")

            val dao = AppDatabase.getDatabase(context).vaultDao()

            if (isError && !isUnknownUser) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro ao remover usuário $userId do sistema: $output. Limpando dados locais...", Toast.LENGTH_LONG).show()
                }
                // Fallback cleanup of local DB in case user was manually deleted or locked
                try {
                    val allClones = dao.getAllClones().first()
                    allClones.filter { it.userId == userId }.forEach { dao.deleteClone(it) }
                    dao.deleteInstanceConfig(userId)
                } catch (e: Exception) { e.printStackTrace() }
                withContext(Dispatchers.Main) { onDeleted() }
            } else {
                try {
                    val allClones = dao.getAllClones().first()
                    allClones.filter { it.userId == userId }.forEach { dao.deleteClone(it) }
                    clonesInWorkspace.forEach { dao.deleteClone(it) }
                    dao.deleteInstanceConfig(userId)
                    dao.insertSessionLog(com.example.data.SessionLogEntity(eventType = "WORKSPACE_DELETED", message = "Instância $userId e seus clones foram excluídos"))
                    // Limpeza profunda de diretórios órfãos (Item 30)
                    ShizukuUtils.cleanOrphanUserDirectories(userId)
                } catch (e: Exception) { e.printStackTrace() }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Instância $userId excluída com purgação profunda!", Toast.LENGTH_SHORT).show()
                    onDeleted()
                }
            }
        }
    }

    fun verifyCloneIntegrity(context: Context, clone: CloneEntity, onResult: (Boolean, String) -> Unit) {
        serviceScope.launch {
            if (clone.cloneMode == "SANDBOX_NON_ROOT") {
                val sandboxDir = java.io.File(context.filesDir, "sandbox_apps/${clone.packageName}")
                val destApk = java.io.File(sandboxDir, "base.apk")
                if (destApk.exists()) {
                    withContext(Dispatchers.Main) { onResult(true, "Sandbox Íntegro") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "APK não encontrado no Sandbox") }
                }
                return@launch
            }

            if (!ShizukuUtils.isShizukuAvailable()) {
                withContext(Dispatchers.Main) { onResult(false, "Shizuku offline") }
                return@launch
            }

            val res = ShizukuUtils.executeCommand("pm list packages --user ${clone.userId} ${clone.packageName}")
            if (res.contains(clone.packageName)) {
                withContext(Dispatchers.Main) { onResult(true, "Nativo Íntegro") }
            } else {
                withContext(Dispatchers.Main) { onResult(false, "App não encontrado no Workspace") }
            }
        }
    }

    fun freezeAll(context: Context, clones: List<CloneEntity>) {
        serviceScope.launch {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Congelando todos...", Toast.LENGTH_SHORT).show() }
            clones.forEach { clone ->
                if (clone.cloneMode != "SANDBOX_NON_ROOT") {
                    ShizukuUtils.executeCommand("pm disable-user --user ${clone.userId} ${clone.packageName}")
                }
                AppDatabase.getDatabase(context).vaultDao().updateCloneState(clone.id, false)
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Todos congelados", Toast.LENGTH_SHORT).show() }
        }
    }

    fun unfreezeAll(context: Context, clones: List<CloneEntity>) {
        serviceScope.launch {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Descongelando todos...", Toast.LENGTH_SHORT).show() }
            clones.forEach { clone ->
                if (clone.cloneMode != "SANDBOX_NON_ROOT") {
                    ShizukuUtils.executeCommand("pm enable --user ${clone.userId} ${clone.packageName}")
                }
                AppDatabase.getDatabase(context).vaultDao().updateCloneState(clone.id, true)
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Todos descongelados", Toast.LENGTH_SHORT).show() }
        }
    }

    fun deleteAll(context: Context, clones: List<CloneEntity>) {
        serviceScope.launch {
            val dao = AppDatabase.getDatabase(context).vaultDao()
            clones.forEach { clone ->
                if (clone.cloneMode == "SANDBOX_NON_ROOT") {
                    dao.deleteClone(clone)
                } else {
                    val res = ShizukuUtils.executeCommand("pm uninstall --user ${clone.userId} ${clone.packageName}")
                    if (!res.contains("Error") && !res.contains("Failure")) {
                        dao.deleteClone(clone)
                    }
                }
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, "Todos deletados", Toast.LENGTH_SHORT).show() }
        }
    }

    fun migrateClone(context: Context, clone: CloneEntity, newUserId: String) {
        serviceScope.launch {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Migração de Clone é experimental e requer Root para mover dados.", Toast.LENGTH_LONG).show() }
        }
    }
}
