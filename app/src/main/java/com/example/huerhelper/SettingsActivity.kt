package com.example.huerhelper

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. Back Button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // 2. Sign Out Button logic
        findViewById<Button>(R.id.btn_sign_out).setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            // This removes all previous screens from the phone's memory
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        // 3. Sparkle Animation
        val sparkle = findViewById<ImageView>(R.id.sparkle_icon_settings)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_glow)
        sparkle.startAnimation(pulse)

        // 1. Find UI Elements
        val tvEdit = findViewById<TextView>(R.id.tv_edit)
        val btnSignOut = findViewById<Button>(R.id.btn_sign_out)
        val layoutViewPassword = findViewById<LinearLayout>(R.id.layout_password_view)
        val layoutChangePassword = findViewById<LinearLayout>(R.id.layout_change_password)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etUsername = findViewById<EditText>(R.id.et_username)

// 2. Find Password Toggle Elements
        val etNewPass = findViewById<EditText>(R.id.et_new_password)
        val ivEyeNew = findViewById<ImageView>(R.id.iv_eye_new)
        val etConfirmPass = findViewById<EditText>(R.id.et_confirm_password)
        val ivEyeConfirm = findViewById<ImageView>(R.id.iv_eye_confirm)

// 3. EDIT CLICK: Switch to Edit Mode
        tvEdit.setOnClickListener {
            layoutChangePassword.visibility = View.VISIBLE
            layoutViewPassword.visibility = View.GONE
            btnSignOut.visibility = View.GONE
            tvEdit.visibility = View.INVISIBLE
            etEmail.isEnabled = true
            etUsername.isEnabled = true
        }

// 4. CANCEL CLICK: Return to View Mode
        btnCancel.setOnClickListener {
            layoutChangePassword.visibility = View.GONE
            layoutViewPassword.visibility = View.VISIBLE
            btnSignOut.visibility = View.VISIBLE
            tvEdit.visibility = View.VISIBLE
            etEmail.isEnabled = false
            etUsername.isEnabled = false
        }

// 5. EYE TOGGLE LOGIC: New Password
        ivEyeNew.setOnClickListener {
            togglePassword(etNewPass, ivEyeNew)
        }

// 6. EYE TOGGLE LOGIC: Confirm Password
        ivEyeConfirm.setOnClickListener {
            togglePassword(etConfirmPass, ivEyeConfirm)
        }

    }
    // This helper function handles the logic for all eye icons
    private fun togglePassword(editText: EditText, imageView: ImageView) {
        if (editText.transformationMethod == PasswordTransformationMethod.getInstance()) {
            // Change to visible text
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye_off) // Make sure you have this icon
        } else {
            // Change back to dots (masked)
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye)
        }
        // This keeps the typing cursor at the end of the password
        editText.setSelection(editText.text.length)
    }


}