package com.example.obstaclerace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlayButtonsSlow = findViewById<Button>(R.id.btn_play_buttons_slow)
        val btnPlayButtonsFast = findViewById<Button>(R.id.btn_play_buttons_fast)
        val btnPlaySensors = findViewById<Button>(R.id.btn_play_sensors)
        val btnHighScores = findViewById<Button>(R.id.btn_high_scores)

        // הפעלה עם כפתורים - מצב איטי
        btnPlayButtonsSlow.setOnClickListener {
            startGameActivity(isSensorMode = false, isFastMode = false)
        }

        // הפעלה עם כפתורים - מצב מהיר
        btnPlayButtonsFast.setOnClickListener {
            startGameActivity(isSensorMode = false, isFastMode = true)
        }

        // הפעלה עם חיישנים
        btnPlaySensors.setOnClickListener {
            startGameActivity(isSensorMode = true, isFastMode = false)
        }

        // מעבר לטבלת שיאים
        btnHighScores.setOnClickListener {
            val intent = Intent(this, HighScoresActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startGameActivity(isSensorMode: Boolean, isFastMode: Boolean) {
        val intent = Intent(this, GameActivity::class.java)
        // שולחים למסך המשחק את ההגדרות שהשחקן בחר
        intent.putExtra("SENSOR_MODE", isSensorMode)
        intent.putExtra("FAST_MODE", isFastMode)
        startActivity(intent)
    }
}