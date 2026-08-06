package com.example.data

data class WorkspaceConfig(
    val id: String,
    val name: String,
    val status: String,
    val fakeName: String,
    val fakeEmail: String,
    val fakePhone: String = "",
    val fakeCompany: String = "",
    val fakeDeviceBrand: String = "Google",
    val fakeDeviceModel: String = "Pixel 8 Pro",
    val fakeDeviceManufacturer: String = "Google",
    val fakeDeviceSdk: String = "34",
    val fakeDeviceAndroidId: String = "4f8a9e2d7c5b1b3a",
    val storageText: String = "",
    val proxyRegion: String = "None",
    val proxyIp: String = "Oculto",
    val iconName: String = "Domain",
    val unlimitedClones: Boolean = false
)
