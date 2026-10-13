
package com.example.sleepmaid

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class BleViewModel : ViewModel() {

    private val scanner = MockBleScanner()
    private val connector: IBleConnector = MockBleConnector()

    private val _state = MutableStateFlow(BleUiState())
    val state: StateFlow<BleUiState> = _state.asStateFlow()

    init {
        // Отримання Notify-повідомлень
        viewModelScope.launch {
            connector.notifications.collect { message ->
                addLog("NOTIFY [${BleProfile.NOTIFY_UUID}]: $message")
            }
        }
    }

    private fun addLog(message: String) {
        Log.d("SleepBLE", message)

        _state.value = _state.value.copy(
            logs = (_state.value.logs + message).takeLast(50)
        )
    }

    fun scan() {
        if (_state.value.phase !in listOf(
                BlePhase.DISCONNECTED,
                BlePhase.FOUND
            )
        ) return

        _state.value = _state.value.copy(
            phase = BlePhase.SCANNING,
            devices = emptyList()
        )

        addLog("SCAN: Початок пошуку пристроїв")

        viewModelScope.launch {
            val devices = scanner.scan()

            _state.value = _state.value.copy(
                phase = BlePhase.FOUND,
                devices = devices
            )

            devices.forEach { device ->
                addLog("FOUND: ${device.name} (${device.id})")
            }
        }
    }

    fun connect(device: BleDevice) {
        if (_state.value.phase !in listOf(
                BlePhase.FOUND,
                BlePhase.DISCONNECTED
            )
        ) return

        if (device !in _state.value.devices) return

        _state.value = _state.value.copy(
            phase = BlePhase.CONNECTING
        )

        addLog("CONNECTING: ${device.name}")

        viewModelScope.launch {
            val success = connector.connect()

            if (success) {
                _state.value = _state.value.copy(
                    phase = BlePhase.CONNECTED
                )

                addLog("CONNECTED: ${device.name}")

                // Імітація читання характеристики
                val status = connector.readStatus()
                addLog("READ [${BleProfile.STATUS_UUID}]: $status")
            } else {
                _state.value = _state.value.copy(
                    phase = BlePhase.DISCONNECTED
                )

                addLog("ERROR: Не вдалося підключитися")
            }
        }
    }

    fun disconnect() {
        if (_state.value.phase != BlePhase.CONNECTED) return

        connector.disconnect()

        _state.value = _state.value.copy(
            phase = BlePhase.DISCONNECTED
        )

        addLog("DISCONNECTED: З'єднання завершено")
    }

    fun sendSensorData(x: Float, y: Float, z: Float) {
        if (_state.value.phase != BlePhase.CONNECTED) return

        // Формування пакета сенсорних даних
        val data = JSONObject()
            .put("type", "accelerometer")
            .put("x", x.toDouble())
            .put("y", y.toDouble())
            .put("z", z.toDouble())
            .toString()

        _state.value = _state.value.copy(
            phase = BlePhase.TRANSFERRING
        )

        viewModelScope.launch {
            addLog("WRITE [${BleProfile.DATA_UUID}]: $data")

            val success = connector.sendData(data)

            if (success) {
                addLog("WRITE SUCCESS: Пакет передано")
            } else {
                addLog("ERROR: Помилка передачі")
            }

            _state.value = _state.value.copy(
                phase = BlePhase.CONNECTED
            )
        }
    }
}
