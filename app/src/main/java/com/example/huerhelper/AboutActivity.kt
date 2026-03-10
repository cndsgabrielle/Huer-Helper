package com.example.huerhelper

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.view.animation.AnimationUtils

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_about)

        val btnBack = findViewById<ImageButton>(R.id.btn_about_back)
        btnBack.setOnClickListener { finish() }

        val aboutSparkle: ImageView = findViewById(R.id.about_sparkle)
        aboutSparkle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))
    }
}