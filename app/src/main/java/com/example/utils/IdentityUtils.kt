package com.example.utils

import android.content.Context
import com.example.ai.AiIdentityGenerator
import com.example.data.AppDatabase
import com.example.data.IdentityEntity
import com.example.data.InstanceConfigEntity
import org.json.JSONObject

object IdentityUtils {
    suspend fun generateAndAttachIdentityToWorkspace(
        context: Context,
        workspaceId: String,
        workspaceName: String,
        prompt: String = "Identidade sintética anônima para o workspace $workspaceName"
    ): InstanceConfigEntity {
        val generator = AiIdentityGenerator(context)
        val jsonStr = generator.generateIdentity(prompt)
        val cleanJsonStr = jsonStr.replace("```json", "").replace("```", "").trim()
        
        var fName = "Persona Anônima"
        var fJob = "Consultor Indoc"
        var fLoc = "São Paulo, BR"
        var fEmail = "shadow.persona@vault.sec"
        var fAddress = "Av. Paulista, 1000"
        var fDob = "1990-01-01"
        
        var fBio = "Sem biografia"
        var fAvatarSeed = "default"
        var fPasswords = listOf<String>()
        
        try {
            val jsonObj = JSONObject(cleanJsonStr)
            fName = jsonObj.optString("fakeName", fName)
            fJob = jsonObj.optString("jobTitle", fJob)
            fLoc = jsonObj.optString("location", fLoc)
            fEmail = jsonObj.optString("email", fEmail)
            fAddress = jsonObj.optString("address", fAddress)
            fDob = jsonObj.optString("dob", fDob)
            fBio = jsonObj.optString("bio", fBio)
            fAvatarSeed = jsonObj.optString("avatarSeed", fAvatarSeed)
            val passArray = jsonObj.optJSONArray("passwords")
            if (passArray != null) {
                val list = mutableListOf<String>()
                for (i in 0 until passArray.length()) {
                    list.add(passArray.optString(i))
                }
                fPasswords = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val dao = AppDatabase.getDatabase(context).vaultDao()
        
        // 1. Create Identity entity
        val identity = IdentityEntity(
            fakeName = fName,
            jobTitle = fJob,
            location = fLoc,
            profileIdea = prompt,
            dob = fDob,
            address = fAddress,
            email = fEmail
        )
        
        // 2. Fetch existing instance config or construct default
        val existingConfig = dao.getInstanceConfig(workspaceId)
        val initialConfig = InstanceConfigEntity(
            workspaceId = workspaceId,
            workspaceName = workspaceName,
            fakeName = fName,
            fakeEmail = fEmail,
            fakePhone = "+55 11 9${(10000000..99999999).random()}",
            fakeCompany = "$fJob • $fLoc",
            vpnEnabled = existingConfig?.vpnEnabled ?: false,
            proxyRegion = existingConfig?.proxyRegion ?: "None",
            iconName = existingConfig?.iconName ?: "Domain",
            unlimitedClones = existingConfig?.unlimitedClones ?: false,
            identityId = identity.id,
            lastUpdated = System.currentTimeMillis()
        )

        // 3. Persist atomically as a single Room transaction linked via foreign key
        val newConfig = dao.createWorkspaceAndIdentityTransaction(identity, initialConfig)
        
        // 3. Apply to Shizuku secure settings if applicable (Batched for performance)
        if (!workspaceId.startsWith("v_")) {
            try {
                val batchCmd = """
                    settings put --user $workspaceId secure fake_identity_name '$fName'
                    settings put --user $workspaceId secure fake_identity_email '$fEmail'
                    settings put --user $workspaceId secure fake_identity_company '$fJob • $fLoc'
                """.trimIndent()
                ShizukuUtils.executeCommand(batchCmd)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return newConfig
    }
}
