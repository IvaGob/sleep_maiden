package com.example.sleepmaid

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CloudStatus {
    WAITING,
    SYNCING,
    SYNCED,
    PENDING,
    ERROR
}

data class CloudUiState(
    val status: CloudStatus = CloudStatus.WAITING,
    val message: String = "Очікування даних"
)

class CloudSyncViewModel : ViewModel() {

    private val repository = SensorRepository()

    private val _uiState = MutableStateFlow(CloudUiState())
    val uiState = _uiState.asStateFlow()

    private var latestReading: SensorData? = null
    private var pendingReading: SensorData? = null
    private var syncJob: Job? = null

    init {
        // Спостереження за станом з'єднання Firebase
        viewModelScope.launch {
            repository.connected.collect { connected ->
                if (!connected && latestReading != null) {
                    _uiState.value = CloudUiState(
                        CloudStatus.PENDING,
                        "Pending: немає зв'язку з Firebase"
                    )
                }
            }
        }
    }

    // Отримання показників від MainActivity
    fun updateReading(
        x: Float,
        y: Float,
        z: Float,
        motion: Float
    ) {
        latestReading = SensorData(
            x = x,
            y = y,
            z = z,
            value = motion
        )
    }

    // Запуск синхронізації з інтервалом 5 секунд
    fun startSync() {
        if (syncJob?.isActive == true) return

        syncJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)

                val latest = latestReading ?: continue

                // Якщо Firebase недоступний,
                // зберігаємо останнє вимірювання як Pending
                if (!repository.connected.value) {
                    pendingReading = latest

                    _uiState.value = CloudUiState(
                        CloudStatus.PENDING,
                        "Pending: немає зв'язку з Firebase"
                    )

                    Log.w(
                        "SleepCloud",
                        "PENDING: очікування з'єднання"
                    )
                    continue
                }

                // Після відновлення зв'язку спочатку
                // відправляємо відкладене вимірювання
                val data = pendingReading ?: latest

                _uiState.value = CloudUiState(
                    CloudStatus.SYNCING,
                    "Відправлення даних..."
                )

                try {
                    repository.upload(data)

                    if (pendingReading === data) {
                        pendingReading = null
                    }

                    _uiState.value = CloudUiState(
                        CloudStatus.SYNCED,
                        "Успішно синхронізовано"
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    pendingReading = data

                    _uiState.value = CloudUiState(
                        CloudStatus.ERROR,
                        "Помилка: ${e.localizedMessage}"
                    )

                    Log.e(
                        "SleepCloud",
                        "UPLOAD ERROR",
                        e
                    )
                }
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    override fun onCleared() {
        stopSync()
        repository.close()
        super.onCleared()
    }
}