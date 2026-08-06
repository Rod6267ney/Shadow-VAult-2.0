package com.example.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class CamouflageMode {
    DEFAULT, CALCULATOR, NOTES
}

object CamouflageManager {

    fun setCamouflage(context: Context, mode: CamouflageMode) {
        val pm = context.packageManager
        val packageName = context.packageName

        val mainAlias = ComponentName(context, "$packageName.MainActivityAlias")
        val calcAlias = ComponentName(context, "$packageName.AliasCalculator")
        val notesAlias = ComponentName(context, "$packageName.AliasNotes")

        fun setComponentState(component: ComponentName, enabled: Boolean) {
            pm.setComponentEnabledSetting(
                component,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        when (mode) {
            CamouflageMode.DEFAULT -> {
                setComponentState(mainAlias, true)
                setComponentState(calcAlias, false)
                setComponentState(notesAlias, false)
            }
            CamouflageMode.CALCULATOR -> {
                setComponentState(calcAlias, true)
                setComponentState(mainAlias, false)
                setComponentState(notesAlias, false)
            }
            CamouflageMode.NOTES -> {
                setComponentState(notesAlias, true)
                setComponentState(mainAlias, false)
                setComponentState(calcAlias, false)
            }
        }
    }
}
