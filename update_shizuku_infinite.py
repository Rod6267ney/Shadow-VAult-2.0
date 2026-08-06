import re

with open("app/src/main/java/com/example/utils/ShizukuUtils.kt", "r") as f:
    content = f.read()

replacement = """
    suspend fun createWorkProfile(profileName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable() || !hasShizukuPermission()) {
            val msg = "Shizuku is not connected or lacks permission."
            onLogEvent?.invoke("ERROR", msg)
            return@withContext Result.failure(Exception(msg))
        }

        try {
            // ==========================================
            // BRUTE FORCE ENGINE: BYPASS USER LIMITS
            // ==========================================
            // 1. Tentar forçar o aumento do limite global de usuários via sysprops
            executeCommand("setprop fw.max_users 999")
            executeCommand("setprop persist.sys.max_profiles 999")
            executeCommand("setprop fw.show_multiuserui 1")
            
            // 2. Tentar alterar limites via settings (dispositivos específicos como Xiaomi/Samsung)
            executeCommand("settings put global multi_user_enabled 1")
            executeCommand("settings put global maximum_user_count 999")

            var output = ""
            var userId: String? = null
            
            // LISTA ESTRATÉGICA DE TIPOS DE PERFIS (Do mais isolado/Work para o menos)
            val profileStrategies = listOf(
                "pm create-user --profileOf 0 --managed $profileName",
                "pm create-user --profileOf 0 --user-type android.os.usertype.profile.MANAGED $profileName",
                "pm create-user --profileOf 0 --user-type android.os.usertype.profile.CLONE $profileName",
                "pm create-user --profileOf 0 --user-type android.os.usertype.profile.PRIVATE $profileName",
                "pm create-user $profileName",
                "pm create-user --user-type android.os.usertype.full.RESTRICTED $profileName",
                "pm create-user --user-type android.os.usertype.full.SECONDARY $profileName"
            )

            for (cmd in profileStrategies) {
                output = executeCommand(cmd, 30000L)
                if (!output.contains("Error") && !output.contains("Exception") && !output.contains("Failure") && !output.contains("Maximum")) {
                    val matchResult = Regex("id (\\\\d+)").find(output)
                    userId = matchResult?.groupValues?.get(1)
                    if (userId != null) {
                        onLogEvent?.invoke("INFO", "Sucesso com estratégia: $cmd")
                        break
                    }
                }
            }
            
            // 3. ESTRATÉGIA DE NESTING (ANINHAMENTO) PARA BURLAR LIMITE HARDCODED
            // Se falhamos em criar no perfil 0, criamos um usuário oculto fantasma e criamos o Work Profile DENTRO dele.
            if (userId == null) {
                onLogEvent?.invoke("WARNING", "Limite do Perfil 0 atingido. Tentando Sandbox Aninhado (Nesting)...")
                val ghostUserOutput = executeCommand("pm create-user GhostSpace_${System.currentTimeMillis() % 100}", 30000L)
                val ghostMatch = Regex("id (\\\\d+)").find(ghostUserOutput)
                val ghostId = ghostMatch?.groupValues?.get(1)
                
                if (ghostId != null) {
                    // Iniciar o Ghost User em background
                    executeCommand("am start-user -b $ghostId")
                    
                    // Tentar criar o perfil gerenciado atrelado ao Ghost User
                    val nestedOutput = executeCommand("pm create-user --profileOf $ghostId --managed $profileName", 30000L)
                    val nestedMatch = Regex("id (\\\\d+)").find(nestedOutput)
                    userId = nestedMatch?.groupValues?.get(1)
                    
                    if (userId != null) {
                        onLogEvent?.invoke("INFO", "Sucesso no Bypass via Aninhamento! Ghost=$ghostId, Work=$userId")
                    } else {
                        // Se falhar o nested, usamos o Ghost Profile como o próprio clone container
                        userId = ghostId
                    }
                }
            }

            if (userId == null) {
                // Última checagem: Conflito de ID ou reciclagem de profile
                val usersOutput = executeCommand("pm list users")
                val regexExisting = Regex("UserInfo\\\\{(\\\\d+):$profileName")
                val match = regexExisting.find(usersOutput)
                if (match != null) {
                   userId = match.groupValues[1]
                   onLogEvent?.invoke("PROFILE_CREATED", "Profile reciclado (Já Existente): $userId ($profileName)")
                   return@withContext Result.success(userId)
                }

                val msg = "Falha Catastrófica: Limite Absoluto do Sistema Atingido. Output Final: $output"
                onLogEvent?.invoke("ERROR", msg)
                return@withContext Result.failure(Exception(msg))
            }

            // Inicializar o ambiente do novo profile
            executeCommand("am start-user -b $userId")
            executeCommand("am start-user $userId")
            
            // Forçar configurações para evitar que o sistema mate o perfil
            executeCommand("settings put --user $userId secure user_setup_complete 1")
            executeCommand("settings put --user $userId global hidden_api_policy 1")
            
            onLogEvent?.invoke("PROFILE_CREATED", "Super-Profile criado e ativado com ID $userId ($profileName)")
            return@withContext Result.success(userId)

        } catch (t: Throwable) {
            val msg = "System error: ${t.message}"
            onLogEvent?.invoke("ERROR", msg)
            return@withContext Result.failure(Exception(msg))
        }
    }
"""

content = re.sub(r'suspend fun createWorkProfile.*?Result\.failure\(Exception\(msg\)\)\s*\}', replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/utils/ShizukuUtils.kt", "w") as f:
    f.write(content)
