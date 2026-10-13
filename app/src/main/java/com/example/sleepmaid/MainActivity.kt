package com.example.sleepmaid


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt
import kotlin.math.sqrt
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var tvX: TextView
    private lateinit var tvY: TextView
    private lateinit var tvZ: TextView
    private lateinit var tvSensorStatus: TextView
    private lateinit var tvMotion: TextView
    private lateinit var tvActivity: TextView

    private lateinit var pbTiltX: ProgressBar
    private lateinit var pbTiltY: ProgressBar
    private lateinit var pbTiltZ: ProgressBar
    private lateinit var pbMotion: ProgressBar

    // Попередні показники сенсора
    private var previousX = 0f
    private var previousY = 0f
    private var previousZ = 0f
    private var hasPreviousSample = false

    // Згладжений показник рухової активності
    private var smoothedMotion = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ініціалізація компонентів інтерфейсу
        tvX = findViewById(R.id.tvX)
        tvY = findViewById(R.id.tvY)
        tvZ = findViewById(R.id.tvZ)
        tvSensorStatus = findViewById(R.id.tvSensorStatus)
        tvMotion = findViewById(R.id.tvMotion)
        tvActivity = findViewById(R.id.tvActivity)

        pbTiltX = findViewById(R.id.pbTiltX)
        pbTiltY = findViewById(R.id.pbTiltY)
        pbTiltZ = findViewById(R.id.pbTiltZ)
        pbMotion = findViewById(R.id.pbMotion)

        // Отримання системного менеджера сенсорів
        sensorManager = getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        // Отримання акселерометра
        accelerometer = sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )

        //Створення навігації між екранами
        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Поточна сторінка — Головна
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_bluetooth -> {
                    val intent = Intent(
                        this,
                        BluetoothActivity::class.java
                    )
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

    }

    override fun onResume() {
        super.onResume()

        // Починаємо нову послідовність вимірювань
        hasPreviousSample = false
        smoothedMotion = 0f

        // Реєстрація слухача подій сенсора
        val registered = accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        } ?: false

        tvSensorStatus.text = if (registered) {
            "Акселерометр активний"
        } else {
            "Акселерометр недоступний"
        }

        findViewById<BottomNavigationView>(
            R.id.bottomNavigation
        ).selectedItemId = R.id.nav_home
    }

    override fun onPause() {
        super.onPause()

        // Зупинка сенсора для економії енергії
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        // Отримання прискорення за трьома осями
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Оновлення координат в інтерфейсі
        tvX.text = "X: %.2f м/с²".format(x)
        tvY.text = "Y: %.2f м/с²".format(y)
        tvZ.text = "Z: %.2f м/с²".format(z)

        // Перетворення нахилу за X у шкалу 0..100
        val tiltX = (
                (x / SensorManager.GRAVITY_EARTH + 1f) * 50f
                ).roundToInt().coerceIn(0, 100)
        val tiltY = (
                (y / SensorManager.GRAVITY_EARTH + 1f) * 50f
                ).roundToInt().coerceIn(0, 100)
        val tiltZ = (
                (z / SensorManager.GRAVITY_EARTH + 1f) * 50f
                ).roundToInt().coerceIn(0, 100)


        pbTiltX.progress = tiltX
        pbTiltY.progress = tiltY
        pbTiltZ.progress = tiltZ

        // Різниця між поточними і попередніми
        // показниками акселерометра
        val motion = if (hasPreviousSample) {
            val dx = x - previousX
            val dy = y - previousY
            val dz = z - previousZ

            sqrt(dx * dx + dy * dy + dz * dz)
        } else {
            0f
        }

        // Зберігаємо показники для наступного вимірювання
        previousX = x
        previousY = y
        previousZ = z
        hasPreviousSample = true

        // Згладжування різких змін
        smoothedMotion =
            0.8f * smoothedMotion + 0.2f * motion

        // Умовна шкала інтенсивності руху
        val motionPercent = (
                smoothedMotion * 100f
                ).roundToInt().coerceIn(0, 100)

        pbMotion.progress = motionPercent
        tvMotion.text = "Інтенсивність руху: $motionPercent%"

        // Проста класифікація рухової активності
        val activity = when {
            smoothedMotion < 0.05f -> "Спокій"
            smoothedMotion < 0.3f -> "Слабкий рух"
            else -> "Активний рух"
        }

        tvActivity.text = "Стан: $activity"
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Для цієї лабораторної додаткова
        // обробка зміни точності не потрібна
    }
}
