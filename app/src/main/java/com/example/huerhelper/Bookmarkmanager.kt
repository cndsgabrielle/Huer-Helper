package com.example.huerhelper

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Bookmark(
    val id: String = "",
    val type: String = "scanned",       // "scanned" or "matched"
    val label: String = "",
    val tags: List<String> = emptyList(),
    val hex1: String = "",
    val hex2: String = "",              // empty for scanned
    val colorName1: String = "",
    val colorName2: String = "",        // empty for scanned
    val savedAt: com.google.firebase.Timestamp? = null
)

object BookmarkManager {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun userId(): String? = auth.currentUser?.uid

    private fun collection() = userId()?.let {
        db.collection("Users").document(it).collection("bookmarks")
    }

    /** Exposed so callers can show a friendly message if not logged in. */
    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun currentUserId(): String? = auth.currentUser?.uid

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveScanned(
        hex: String,
        colorName: String,
        label: String,
        tags: List<String>,
        onSuccess: (bookmarkId: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val col = collection() ?: return onError(Exception("Not logged in"))
        val data = hashMapOf(
            "type"       to "scanned",
            "label"      to label,
            "tags"       to tags,
            "hex1"       to hex,
            "hex2"       to "",
            "colorName1" to colorName,
            "colorName2" to "",
            "savedAt"    to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        col.add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it) }
    }

    fun saveMatched(
        hex1: String, colorName1: String,
        hex2: String, colorName2: String,
        label: String,
        tags: List<String>,
        onSuccess: (bookmarkId: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val col = collection() ?: return onError(Exception("Not logged in"))
        val data = hashMapOf(
            "type"       to "matched",
            "label"      to label,
            "tags"       to tags,
            "hex1"       to hex1,
            "hex2"       to hex2,
            "colorName1" to colorName1,
            "colorName2" to colorName2,
            "savedAt"    to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        col.add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it) }
    }

    // ── Fetch ─────────────────────────────────────────────────────────────────

    fun fetchAll(
        onSuccess: (List<Bookmark>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val col = collection() ?: return onError(Exception("Not logged in"))
        col.orderBy("savedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Bookmark(
                            id         = doc.id,
                            type       = doc.getString("type") ?: "scanned",
                            label      = doc.getString("label") ?: "",
                            tags       = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            hex1       = doc.getString("hex1") ?: "",
                            hex2       = doc.getString("hex2") ?: "",
                            colorName1 = doc.getString("colorName1") ?: "",
                            colorName2 = doc.getString("colorName2") ?: "",
                            savedAt    = doc.getTimestamp("savedAt")
                        )
                    } catch (e: Exception) { null }
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    // ── Update label + tags ───────────────────────────────────────────────────

    fun update(
        bookmarkId: String,
        newLabel: String,
        newTags: List<String>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val col = collection() ?: return onError(Exception("Not logged in"))
        col.document(bookmarkId)
            .update(mapOf("label" to newLabel, "tags" to newTags))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun delete(
        bookmarkId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val col = collection() ?: return onError(Exception("Not logged in"))
        col.document(bookmarkId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}