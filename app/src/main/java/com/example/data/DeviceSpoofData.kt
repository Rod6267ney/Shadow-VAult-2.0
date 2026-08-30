package com.example.data

import java.util.UUID
import kotlin.random.Random

data class DeviceSpoofData(
    val brand: String = "Samsung",
    val model: String = "Galaxy S24 Ultra",
    val manufacturer: String = "samsung",
    val device: String = "e3q",
    val product: String = "e3qsq",
    val board: String = "erd9945",
    val imei: String = generateRandomIMEI(),
    val macAddress: String = generateRandomMAC(),
    val androidId: String = generateRandomAndroidId()
) {
    companion object {
        fun generateRandomIMEI(): String {
            val prefix = "35" // Common prefix for IMEI
            val randomDigits = (1..13).joinToString("") { Random.nextInt(0, 10).toString() }
            return prefix + randomDigits // For simulation, this is enough. Real IMEIs need Luhn algorithm.
        }

        fun generateRandomMAC(): String {
            return (1..6).joinToString(":") {
                Random.nextInt(0, 256).toString(16).padStart(2, '0')
            }
        }

        fun generateRandomAndroidId(): String {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        }
        
        val PREDEFINED_MODELS = listOf(
            DeviceSpoofData(
                brand = "Samsung", model = "Galaxy S24 Ultra", manufacturer = "samsung", 
                device = "e3q", product = "e3qsq", board = "erd9945"
            ),
            DeviceSpoofData(
                brand = "Google", model = "Pixel 8 Pro", manufacturer = "Google", 
                device = "husky", product = "husky", board = "husky"
            ),
            DeviceSpoofData(
                brand = "Xiaomi", model = "14 Pro", manufacturer = "Xiaomi", 
                device = "shennong", product = "shennong", board = "shennong"
            )
        )
    }
}
