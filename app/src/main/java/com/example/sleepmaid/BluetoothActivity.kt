
package com.example.sleepmaid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class BluetoothActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var bleViewModel: BleViewModel

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var hasSensorSample = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        bleViewModel = ViewModelProvider(this)[
            BleViewModel::class.java
        ]

        // Налаштування нижньої навігації
        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        bottomNavigation.selectedItemId = R.id.nav_bluetooth

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    finish()
                    true
                }

                R.id.nav_bluetooth -> true

                else -> false
            }
        }

        // Отримання акселерометра
        sensorManager = getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )

        // Елементи Bluetooth-інтерфейсу
        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val btnDisconnect =
            findViewById<Button>(R.id.btnDisconnect)

        val pbScan = findViewById<ProgressBar>(R.id.pbScan)
        val tvBleStatus =
            findViewById<TextView>(R.id.tvBleStatus)
        val tvBleTerminal =
            findViewById<TextView>(R.id.tvBleTerminal)
        val llDevices =
            findViewById<LinearLayout>(R.id.llDevices)

        // Пошук віртуальних пристроїв
        btnScan.setOnClickListener {
            bleViewModel.scan()
        }

        // Передача поточних даних акселерометра
        btnSend.setOnClickListener {
            if (hasSensorSample) {
                bleViewModel.sendSensorData(
                    lastX, lastY, lastZ
                )
            } else {
                Toast.makeText(
                    this,
                    "Очікування даних акселерометра",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Відключення від пристрою
        btnDisconnect.setOnClickListener {
            bleViewModel.disconnect()
        }

        // Спостереження за StateFlow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleViewModel.state.collect { state ->

                    tvBleStatus.text =
                        "Статус: ${state.phase}"

                    pbScan.visibility =
                        if (state.phase == BlePhase.SCANNING) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    btnScan.isEnabled =
                        state.phase == BlePhase.DISCONNECTED ||
                                state.phase == BlePhase.FOUND

                    btnSend.isEnabled =
                        state.phase == BlePhase.CONNECTED

                    btnDisconnect.isEnabled =
                        state.phase == BlePhase.CONNECTED

                    // Відображення знайдених пристроїв
                    llDevices.removeAllViews()

                    state.devices.forEach { device ->
                        val button = Button(
                            this@BluetoothActivity
                        )

                        button.text = device.name

                        button.isEnabled =
                            state.phase == BlePhase.FOUND ||
                                    state.phase == BlePhase.DISCONNECTED

                        button.setOnClickListener {
                            bleViewModel.connect(device)
                        }

                        llDevices.addView(button)
                    }

                    // Відображення журналу
                    tvBleTerminal.text =
                        state.logs.joinToString("\n")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        hasSensorSample = false

        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        lastX = event.values[0]
        lastY = event.values[1]
        lastZ = event.values[2]
        hasSensorSample = true
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Додаткова обробка не потрібна
    }
}
