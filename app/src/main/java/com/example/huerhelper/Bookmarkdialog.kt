package com.example.huerhelper

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class BookmarkDialog : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_BOOKMARK_ID = "bookmark_id"
        private const val ARG_LABEL       = "label"
        private const val ARG_TAGS        = "tags"
        private const val ARG_HEX1        = "hex1"
        private const val ARG_HEX2        = "hex2"
        private const val ARG_NAME1       = "name1"
        private const val ARG_NAME2       = "name2"
        private const val ARG_TYPE        = "type"

        fun newInstance(
            hex1: String,
            colorName1: String,
            hex2: String = "",
            colorName2: String = "",
            bookmarkId: String = "",
            initialLabel: String = "",
            initialTags: List<String> = emptyList()
        ): BookmarkDialog {
            val type = if (hex2.isNotEmpty()) "matched" else "scanned"
            return BookmarkDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKMARK_ID, bookmarkId)
                    putString(ARG_LABEL,       initialLabel)
                    putStringArrayList(ARG_TAGS, ArrayList(initialTags))
                    putString(ARG_HEX1,  hex1)
                    putString(ARG_HEX2,  hex2)
                    putString(ARG_NAME1, colorName1)
                    putString(ARG_NAME2, colorName2)
                    putString(ARG_TYPE,  type)
                }
            }
        }
    }

    var onSaved: (() -> Unit)? = null
    private val tags = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_bookmark, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Remove default gray bottom sheet background and shadow ─────────────
        dialog?.setOnShowListener {
            val bottomSheet = (it as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }

        val args       = requireArguments()
        val bookmarkId = args.getString(ARG_BOOKMARK_ID, "")
        val hex1       = args.getString(ARG_HEX1, "#808080")
        val hex2       = args.getString(ARG_HEX2, "")
        val colorName1 = args.getString(ARG_NAME1, "")
        val colorName2 = args.getString(ARG_NAME2, "")
        val type       = args.getString(ARG_TYPE, "scanned")
        val isEdit     = bookmarkId.isNotEmpty()

        tags.addAll(args.getStringArrayList(ARG_TAGS) ?: emptyList())

        // ── Swatches ──────────────────────────────────────────────────────────
        val swatch1: View    = view.findViewById(R.id.dialog_swatch1)
        val swatch2: View    = view.findViewById(R.id.dialog_swatch2)
        val swatch2Container = view.findViewById<View>(R.id.dialog_swatch2_container)

        try { swatch1.setBackgroundColor(Color.parseColor(hex1)) } catch (_: Exception) {}

        if (type == "matched" && hex2.isNotEmpty()) {
            swatch2Container.visibility = View.VISIBLE
            try { swatch2.setBackgroundColor(Color.parseColor(hex2)) } catch (_: Exception) {}
        } else {
            swatch2Container.visibility = View.GONE
        }

        // ── Label ─────────────────────────────────────────────────────────────
        val etLabel: EditText = view.findViewById(R.id.et_label)
        val initialLabel = args.getString(ARG_LABEL, "")
        etLabel.setText(
            if (initialLabel.isNotEmpty()) initialLabel
            else if (type == "matched") "$colorName1 + $colorName2"
            else colorName1
        )

        // ── Tags ──────────────────────────────────────────────────────────────
        val chipGroup: ChipGroup = view.findViewById(R.id.chip_group_tags)
        val etTag: EditText      = view.findViewById(R.id.et_new_tag)
        val btnAddTag: View      = view.findViewById(R.id.btn_add_tag)

        tags.forEach { addChip(chipGroup, it) }

        btnAddTag.setOnClickListener {
            val tag = etTag.text.toString().trim().lowercase()
            if (tag.isNotEmpty() && !tags.contains(tag)) {
                tags.add(tag)
                addChip(chipGroup, tag)
                etTag.text.clear()
            }
        }

        // ── Buttons ───────────────────────────────────────────────────────────
        val btnSave: MaterialButton   = view.findViewById(R.id.btn_dialog_save)
        val btnCancel: MaterialButton = view.findViewById(R.id.btn_dialog_cancel)

        btnSave.text = if (isEdit) "Update" else "Save"

        btnSave.setOnClickListener {
            val label = etLabel.text.toString().trim().ifEmpty {
                if (type == "matched") "$colorName1 + $colorName2" else colorName1
            }

            if (isEdit) {
                BookmarkManager.update(
                    bookmarkId = bookmarkId,
                    newLabel   = label,
                    newTags    = tags.toList(),
                    onSuccess  = { onSaved?.invoke(); dismiss() },
                    onError    = { e -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                )
            } else {
                if (type == "matched") {
                    BookmarkManager.saveMatched(
                        hex1       = hex1,       colorName1 = colorName1,
                        hex2       = hex2,       colorName2 = colorName2,
                        label      = label,
                        tags       = tags.toList(),
                        onSuccess  = { onSaved?.invoke(); dismiss() },
                        onError    = { e -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    BookmarkManager.saveScanned(
                        hex       = hex1,
                        colorName  = colorName1,
                        label      = label,
                        tags       = tags.toList(),
                        onSuccess  = { onSaved?.invoke(); dismiss() },
                        onError    = { e -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }

        btnCancel.setOnClickListener { dismiss() }
    }

    private fun addChip(group: ChipGroup, text: String) {
        val chip = Chip(requireContext()).apply {
            this.text = text
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                tags.remove(text)
                group.removeView(this)
            }
        }
        group.addView(chip)
    }
}