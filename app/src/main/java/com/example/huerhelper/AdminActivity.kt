package com.example.huerhelper

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Set Transparent Status Bar
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_admin)

        // 2. Start sparkle animation
        val sparkle = findViewById<ImageView>(R.id.sparkle_icon_admin)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        sparkle.startAnimation(pulse)

        // 3. Setup Settings Button (Replacing the old back button)
        val btnSettings = findViewById<ImageButton>(R.id.btn_admin_settings)

        btnSettings.setOnClickListener {
            // Navigate to Settings Page to allow Logout
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}