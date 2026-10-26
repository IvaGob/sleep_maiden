package com.example.sleepmaid

data class SensorData(
    val deviceId: String = "sleep-maiden-emulator",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "accelerometer",
    val value: Float = 0f,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)