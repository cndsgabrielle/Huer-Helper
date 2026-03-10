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
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth
        db = Firebase.firestore

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
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid

                        val userProfile = hashMapOf(
                            "username" to username,
                            "email"    to email,
                            "uid"      to userId,
                            "status"   to "active",
                            "role"     to "user",
                            "createdAt" to System.currentTimeMillis()
                        )

                        if (userId != null) {
                            db.collection("Users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Welcome, $username!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        setupSignInLink()
    }

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