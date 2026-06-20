package com.example.obstaclerace

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var tvOdometer: TextView
    private lateinit var tvCoins: TextView
    private lateinit var gameGrid: GridLayout
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var hearts: List<ImageView>

    private val numLanes = 5
    private val numRows = 8
    private var carLane = 2
    private var lives = 3
    private var distance = 0
    private var coinsCount = 0
    private var gameJob: Job? = null
    private var isGameActive = true

    private var isSensorMode = false
    private var isFastMode = false
    private var currentDelay = 600L
    private var coinRotation = 0f

    private val gridState = Array(numRows) { IntArray(numLanes) { 0 } }
    private val gridImages = Array(numRows) { arrayOfNulls<ImageView>(numLanes) }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastSensorMoveTime: Long = 0

    // לקוח המיקום של גוגל
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // מנגנון בקשת ההרשאות החדש של אנדרואיד
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // המשתמש אישר! ננסה שוב לקחת מיקום
                fetchLocationAndSaveScore()
            } else {
                // המשתמש סירב, נשמור את השיא עם קואורדינטות אפס או ברירת מחדל
                Toast.makeText(this, "ללא הרשאת מיקום, השיא נשמר ללא GPS", Toast.LENGTH_SHORT).show()
                ScoreManager.saveScore(this, distance, 32.11, 34.80) // ברירת מחדל לאזור המרכז
                finishGameActivity()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        isSensorMode = intent.getBooleanExtra("SENSOR_MODE", false)
        isFastMode = intent.getBooleanExtra("FAST_MODE", false)
        currentDelay = if (isFastMode) 300L else 600L

        // אתחול שירות המיקומים
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupGrid()
        setupControls()

        if (isSensorMode) {
            initSensors()
        }

        startGameLoop()
    }

    private fun initViews() {
        tvOdometer = findViewById(R.id.tv_odometer)
        tvCoins = findViewById(R.id.tv_coins)
        gameGrid = findViewById(R.id.game_grid)
        btnLeft = findViewById(R.id.btn_move_left)
        btnRight = findViewById(R.id.btn_move_right)

        hearts = listOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )
    }

    private fun setupGrid() {
        gameGrid.removeAllViews()
        gameGrid.columnCount = numLanes
        gameGrid.rowCount = numRows

        for (r in 0 until numRows) {
            for (c in 0 until numLanes) {
                val imageView = ImageView(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(c, 1f)
                        rowSpec = GridLayout.spec(r, 1f)
                        setMargins(4, 4, 4, 4)
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
                gridImages[r][c] = imageView
                gameGrid.addView(imageView)
            }
        }
        updateMatrixUI()
    }

    private fun setupControls() {
        if (isSensorMode) {
            findViewById<View>(R.id.bottom_controls).visibility = View.GONE
        } else {
            btnLeft.setOnClickListener { moveCar(-1) }
            btnRight.setOnClickListener { moveCar(1) }
        }
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun moveCar(direction: Int) {
        if (!isGameActive) return
        val newLane = carLane + direction
        if (newLane in 0 until numLanes) {
            carLane = newLane
            updateMatrixUI()
            checkCollision()
        }
    }

    private fun startGameLoop() {
        gameJob = lifecycleScope.launch {
            while (isGameActive) {
                delay(currentDelay)
                coinRotation = (coinRotation + 60f) % 360f
                moveObjectsDown()
                generateRandomObject()
                distance += 10
                tvOdometer.text = "Dist: ${distance}m"
                updateMatrixUI()
                checkCollision()
            }
        }
    }

    private fun moveObjectsDown() {
        for (r in numRows - 1 downTo 1) {
            for (c in 0 until numLanes) {
                gridState[r][c] = gridState[r - 1][c]
            }
        }
        for (c in 0 until numLanes) {
            gridState[0][c] = 0
        }
    }

    private fun generateRandomObject() {
        if (Random.nextInt(100) < 40) {
            val randomLane = Random.nextInt(numLanes)
            val isCoin = Random.nextInt(100) < 30
            gridState[0][randomLane] = if (isCoin) 2 else 1
        }
    }

    private fun updateMatrixUI() {
        for (r in 0 until numRows) {
            for (c in 0 until numLanes) {
                val imageView = gridImages[r][c] ?: continue
                if (r == numRows - 1 && c == carLane) {
                    imageView.setImageResource(R.drawable.car)
                    imageView.visibility = View.VISIBLE
                    imageView.rotation = 0f
                    imageView.rotationY = 0f
                } else {
                    when (gridState[r][c]) {
                        1 -> {
                            imageView.setImageResource(R.drawable.stone)
                            imageView.visibility = View.VISIBLE
                            imageView.rotation = 0f
                            imageView.rotationY = 0f
                        }
                        2 -> {
                            imageView.setImageResource(R.drawable.coin)
                            imageView.visibility = View.VISIBLE
                            imageView.rotation = 0f
                            imageView.rotationY = coinRotation
                        }
                        else -> {
                            imageView.setImageDrawable(null)
                        }
                    }
                }
            }
        }
    }

    private fun checkCollision() {
        val currentCellState = gridState[numRows - 1][carLane]
        if (currentCellState == 1) {
            gridState[numRows - 1][carLane] = 0
            lives--

            MediaPlayer.create(this, R.raw.crash)?.start()
            triggerVibration()
            updateHeartsUI()
            Toast.makeText(this, "Crash! \uD83D\uDCA5", Toast.LENGTH_SHORT).show()

            if (lives <= 0) {
                endGame()
            }
        } else if (currentCellState == 2) {
            gridState[numRows - 1][carLane] = 0
            coinsCount++
            tvCoins.text = "Coins: $coinsCount"

            MediaPlayer.create(this, R.raw.coin_sound)?.start()
            triggerVibration()
        }
    }

    private fun updateHeartsUI() {
        for (i in hearts.indices) {
            if (i < lives) {
                hearts[i].visibility = View.VISIBLE
            } else {
                hearts[i].visibility = View.INVISIBLE
            }
        }
    }

    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(300)
        }
    }

    private fun endGame() {
        isGameActive = false
        gameJob?.cancel()

        // במקום לסיים מיד, אנחנו מבקשים את המיקום
        fetchLocationAndSaveScore()
    }

    private fun fetchLocationAndSaveScore() {
        // בדיקה האם יש לנו כבר הרשאת מיקום
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // יש הרשאה! מביאים את המיקום העדכני
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    ScoreManager.saveScore(this, distance, location.latitude, location.longitude)
                } else {
                    // במקרה נדיר שה-GPS כבוי לגמרי במכשיר
                    ScoreManager.saveScore(this, distance, 32.11, 34.80)
                }
                finishGameActivity()
            }
        } else {
            // אין הרשאה - קופץ חלון בקשה למשתמש
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun finishGameActivity() {
        Toast.makeText(this, "Game Over! Total Distance: ${distance}m", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isSensorMode || !isGameActive || event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val tiltX = event.values[0]
            val tiltY = event.values[1]
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastSensorMoveTime > 300) {
                if (tiltX < -2.0) {
                    moveCar(1)
                    lastSensorMoveTime = currentTime
                } else if (tiltX > 2.0) {
                    moveCar(-1)
                    lastSensorMoveTime = currentTime
                }
            }

            val clampedY = tiltY.coerceIn(0f, 10f)
            currentDelay = (200 + (clampedY * 60)).toLong()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        if (isSensorMode && ::sensorManager.isInitialized) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isSensorMode && ::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameJob?.cancel()
    }
}