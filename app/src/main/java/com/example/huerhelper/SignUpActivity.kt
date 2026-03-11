package com.example.huerhelper

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth
        db   = Firebase.firestore

        rootView = findViewById(android.R.id.content)

        // Sparkle animation
        val signupSparkle = findViewById<ImageView>(R.id.signup_sparkle)
        signupSparkle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))

        // Input fields
        val btnCreateAccount  = findViewById<Button>(R.id.btn_create_account)
        val etEmail           = findViewById<EditText>(R.id.et_email)
        val etUsername        = findViewById<EditText>(R.id.et_username)
        val etPassword        = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)

        // Eye icons
        val ivEyePassword = findViewById<ImageView>(R.id.iv_eye_password)
        val ivEyeConfirm  = findViewById<ImageView>(R.id.iv_eye_confirm)

        // ── Password visibility toggles ───────────────────────────────────────
        var passwordVisible = false
        var confirmVisible  = false

        ivEyePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivEyePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivEyePassword.setImageResource(R.drawable.ic_eye)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        ivEyeConfirm.setOnClickListener {
            confirmVisible = !confirmVisible
            if (confirmVisible) {
                etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivEyeConfirm.setImageResource(R.drawable.ic_eye_off)
            } else {
                etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivEyeConfirm.setImageResource(R.drawable.ic_eye)
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        // ── Create account ────────────────────────────────────────────────────
        btnCreateAccount.setOnClickListener {
            val email           = etEmail.text.toString().trim()
            val username        = etUsername.text.toString().trim()
            val password        = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                showSnackbar("Please fill in all fields")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showSnackbar("Passwords do not match!")
                return@setOnClickListener
            }

            if (password.length < 6) {
                showSnackbar("Password must be at least 6 characters")
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid

                        val userProfile = hashMapOf(
                            "username"  to username,
                            "email"     to email,
                            "uid"       to userId,
                            "status"    to "active",
                            "role"      to "user",
                            "createdAt" to System.currentTimeMillis()
                        )

                        if (userId != null) {
                            db.collection("Users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    navigateWithMessage("Welcome, $username!", MainActivity::class.java)
                                }
                                .addOnFailureListener { e ->
                                    showSnackbar("Database Error: ${e.message}")
                                }
                        }
                    } else {
                        showSnackbar("Sign up failed: ${task.exception?.message}")
                    }
                }
        }

        setupSignInLink()
    }

    // ── Custom Snackbar ───────────────────────────────────────────────────────

    private fun showSnackbar(message: String) {
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundResource(R.drawable.glass_card_dark)

        val tvMessage = snackbarView.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )
        tvMessage.setTextColor(Color.WHITE)
        tvMessage.textSize = 14f
        tvMessage.typeface = Typeface.DEFAULT_BOLD

        snackbar.show()
    }

    // ── Navigate with message ─────────────────────────────────────────────────

    private fun navigateWithMessage(message: String, destination: Class<*>) {
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundResource(R.drawable.glass_card_dark)

        val tvMessage = snackbarView.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )
        tvMessage.setTextColor(Color.WHITE)
        tvMessage.textSize = 14f
        tvMessage.typeface = Typeface.DEFAULT_BOLD

        snackbar.show()

        rootView.postDelayed({
            startActivity(Intent(this, destination))
            finish()
        }, 1500)
    }

    // ── Sign In Link ──────────────────────────────────────────────────────────

    private fun setupSignInLink() {
        val signInTextView = findViewById<TextView>(R.id.login_link)
        val fullText = "Already have an account? Sign In"
        val spannableString = SpannableString(fullText)

        val start = fullText.indexOf("Sign In")
        val end   = start + "Sign In".length

        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#22D3EE")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        signInTextView.text = spannableString
        signInTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        signInTextView.setOnClickListener { finish() }
    }
}