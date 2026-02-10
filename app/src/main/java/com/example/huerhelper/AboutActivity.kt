package com.example.huerhelper

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.view.animation.AnimationUtils

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the status bar transparent to match the immersive background
        window.statusBarColor = Color.TRANSPARENT

        // Link this code to your XML file
        setContentView(R.layout.activity_about)

        // Setup the back button
        val btnBack = findViewById<ImageButton>(R.id.btn_about_back)
        btnBack.setOnClickListener {
            // Closes this activity and goes back to the previous screen
            finish()
        }

        val aboutSparkle: ImageView = findViewById(R.id.about_sparkle)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        aboutSparkle.startAnimation(pulseAnimation)
    }
}