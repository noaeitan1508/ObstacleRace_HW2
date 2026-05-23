package com.example.obstaclerace

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var imgGameOverSign: ImageView
    private lateinit var layoutGameOver: ConstraintLayout
    private lateinit var hearts: Array<ImageView>
    private lateinit var rockMatrix: Array<Array<ImageView>>
    private lateinit var carArray: Array<ImageView>

    private var currentLane = 1
    private var obstacleRow = -1
    private var obstacleCol = -1
    private var lives = 3
    private var isGameRunning = false

    // משתנה ששומר את ההודעה הקיימת כדי למנוע דיליי
    private var currentToast: Toast? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        resetGame()
    }

    private fun initViews() {
        btnLeft = findViewById(R.id.game_BTN_left)
        btnRight = findViewById(R.id.game_BTN_right)
        layoutGameOver = findViewById(R.id.layout_game_over)
        imgGameOverSign = findViewById(R.id.img_game_over_sign)

        hearts = arrayOf(
            findViewById(R.id.game_IMG_heart1),
            findViewById(R.id.game_IMG_heart2),
            findViewById(R.id.game_IMG_heart3)
        )

        carArray = arrayOf(
            findViewById(R.id.car_0),
            findViewById(R.id.car_1),
            findViewById(R.id.car_2)
        )

        // מסובב את המכוניות כך שיפנו קדימה תמיד
        for (car in carArray) {
            car.rotation = 180f
        }

        rockMatrix = arrayOf(
            arrayOf(findViewById(R.id.rock_0_0), findViewById(R.id.rock_0_1), findViewById(R.id.rock_0_2)),
            arrayOf(findViewById(R.id.rock_1_0), findViewById(R.id.rock_1_1), findViewById(R.id.rock_1_2)),
            arrayOf(findViewById(R.id.rock_2_0), findViewById(R.id.rock_2_1), findViewById(R.id.rock_2_2)),
            arrayOf(findViewById(R.id.rock_3_0), findViewById(R.id.rock_3_1), findViewById(R.id.rock_3_2)),
            arrayOf(findViewById(R.id.rock_4_0), findViewById(R.id.rock_4_1), findViewById(R.id.rock_4_2)),
            arrayOf(findViewById(R.id.rock_5_0), findViewById(R.id.rock_5_1), findViewById(R.id.rock_5_2))
        )
    }

    private fun setupListeners() {
        btnLeft.setOnClickListener { moveCarLeft() }
        btnRight.setOnClickListener { moveCarRight() }

        // לחיצה על מסך ה-Game Over מאפסת את המשחק (בלחיצה על התמונה עצמה)
        imgGameOverSign.setOnClickListener { resetGame() }
    }

    private fun moveCarLeft() {
        if (currentLane > 0 && isGameRunning) {
            carArray[currentLane].visibility = View.INVISIBLE
            currentLane--
            carArray[currentLane].visibility = View.VISIBLE
        }
    }

    private fun moveCarRight() {
        if (currentLane < 2 && isGameRunning) {
            carArray[currentLane].visibility = View.INVISIBLE
            currentLane++
            carArray[currentLane].visibility = View.VISIBLE
        }
    }

    private fun startGameLoop() {
        gameRunnable = object : Runnable {
            override fun run() {
                if (isGameRunning) {
                    moveObstacle()
                    handler.postDelayed(this, 250)
                }
            }
        }
        handler.post(gameRunnable)
    }

    private fun moveObstacle() {
        if (obstacleRow >= 0 && obstacleRow < 6) {
            rockMatrix[obstacleRow][obstacleCol].visibility = View.INVISIBLE
        }

        obstacleRow++

        if (obstacleRow >= 6) {
            obstacleRow = 0
            obstacleCol = Random.nextInt(3)
        }

        rockMatrix[obstacleRow][obstacleCol].visibility = View.VISIBLE

        if (obstacleRow == 5 && obstacleCol == currentLane) {
            handleCollision()
        }
    }

    private fun handleCollision() {
        if (lives > 0) {
            lives--
            hearts[lives].visibility = View.INVISIBLE
            vibrateDevice()

            // מבטל את ההודעה הישנה מיד ומקפיץ את החדשה כדי למנוע את הדיליי
            currentToast?.cancel()
            currentToast = Toast.makeText(this, "התרסקות! נותרו $lives חיים", Toast.LENGTH_SHORT)
            currentToast?.show()

            rockMatrix[obstacleRow][obstacleCol].visibility = View.INVISIBLE
            obstacleRow = -1
        }

        if (lives == 0) {
            gameOver()
        }
    }

    private fun gameOver() {
        isGameRunning = false
        vibrateDevice()
        layoutGameOver.visibility = View.VISIBLE
    }

    private fun resetGame() {
        layoutGameOver.visibility = View.GONE
        lives = 3
        hearts.forEach { it.visibility = View.VISIBLE }

        carArray.forEach { it.visibility = View.INVISIBLE }
        currentLane = 1
        carArray[currentLane].visibility = View.VISIBLE

        for (row in 0..5) {
            for (col in 0..2) {
                rockMatrix[row][col].visibility = View.INVISIBLE
            }
        }

        obstacleRow = -1
        obstacleCol = Random.nextInt(3)

        isGameRunning = true
        startGameLoop()
    }

    private fun vibrateDevice() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isGameRunning) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> { moveCarLeft(); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> { moveCarRight(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gameRunnable)
    }
}