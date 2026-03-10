package com.example.huerhelper

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        db   = Firebase.firestore

        rootView = findViewById(android.R.id.content)

        setupSignUpLink()

        val sparkleIcon = findViewById<ImageView>(R.id.sparkle_icon)
        sparkleIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))

        val signInButton  = findViewById<Button>(R.id.btn_signin)
        val usernameField = findViewById<EditText>(R.id.et_username)
        val passwordField = findViewById<EditText>(R.id.et_password)
        val ivEyePassword = findViewById<ImageView>(R.id.iv_eye_password)

        // ── Password visibility toggle ────────────────────────────────────────
        var passwordVisible = false
        ivEyePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                passwordField.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                ivEyePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                passwordField.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                ivEyePassword.setImageResource(R.drawable.ic_eye)
            }
            passwordField.setSelection(passwordField.text.length)
        }

        signInButton.setOnClickListener {
            val userEntry = usernameField.text.toString().trim()
            val pass      = passwordField.text.toString().trim()

            if (userEntry.isEmpty() || pass.isEmpty()) {
                showSnackbar("Please fill in all fields")
                return@setOnClickListener
            }

            performLogin(userEntry, pass)
        }
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

    // ── Navigate with message — shows snackbar then navigates after 1.5s ─────

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

    // ── Login ─────────────────────────────────────────────────────────────────

    private fun performLogin(username: String, pass: String) {
        db.collection("Users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showSnackbar("Username not found")
                    return@addOnSuccessListener
                }

                val userDoc = documents.documents[0]
                val role    = userDoc.getString("role")   ?: "user"
                val status  = userDoc.getString("status") ?: "active"

                if (status == "suspended") {
                    showSuspendedDialog()
                    return@addOnSuccessListener
                }

                if (role == "admin") {
                    val storedPassword = userDoc.getString("password") ?: ""
                    if (pass == storedPassword) {
                        navigateWithMessage("Welcome, Admin!", AdminActivity::class.java)
                    } else {
                        showSnackbar("Incorrect password.")
                    }
                    return@addOnSuccessListener
                }

                val email = userDoc.getString("email") ?: run {
                    showSnackbar("Account error. Please contact support.")
                    return@addOnSuccessListener
                }

                auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection("Users")
                                .document(task.result.user!!.uid)
                                .get()
                                .addOnSuccessListener { freshDoc ->
                                    val freshStatus = freshDoc.getString("status") ?: "active"
                                    if (freshStatus == "suspended") {
                                        auth.signOut()
                                        showSuspendedDialog()
                                    } else {
                                        db.collection("Users")
                                            .document(task.result.user!!.uid)
                                            .update("lastActive", com.google.firebase.firestore.FieldValue.serverTimestamp())
                                        navigateWithMessage("Welcome back, $username!", HomeActivity::class.java)
                                    }
                                }
                                .addOnFailureListener {
                                    navigateWithMessage("Welcome back, $username!", HomeActivity::class.java)
                                }
                        } else {
                            showSnackbar("Incorrect password.")
                        }
                    }
            }
            .addOnFailureListener {
                showSnackbar("Error connecting to database")
            }
    }

    // ── Suspended Dialog ──────────────────────────────────────────────────────

    private fun showSuspendedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm, null)
        val tvTitle    = dialogView.findViewById<TextView>(R.id.tv_confirm_title)
        val tvMessage  = dialogView.findViewById<TextView>(R.id.tv_confirm_message)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_dialog_confirm)

        tvTitle.text         = "Account Suspended"
        tvMessage.text       = "Your account has been suspended. Please contact support for assistance."
        tvMessage.visibility = View.VISIBLE
        btnConfirm.text      = "OK"
        btnCancel.visibility = View.GONE

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnConfirm.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ── Sign Up Link ──────────────────────────────────────────────────────────

    private fun setupSignUpLink() {
        val signUpTextView = findViewById<TextView>(R.id.signup_link)
        val fullText = "Don't have an account? Sign Up"
        val spannableString = SpannableString(fullText)

        val start = fullText.indexOf("Sign Up")
        val end   = start + "Sign Up".length

        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#22D3EE")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        signUpTextView.text = spannableString
        signUpTextView.movementMethod = LinkMovementMethod.getInstance()
        signUpTextView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}