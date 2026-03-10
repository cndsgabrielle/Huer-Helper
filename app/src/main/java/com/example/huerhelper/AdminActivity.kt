package com.example.huerhelper

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.*

class AdminActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rootView: View

    private lateinit var tvTotalUsers: TextView
    private lateinit var tvActiveToday: TextView
    private lateinit var tvUserBookmarks: TextView
    private lateinit var tvGlobalLibrary: TextView
    private lateinit var tvSuspended: TextView
    private lateinit var tvActiveAccounts: TextView
    private lateinit var userListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_admin)

        rootView = findViewById(android.R.id.content)

        val sparkle = findViewById<ImageView>(R.id.sparkle_icon_admin)
        sparkle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))

        findViewById<ImageButton>(R.id.btn_admin_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        tvTotalUsers      = findViewById(R.id.tv_total_users)
        tvActiveToday     = findViewById(R.id.tv_active_today)
        tvUserBookmarks   = findViewById(R.id.tv_user_bookmarks)
        tvGlobalLibrary   = findViewById(R.id.tv_global_library)
        tvSuspended       = findViewById(R.id.tv_suspended_count)
        tvActiveAccounts  = findViewById(R.id.tv_active_count)
        userListContainer = findViewById(R.id.user_list_container)

        loadDashboard()
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

    // ── Dashboard ─────────────────────────────────────────────────────────────

    private fun loadDashboard() {
        db.collection("Users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents

                val total     = users.count { it.getString("role") != "admin" }
                val suspended = users.count { it.getString("status") == "suspended" && it.getString("role") != "admin" }
                val active    = total - suspended

                val todayStart  = getTodayStart()
                val activeToday = users.count { doc ->
                    val lastActive = doc.getTimestamp("lastActive")
                    lastActive != null && lastActive.toDate().after(todayStart)
                }

                tvTotalUsers.text     = total.toString()
                tvActiveToday.text    = activeToday.toString()
                tvSuspended.text      = suspended.toString()
                tvActiveAccounts.text = active.toString()

                loadTotalBookmarks(snapshot)
                buildUserList(snapshot)
            }
            .addOnFailureListener {
                showSnackbar("Failed to load users: ${it.message}")
            }

        tvGlobalLibrary.text = "391"
    }

    private fun loadTotalBookmarks(usersSnapshot: QuerySnapshot) {
        val userIds = usersSnapshot.documents.map { it.id }
        if (userIds.isEmpty()) { tvUserBookmarks.text = "0"; return }

        var totalBookmarks   = 0
        var completedQueries = 0

        userIds.forEach { uid ->
            db.collection("Users").document(uid).collection("bookmarks")
                .get()
                .addOnSuccessListener { bookmarkSnap ->
                    totalBookmarks += bookmarkSnap.size()
                    completedQueries++
                    if (completedQueries == userIds.size) tvUserBookmarks.text = totalBookmarks.toString()
                }
                .addOnFailureListener {
                    completedQueries++
                    if (completedQueries == userIds.size) tvUserBookmarks.text = totalBookmarks.toString()
                }
        }
    }

    private fun buildUserList(snapshot: QuerySnapshot) {
        userListContainer.removeAllViews()

        if (snapshot.isEmpty) {
            val empty = TextView(this).apply {
                text = "No users found."
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(16, 16, 16, 16)
            }
            userListContainer.addView(empty)
            return
        }

        snapshot.documents.forEach { doc ->
            val uid      = doc.id
            val username = doc.getString("username") ?: "Unknown"
            val email    = doc.getString("email")    ?: "No email"
            val status   = doc.getString("status")   ?: "active"
            val role     = doc.getString("role")     ?: "user"

            if (role == "admin") return@forEach

            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_user_admin, userListContainer, false)

            itemView.findViewById<TextView>(R.id.user_display_name).text  = username
            itemView.findViewById<TextView>(R.id.user_display_email).text = email

            val tvStatus = itemView.findViewById<TextView>(R.id.tv_user_status)
            if (status == "suspended") {
                tvStatus.text = "Suspended"
                tvStatus.setBackgroundResource(R.drawable.btn_gradient_pink)
            } else {
                tvStatus.text = "Active"
                tvStatus.setBackgroundResource(R.drawable.card_gradient_blue)
            }

            itemView.findViewById<Button>(R.id.btn_edit_user).setOnClickListener {
                showEditDialog(uid, username, email)
            }

            val btnSuspend = itemView.findViewById<Button>(R.id.btn_suspend_user)
            btnSuspend.text = if (status == "suspended") "Unsuspend" else "Suspend"
            btnSuspend.setOnClickListener {
                val newStatus   = if (status == "suspended") "active" else "suspended"
                val actionLabel = if (newStatus == "suspended") "Suspend" else "Unsuspend"
                showConfirmDialog(
                    title       = "$actionLabel $username?",
                    message     = null,
                    confirmText = "Yes"
                ) {
                    db.collection("Users").document(uid)
                        .update("status", newStatus)
                        .addOnSuccessListener {
                            showSnackbar("Updated!")
                            loadDashboard()
                        }
                        .addOnFailureListener { e ->
                            showSnackbar("Error: ${e.message}")
                        }
                }
            }

            itemView.findViewById<androidx.cardview.widget.CardView>(R.id.btn_delete_user).setOnClickListener {
                showConfirmDialog(
                    title       = "Delete User",
                    message     = "Permanently delete $username? This cannot be undone.",
                    confirmText = "Delete"
                ) {
                    db.collection("Users").document(uid)
                        .delete()
                        .addOnSuccessListener {
                            showSnackbar("$username deleted.")
                            loadDashboard()
                        }
                        .addOnFailureListener { e ->
                            showSnackbar("Error: ${e.message}")
                        }
                }
            }

            userListContainer.addView(itemView)
        }
    }

    // ── Glass Edit Dialog ─────────────────────────────────────────────────────

    private fun showEditDialog(uid: String, currentUsername: String, currentEmail: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_user, null)
        val etUsername = dialogView.findViewById<EditText>(R.id.et_edit_username)
        val etEmail    = dialogView.findViewById<EditText>(R.id.et_edit_email)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnSave    = dialogView.findViewById<TextView>(R.id.btn_dialog_confirm)

        etUsername.setText(currentUsername)
        etEmail.setText(currentEmail)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newUsername = etUsername.text.toString().trim()
            val newEmail    = etEmail.text.toString().trim()
            if (newUsername.isEmpty()) {
                showSnackbar("Username cannot be empty.")
                return@setOnClickListener
            }
            db.collection("Users").document(uid)
                .update(mapOf("username" to newUsername, "email" to newEmail))
                .addOnSuccessListener {
                    showSnackbar("User updated!")
                    loadDashboard()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    showSnackbar("Error: ${e.message}")
                }
        }

        dialog.show()
    }

    // ── Glass Confirm Dialog ──────────────────────────────────────────────────

    private fun showConfirmDialog(
        title: String,
        message: String?,
        confirmText: String,
        onConfirm: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
        val tvTitle    = dialogView.findViewById<TextView>(R.id.tv_confirm_title)
        val tvMessage  = dialogView.findViewById<TextView>(R.id.tv_confirm_message)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_dialog_confirm)

        tvTitle.text    = title
        btnConfirm.text = confirmText

        if (message != null) {
            tvMessage.text       = message
            tvMessage.visibility = View.VISIBLE
        } else {
            tvMessage.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getTodayStart(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}