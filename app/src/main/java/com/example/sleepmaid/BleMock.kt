
package com.example.sleepmaid

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// UUID віртуального BLE-профілю
object BleProfile {
    const val SERVICE_UUID =
        "8f4e1000-04e0-4d80-aad0-b45685b5ad03"

    const val STATUS_UUID =
        "8f4e1001-04e0-4d80-aad0-b45685b5ad03"

    const val DATA_UUID =
        "8f4e1002-04e0-4d80-aad0-b45685b5ad03"

    const val NOTIFY_UUID =
        "8f4e1003-04e0-4d80-aad0-b45685b5ad03"
}

// Модель віртуального пристрою
data class BleDevice(
    val name: String,
    val id: String
)

// Абстракція BLE-з'єднання
interface IBleConnector {
    val notifications: SharedFlow<String>

    suspend fun connect(): Boolean
    fun disconnect()
    suspend fun sendData(data: String): Boolean
    suspend fun readStatus(): String
}

// Імітація пошуку BLE-пристроїв
class MockBleScanner {

    suspend fun scan(): List<BleDevice> {
        // Імітуємо сканування протягом 2 секунд
        delay(2000)

        return listOf(
            BleDevice(
                name = "Sleep Hub Simulator",
                id = "MOCK-SLEEP-HUB-01"
            )
        )
    }
}

// Імітація підключення та обміну даними
class MockBleConnector : IBleConnector {

    private var isConnected = false

    private val _notifications =
        MutableSharedFlow<String>(extraBufferCapacity = 8)

    override val notifications: SharedFlow<String> =
        _notifications.asSharedFlow()

    override suspend fun connect(): Boolean {
        // Імітуємо затримку підключення
        delay(600)

        isConnected = true
        return true
    }

    override fun disconnect() {
        isConnected = false
    }

    // Імітація BLE Write
    override suspend fun sendData(data: String): Boolean {
        if (!isConnected) return false

        delay(300)

        if (!isConnected) return false

        // Імітація підтвердження через Notify
        _notifications.emit(
            """{"status":"received","device":"Sleep Hub"}"""
        )

        return true
    }

    // Імітація BLE Read
    override suspend fun readStatus(): String {
        delay(100)

        return if (isConnected) {
            """{"connected":true,"device":"Sleep Hub"}"""
        } else {
            """{"connected":false}"""
        }
    }
}

// Стани взаємодії з пристроєм
enum class BlePhase {
    DISCONNECTED,
    SCANNING,
    FOUND,
    CONNECTING,
    CONNECTED,
    TRANSFERRING
}

// Дані для відображення в інтерфейсі
data class BleUiState(
    val phase: BlePhase = BlePhase.DISCONNECTED,
    val devices: List<BleDevice> = emptyList(),
    val logs: List<String> = emptyList()
)
