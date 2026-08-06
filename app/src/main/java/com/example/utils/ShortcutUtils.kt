package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.data.CloneEntity
import android.widget.Toast

object ShortcutUtils {

    fun createWorkspaceShortcut(context: Context, workspaceId: String, workspaceName: String) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("shortcut_workspace_id", workspaceId)
            }
            val shortcutInfo = ShortcutInfoCompat.Builder(context, "workspace_$workspaceId")
                .setShortLabel(workspaceName)
                .setLongLabel("Abrir Cofre $workspaceName")
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon))
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            Toast.makeText(context, "Atalho solicitado para $workspaceName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Atalhos não suportados neste launcher", Toast.LENGTH_SHORT).show()
        }
    }

    fun createCloneShortcut(context: Context, clone: CloneEntity) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("LAUNCH_CLONE_USER_ID", clone.userId)
                putExtra("LAUNCH_CLONE_PACKAGE", clone.packageName)
            }

            val shortcutInfo = ShortcutInfoCompat.Builder(context, "clone_${clone.userId}_${clone.packageName}")
                .setShortLabel(clone.appName)
                .setLongLabel("Launch ${clone.appName}")
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon))
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            Toast.makeText(context, "Atalho solicitado para ${clone.appName}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Atalhos não suportados neste launcher", Toast.LENGTH_SHORT).show()
        }
    }
}
