package com.example.huerhelper

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MatchResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_results)

        // 1. Setup the Glowing Star Animation
        val resultSparkle: ImageView = findViewById(R.id.result_sparkle)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        resultSparkle.startAnimation(pulseAnimation)

        val btnRetry: Button = findViewById(R.id.btn_retry)
        btnRetry.setOnClickListener {
            // This closes the results and returns to the camera to try another match
            finish()
        }
    }
}