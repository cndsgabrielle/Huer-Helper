package com.example.huerhelper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.ImageButton
import android.widget.ImageView
import android.view.View
import android.widget.TextView
import android.content.Intent
import android.view.animation.AnimationUtils

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. LIVE COLOR SCAN (Top Card - ID: card_live_scan)
        val scanCard = findViewById<View>(R.id.card_live_scan)
        // Keep default styling or set it here
        scanCard.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // 2. STYLE MATCHER (Middle Card - ID: card_style_matcher)
        // In your XML, card_style_matcher is the 2nd include!
        val styleCard = findViewById<View>(R.id.card_style_matcher)
        styleCard.findViewById<View>(R.id.card_background).setBackgroundResource(R.drawable.card_gradient_teal)
        styleCard.findViewById<ImageView>(R.id.feature_icon).setImageResource(R.drawable.ic_style)
        styleCard.findViewById<TextView>(R.id.feature_title).text = "Style Matcher"
        styleCard.findViewById<TextView>(R.id.feature_desc).text = "Coordinate your wardrobe with dual-camera"

        styleCard.setOnClickListener {
            startActivity(Intent(this, StyleMatcherActivity::class.java))
        }

        // 3. BOOKMARKS (Bottom Card - ID: card_bookmarks)
        // In your XML, card_bookmarks is the 3rd include!
        val bookmarksCard = findViewById<View>(R.id.card_bookmarks)
        bookmarksCard.findViewById<View>(R.id.card_background).setBackgroundResource(R.drawable.card_gradient_purple)
        bookmarksCard.findViewById<ImageView>(R.id.feature_icon).setImageResource(R.drawable.ic_bookmark)
        bookmarksCard.findViewById<TextView>(R.id.feature_title).text = "Bookmarks"
        bookmarksCard.findViewById<TextView>(R.id.feature_desc).text = "Your personal color memory library"

        val aboutButton = findViewById<ImageButton>(R.id.btn_about)
        aboutButton.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        val settingsBtn = findViewById<ImageButton>(R.id.btn_settings)
        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val sparkleIcon: ImageView = findViewById(R.id.sparkle_icon)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        sparkleIcon.startAnimation(pulseAnimation)

        bookmarksCard.setOnClickListener {
            val intent = Intent(this, BookmarksActivity::class.java)
            startActivity(intent)
        }

    }
}