package com.example.huerhelper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.view.animation.AnimationUtils


class BookmarksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        // 1. Back button navigation
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // 2. Start Scanning button navigation
        //findViewById<Button>(R.id.btn_start_scanning).setOnClickListener {
           // startActivity(Intent(this, ScanActivity::class.java))
       // }

        // 3. Glowing sparkle animation
        val sparkle = findViewById<ImageView>(R.id.sparkle_icon_bookmarks)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        sparkle.startAnimation(pulse)
    }
}