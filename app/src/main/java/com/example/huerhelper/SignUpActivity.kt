package com.example.huerhelper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import android.content.Intent
import android.widget.Button


class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // 1. Star Animation
        val signupSparkle = findViewById<ImageView>(R.id.signup_sparkle)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        signupSparkle.startAnimation(pulse)

        // Inside SignUpActivity.kt onCreate
        val btnCreateAccount = findViewById<Button>(R.id.btn_create_account)

        btnCreateAccount.setOnClickListener {
            // 1. (Optional) Add your logic here to save user data to your database

            // 2. Navigate back to the Login Screen (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // 3. Close the sign-up screen so the user can't "go back" to it
            finish()
        }

        setupSignInLink()
    }

    private fun setupSignInLink() {
        val signInTextView = findViewById<TextView>(R.id.login_link)
        val fullText = "Already have an account? Sign In"
        val spannableString = SpannableString(fullText)

        val start = fullText.indexOf("Sign In")
        val end = start + "Sign In".length


        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#22D3EE")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        signInTextView.text = spannableString
        signInTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()


        signInTextView.setOnClickListener {
            finish()
        }
    }
}