package com.example.huerhelper

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Immersive Transparent Status Bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)

        // 2. Setup the Sign Up link styling
        setupSignUpLink()

        // 3. Star Animation
        val sparkleIcon = findViewById<ImageView>(R.id.sparkle_icon)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        sparkleIcon.startAnimation(pulse)

        // 4. Login and Admin Logic
        val signInButton = findViewById<Button>(R.id.btn_signin)
        val usernameField = findViewById<EditText>(R.id.et_username)
        val passwordField = findViewById<EditText>(R.id.et_password)

        signInButton.setOnClickListener {
            // Trim whitespace to prevent "admin " (with space) from failing
            val user = usernameField.text.toString().trim()
            val pass = passwordField.text.toString().trim()

            if (user == "admin" && pass == "admin") {
                // Navigate to Admin Dashboard
                val intent = Intent(this, AdminActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Navigate to Home Page for users
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupSignUpLink() {
        val signUpTextView = findViewById<TextView>(R.id.signup_link)
        val fullText = "Don't have an account? Sign Up"
        val spannableString = SpannableString(fullText)

        val start = fullText.indexOf("Sign Up")
        val end = start + "Sign Up".length

        // Apply Vibrant Cyan Color (#22D3EE)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#22D3EE")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        signUpTextView.text = spannableString
        signUpTextView.movementMethod = LinkMovementMethod.getInstance()

        signUpTextView.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}