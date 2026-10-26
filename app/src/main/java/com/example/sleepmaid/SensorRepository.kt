package com.example.sleepmaid

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class SensorRepository {

    private val database = FirebaseDatabase.getInstance(
        "https://sleepmaiden-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    // Місце збереження показників у Firebase
    private val readingsRef = database
        .getReference("sensor_readings")
        .child("sleep-maiden-emulator")

    // Стан з'єднання з сервером Firebase
    private val connectionRef =
        database.getReference(".info/connected")

    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    private val connectionListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            val isConnected =
                snapshot.getValue(Boolean::class.java) ?: false

            _connected.value = isConnected

            Log.d(
                "SleepCloud",
                "Firebase connected: $isConnected"
            )
        }

        override fun onCancelled(error: DatabaseError) {
            _connected.value = false
            Log.e("SleepCloud", error.message)
        }
    }

    init {
        connectionRef.addValueEventListener(connectionListener)
    }

    // Асинхронне відправлення вимірювання
    suspend fun upload(data: SensorData) {
        val newReading = readingsRef.push()

        // await() очікує підтвердження операції
        // без блокування головного потоку
        newReading.setValue(data).await()

        Log.d(
            "SleepCloud",
            "UPLOAD SUCCESS: ${newReading.key}, " +
                    "X=${data.x}, Y=${data.y}, Z=${data.z}"
        )
    }

    fun close() {
        connectionRef.removeEventListener(connectionListener)
    }
}