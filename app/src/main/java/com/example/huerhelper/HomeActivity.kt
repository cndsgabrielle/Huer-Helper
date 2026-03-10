package com.example.huerhelper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // --- FIREBASE USERNAME INTEGRATION ---
        val subtitle = findViewById<TextView>(R.id.home_subtitle)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("Users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("username")
                        subtitle.text = "Welcome back, $name!"
                    }
                }
        }

        // --- 1. LIVE COLOR SCAN (Top Card) ---
        val scanCard = findViewById<View>(R.id.card_live_scan)
        scanCard.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // --- 2. STYLE MATCHER (Middle Card) ---
        val styleCard = findViewById<View>(R.id.card_style_matcher)
        styleCard.findViewById<View>(R.id.card_background).setBackgroundResource(R.drawable.card_gradient_teal)
        styleCard.findViewById<ImageView>(R.id.feature_icon).setImageResource(R.drawable.ic_style)
        styleCard.findViewById<TextView>(R.id.feature_title).text = "Style Matcher"
        styleCard.findViewById<TextView>(R.id.feature_desc).text = "Coordinate your wardrobe with dual-camera"
        styleCard.setOnClickListener {
            startActivity(Intent(this, StyleMatcherActivity::class.java))
        }

        // --- 3. BOOKMARKS (Bottom Card) ---
        val bookmarksCard = findViewById<View>(R.id.card_bookmarks)
        bookmarksCard.findViewById<View>(R.id.card_background).setBackgroundResource(R.drawable.card_gradient_purple)
        bookmarksCard.findViewById<ImageView>(R.id.feature_icon).setImageResource(R.drawable.ic_bookmark)
        bookmarksCard.findViewById<TextView>(R.id.feature_title).text = "Bookmarks"
        bookmarksCard.findViewById<TextView>(R.id.feature_desc).text = "Your personal color memory library"
        bookmarksCard.setOnClickListener {
            startActivity(Intent(this, BookmarksActivity::class.java))
        }

        // --- BUTTONS AND ANIMATIONS ---
        val aboutButton = findViewById<ImageButton>(R.id.btn_about)
        aboutButton.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        val settingsBtn = findViewById<ImageButton>(R.id.btn_settings)
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val sparkleIcon: ImageView = findViewById(R.id.sparkle_icon)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        sparkleIcon.startAnimation(pulseAnimation)
    }
}