package com.example.huerhelper

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rootView: View

    private var adminDocId: String? = null
    private var isAdminSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth     = FirebaseAuth.getInstance()
        db       = FirebaseFirestore.getInstance()
        rootView = findViewById(android.R.id.content)

        // ── UI Elements ───────────────────────────────────────────────────────
        val btnBack              = findViewById<ImageButton>(R.id.btn_back)
        val btnSignOut           = findViewById<Button>(R.id.btn_sign_out)
        val sparkle              = findViewById<ImageView>(R.id.sparkle_icon_settings)
        val tvEdit               = findViewById<TextView>(R.id.tv_edit)
        val layoutViewPassword   = findViewById<LinearLayout>(R.id.layout_password_view)
        val layoutChangePassword = findViewById<LinearLayout>(R.id.layout_change_password)
        val btnCancel            = findViewById<Button>(R.id.btn_cancel)
        val btnSave              = findViewById<Button>(R.id.btn_save_changes)
        val etEmail              = findViewById<EditText>(R.id.et_email)
        val etUsername           = findViewById<EditText>(R.id.et_username)
        val etNewPass            = findViewById<EditText>(R.id.et_new_password)
        val etConfirmPass        = findViewById<EditText>(R.id.et_confirm_password)
        val ivEyeNew             = findViewById<ImageView>(R.id.iv_eye_new)
        val ivEyeConfirm         = findViewById<ImageView>(R.id.iv_eye_confirm)

        findViewById<ImageView>(R.id.iv_eye_view).visibility = View.GONE
        etNewPass.transformationMethod    = PasswordTransformationMethod.getInstance()
        etConfirmPass.transformationMethod = PasswordTransformationMethod.getInstance()

        // ── Sparkle & Back ────────────────────────────────────────────────────
        sparkle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))
        btnBack.setOnClickListener { finish() }

        // ── Load user data ────────────────────────────────────────────────────
        val firebaseUser = auth.currentUser

        if (firebaseUser != null) {
            isAdminSession = false
            etEmail.setText(firebaseUser.email)
            db.collection("Users").document(firebaseUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) etUsername.setText(doc.getString("username"))
                }
        } else {
            isAdminSession = true
            loadAdminData(etUsername, etEmail, etNewPass)
        }

        // ── Mode Toggles ──────────────────────────────────────────────────────
        tvEdit.setOnClickListener {
            layoutChangePassword.visibility = View.VISIBLE
            layoutViewPassword.visibility   = View.GONE
            btnSignOut.visibility           = View.GONE
            tvEdit.visibility               = View.INVISIBLE
            etUsername.isEnabled            = true
            if (isAdminSession) etEmail.isEnabled = true
        }

        btnCancel.setOnClickListener {
            layoutChangePassword.visibility = View.GONE
            layoutViewPassword.visibility   = View.VISIBLE
            btnSignOut.visibility           = View.VISIBLE
            tvEdit.visibility               = View.VISIBLE
            etUsername.isEnabled            = false
            etEmail.isEnabled               = false
            etNewPass.text.clear()
            etConfirmPass.text.clear()
        }

        // ── Eye Toggles ───────────────────────────────────────────────────────
        ivEyeNew.setOnClickListener     { togglePassword(etNewPass, ivEyeNew) }
        ivEyeConfirm.setOnClickListener { togglePassword(etConfirmPass, ivEyeConfirm) }

        // ── Save Logic ────────────────────────────────────────────────────────
        btnSave.setOnClickListener {
            val newUsername = etUsername.text.toString().trim()
            val newPass     = etNewPass.text.toString().trim()
            val confirmPass = etConfirmPass.text.toString().trim()

            if (isAdminSession) {
                // ── Admin save ────────────────────────────────────────────────
                val docId = adminDocId ?: run {
                    showSnackbar("Admin document not found.")
                    return@setOnClickListener
                }

                val updates = mutableMapOf<String, Any>()
                if (newUsername.isNotEmpty()) updates["username"] = newUsername

                if (newPass.isNotEmpty()) {
                    if (newPass != confirmPass) {
                        showSnackbar("Passwords do not match")
                        return@setOnClickListener
                    }
                    updates["password"] = newPass
                }

                if (updates.isEmpty()) {
                    showSnackbar("Nothing to update.")
                    return@setOnClickListener
                }

                db.collection("Users").document(docId)
                    .update(updates)
                    .addOnSuccessListener {
                        showSnackbar("Admin profile updated!")
                        btnCancel.performClick()
                    }
                    .addOnFailureListener { e ->
                        showSnackbar("Error: ${e.message}")
                    }

            } else {
                // ── Regular user save ─────────────────────────────────────────
                val user = firebaseUser ?: return@setOnClickListener

                if (newUsername.isNotEmpty()) {
                    db.collection("Users").document(user.uid).update("username", newUsername)
                }

                if (newPass.isNotEmpty()) {
                    if (newPass != confirmPass) {
                        showSnackbar("Passwords do not match")
                        return@setOnClickListener
                    }
                    if (newPass.length < 6) {
                        showSnackbar("Password must be at least 6 characters")
                        return@setOnClickListener
                    }
                    user.updatePassword(newPass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showSnackbar("Profile and password updated!")
                            btnCancel.performClick()
                        } else {
                            showSnackbar("Error: ${task.exception?.message}")
                        }
                    }
                } else {
                    showSnackbar("Username updated!")
                    btnCancel.performClick()
                }
            }
        }

        // ── Sign Out ──────────────────────────────────────────────────────────
        btnSignOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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

    // ── Fetch admin data ──────────────────────────────────────────────────────

    private fun loadAdminData(etUsername: EditText, etEmail: EditText, etPassword: EditText) {
        db.collection("Users")
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents[0]
                    adminDocId = doc.id
                    etUsername.setText(doc.getString("username") ?: "")
                    etEmail.setText(doc.getString("email") ?: "")
                    etPassword.setText(doc.getString("password") ?: "")
                }
            }
            .addOnFailureListener {
                showSnackbar("Failed to load admin data.")
            }
    }

    // ── Password visibility toggle ────────────────────────────────────────────

    private fun togglePassword(editText: EditText, imageView: ImageView) {
        val selection = editText.selectionStart
        if (editText.transformationMethod is PasswordTransformationMethod) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye_off)
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye)
        }
        editText.setSelection(selection)
    }
}