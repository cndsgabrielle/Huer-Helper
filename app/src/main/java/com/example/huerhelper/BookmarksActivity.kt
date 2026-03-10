package com.example.huerhelper

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*

class BookmarksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var btnAll: TextView
    private lateinit var btnScanned: TextView
    private lateinit var btnMatched: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private var allBookmarks = listOf<Bookmark>()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        recyclerView = findViewById(R.id.rv_bookmarks)
        tvCount      = findViewById(R.id.tv_bookmark_count)
        btnAll       = findViewById(R.id.btn_filter_all)
        btnScanned   = findViewById(R.id.btn_filter_scanned)
        btnMatched   = findViewById(R.id.btn_filter_matched)
        progressBar  = findViewById(R.id.progress_bar)
        tvEmpty      = findViewById(R.id.tv_empty)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val sparkle = findViewById<ImageView>(R.id.sparkle_icon_bookmarks)
        sparkle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        btnAll.setOnClickListener     { setFilter("all") }
        btnScanned.setOnClickListener { setFilter("scanned") }
        btnMatched.setOnClickListener { setFilter("matched") }

        loadBookmarks()
    }

    private fun loadBookmarks() {
        showLoading(true)
        BookmarkManager.fetchAll(
            onSuccess = { list ->
                allBookmarks = list
                setFilter(currentFilter)
                showLoading(false)
            },
            onError = { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setFilter(filter: String) {
        currentFilter = filter

        val activeBackground   = R.drawable.card_gradient_blue
        val inactiveBackground = R.drawable.glass_button_bg
        btnAll.setBackgroundResource    (if (filter == "all")     activeBackground else inactiveBackground)
        btnScanned.setBackgroundResource(if (filter == "scanned") activeBackground else inactiveBackground)
        btnMatched.setBackgroundResource(if (filter == "matched") activeBackground else inactiveBackground)

        val filtered = when (filter) {
            "scanned" -> allBookmarks.filter { it.type == "scanned" }
            "matched" -> allBookmarks.filter { it.type == "matched" }
            else      -> allBookmarks
        }

        val total   = allBookmarks.size
        val scanned = allBookmarks.count { it.type == "scanned" }
        val matched = allBookmarks.count { it.type == "matched" }
        tvCount.text = "$total saved color${if (total != 1) "s" else ""}"

        btnAll.text     = "All ($total)"
        btnScanned.text = "Scanned ($scanned)"
        btnMatched.text = "Matched ($matched)"

        if (filtered.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility      = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility      = View.GONE
            recyclerView.adapter    = BookmarkAdapter(filtered)
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility  = if (loading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (loading) View.GONE    else View.VISIBLE
    }

    // ── Glass Delete Dialog ───────────────────────────────────────────────────

    private fun showDeleteDialog(bookmarkId: String, label: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
        val tvTitle    = dialogView.findViewById<TextView>(R.id.tv_confirm_title)
        val tvMessage  = dialogView.findViewById<TextView>(R.id.tv_confirm_message)
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_dialog_confirm)

        tvTitle.text    = "Delete Bookmark"
        tvMessage.text  = "Remove \"$label\"? This cannot be undone."
        tvMessage.visibility = View.VISIBLE
        btnConfirm.text = "Delete"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            BookmarkManager.delete(
                bookmarkId = bookmarkId,
                onSuccess  = { loadBookmarks() },
                onError    = { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────────────

    inner class BookmarkAdapter(private val items: List<Bookmark>)
        : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val swatch1: View          = view.findViewById(R.id.swatch1)
            val swatch2: View          = view.findViewById(R.id.swatch2)
            val tvBadge: TextView      = view.findViewById(R.id.tv_type_badge)
            val tvLabel: TextView      = view.findViewById(R.id.tv_label)
            val tvDetails: TextView    = view.findViewById(R.id.tv_details)
            val tvDate: TextView       = view.findViewById(R.id.tv_date)
            val chipGroup: ChipGroup   = view.findViewById(R.id.chip_group_item_tags)
            val btnEdit: ImageButton   = view.findViewById(R.id.btn_edit)
            val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bookmark, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val b = items[position]

            try { holder.swatch1.setBackgroundColor(Color.parseColor(b.hex1)) } catch (_: Exception) {}

            if (b.type == "matched" && b.hex2.isNotEmpty()) {
                holder.swatch2.visibility = View.VISIBLE
                try { holder.swatch2.setBackgroundColor(Color.parseColor(b.hex2)) } catch (_: Exception) {}
            } else {
                holder.swatch2.visibility = View.GONE
            }

            if (b.type == "matched") {
                holder.tvBadge.text = "Matched"
                holder.tvBadge.setBackgroundResource(R.drawable.btn_gradient_purple)
            } else {
                holder.tvBadge.text = "Scanned"
                holder.tvBadge.setBackgroundResource(R.drawable.card_gradient_blue)
            }

            holder.tvLabel.text = b.label.ifEmpty {
                if (b.type == "matched") "${b.colorName1} + ${b.colorName2}" else b.colorName1
            }

            holder.tvDetails.text = if (b.type == "matched") {
                "${b.colorName1} (${b.hex1})\n+ ${b.colorName2} (${b.hex2})"
            } else {
                "${b.hex1}"
            }

            holder.chipGroup.removeAllViews()
            if (b.tags.isNotEmpty()) {
                holder.chipGroup.visibility = View.VISIBLE
                b.tags.forEach { tag ->
                    val chip = Chip(holder.chipGroup.context).apply {
                        text = tag
                        isClickable = false
                        isCheckable = false
                        textSize = 10f
                    }
                    holder.chipGroup.addView(chip)
                }
            } else {
                holder.chipGroup.visibility = View.GONE
            }

            holder.tvDate.text = b.savedAt?.toDate()?.let {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
            } ?: ""

            holder.btnEdit.setOnClickListener {
                val dialog = BookmarkDialog.newInstance(
                    hex1         = b.hex1,
                    colorName1   = b.colorName1,
                    hex2         = b.hex2,
                    colorName2   = b.colorName2,
                    bookmarkId   = b.id,
                    initialLabel = b.label,
                    initialTags  = b.tags
                )
                dialog.onSaved = { loadBookmarks() }
                dialog.show(supportFragmentManager, "edit_bookmark")
            }

            // ── Delete — now uses glass dialog ────────────────────────────────
            holder.btnDelete.setOnClickListener {
                showDeleteDialog(
                    bookmarkId = b.id,
                    label      = b.label.ifEmpty { b.colorName1 }
                )
            }
        }
    }
}