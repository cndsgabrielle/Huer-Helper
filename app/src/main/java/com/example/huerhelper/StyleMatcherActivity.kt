package com.example.huerhelper

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StyleMatcherActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COLOR1_R = "color1_r"
        const val EXTRA_COLOR1_G = "color1_g"
        const val EXTRA_COLOR1_B = "color1_b"
        const val EXTRA_COLOR2_R = "color2_r"
        const val EXTRA_COLOR2_G = "color2_g"
        const val EXTRA_COLOR2_B = "color2_b"
    }

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var rootView: View

    private lateinit var viewFinder1: PreviewView
    private lateinit var viewFinder2: PreviewView
    private lateinit var snapshot1: ImageView
    private lateinit var snapshot2: ImageView
    private lateinit var slot1CapturedLabel: TextView
    private lateinit var slot2Placeholder: View
    private lateinit var slot2PlaceholderLabel: TextView
    private lateinit var btnCapture1: Button
    private lateinit var btnGallery1: Button
    private lateinit var btnCapture2: Button
    private lateinit var btnGallery2: Button
    private lateinit var btnGroup1: LinearLayout
    private lateinit var btnGroup2: LinearLayout

    private var capturedColor1: IntArray? = null

    @Volatile private var latestR = 128
    @Volatile private var latestG = 128
    @Volatile private var latestB = 128

    private var cameraProvider: ProcessCameraProvider? = null

    private val gallerySlot1Launcher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            showSnackbar("No image selected.")
            return@registerForActivityResult
        }

        // Run bitmap loading off the main thread to avoid ANR on large images
        Thread {
            val bmp = getBitmapFromUri(uri)
            if (bmp == null) {
                runOnUiThread { showSnackbar("Failed to load image. Try another photo.") }
                return@Thread
            }
            capturedColor1 = averageColorFromBitmap(bmp)

            runOnUiThread {
                snapshot1.setImageBitmap(bmp)
                snapshot1.visibility   = View.VISIBLE
                viewFinder1.visibility = View.GONE
                onSlot1Captured()
            }
        }.start()
    }

    private val gallerySlot2Launcher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            showSnackbar("No image selected.")
            return@registerForActivityResult
        }
        val c1 = capturedColor1 ?: run {
            showSnackbar("Please capture the first item first.")
            return@registerForActivityResult
        }

        // Run bitmap loading off the main thread to avoid ANR on large images
        Thread {
            val bmp = getBitmapFromUri(uri)
            if (bmp == null) {
                runOnUiThread { showSnackbar("Failed to load image. Try another photo.") }
                return@Thread
            }
            val c2 = averageColorFromBitmap(bmp)

            runOnUiThread {
                snapshot2.setImageBitmap(bmp)
                snapshot2.visibility   = View.VISIBLE
                viewFinder2.visibility = View.GONE
                btnGroup2.visibility   = View.GONE
                navigateToResults(c1, c2)
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_style_matcher)

        rootView       = findViewById(android.R.id.content)
        cameraExecutor = Executors.newSingleThreadExecutor()

        viewFinder1           = findViewById(R.id.viewFinder1)
        viewFinder2           = findViewById(R.id.viewFinder2)
        snapshot1             = findViewById(R.id.snapshot1)
        snapshot2             = findViewById(R.id.snapshot2)
        slot1CapturedLabel    = findViewById(R.id.slot1_captured_label)
        slot2Placeholder      = findViewById(R.id.slot2_color_fill)
        slot2PlaceholderLabel = findViewById(R.id.slot2_captured_label)
        btnCapture1           = findViewById(R.id.btn_capture_first)
        btnGallery1           = findViewById(R.id.btn_gallery_first)
        btnCapture2           = findViewById(R.id.btn_capture_second)
        btnGallery2           = findViewById(R.id.btn_gallery_second)
        btnGroup1             = findViewById(R.id.btn_group_1)
        btnGroup2             = findViewById(R.id.btn_group_2)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        resetToSlot1()
        startCamera(viewFinder1)

        btnCapture1.setOnClickListener {
            capturedColor1 = intArrayOf(latestR, latestG, latestB)
            val bmp = viewFinder1.bitmap
            if (bmp != null) {
                snapshot1.setImageBitmap(bmp)
                snapshot1.visibility   = View.VISIBLE
                viewFinder1.visibility = View.GONE
            }
            onSlot1Captured()
        }

        btnGallery1.setOnClickListener {
            gallerySlot1Launcher.launch("image/*")
        }

        btnCapture2.setOnClickListener {
            val c1 = capturedColor1 ?: run {
                showSnackbar("Please capture the first item first.")
                return@setOnClickListener
            }
            val bmp = viewFinder2.bitmap
            if (bmp != null) {
                snapshot2.setImageBitmap(bmp)
                snapshot2.visibility   = View.VISIBLE
                viewFinder2.visibility = View.GONE
            }
            navigateToResults(c1, intArrayOf(latestR, latestG, latestB))
        }

        btnGallery2.setOnClickListener {
            if (capturedColor1 == null) {
                showSnackbar("Please capture the first item first.")
                return@setOnClickListener
            }
            gallerySlot2Launcher.launch("image/*")
        }
    }

    private fun onSlot1Captured() {
        slot1CapturedLabel.visibility    = View.VISIBLE
        slot1CapturedLabel.text          = "✓ First item captured"
        btnGroup1.visibility             = View.GONE
        slot2Placeholder.visibility      = View.GONE
        slot2PlaceholderLabel.visibility = View.GONE
        viewFinder2.visibility           = View.VISIBLE
        btnGroup2.visibility             = View.VISIBLE
        latestR = 128; latestG = 128; latestB = 128
        bindCamera(viewFinder2)
        showSnackbar("First item captured! Now pick the second item.")
    }

    private fun navigateToResults(c1: IntArray, c2: IntArray) {
        val intent = Intent(this, MatchResultsActivity::class.java).apply {
            putExtra(EXTRA_COLOR1_R, c1[0]); putExtra(EXTRA_COLOR1_G, c1[1]); putExtra(EXTRA_COLOR1_B, c1[2])
            putExtra(EXTRA_COLOR2_R, c2[0]); putExtra(EXTRA_COLOR2_G, c2[1]); putExtra(EXTRA_COLOR2_B, c2[2])
        }
        startActivity(intent)
    }

    private fun averageColorFromBitmap(bmp: Bitmap): IntArray {
        val cx   = bmp.width  / 2
        val cy   = bmp.height / 2
        val half = 50
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0

        for (dy in -half..half) {
            for (dx in -half..half) {
                val pixel = bmp.getPixel(
                    (cx + dx).coerceIn(0, bmp.width  - 1),
                    (cy + dy).coerceIn(0, bmp.height - 1)
                )
                rSum += Color.red(pixel)
                gSum += Color.green(pixel)
                bSum += Color.blue(pixel)
                count++
            }
        }

        return if (count > 0) intArrayOf(
            (rSum / count).toInt(),
            (gSum / count).toInt(),
            (bSum / count).toInt()
        ) else intArrayOf(128, 128, 128)
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            // First pass: decode bounds only to get dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate sample size to keep image under ~1024px
            val maxSize = 1024
            var sampleSize = 1
            var width = options.outWidth
            var height = options.outHeight
            while (width > maxSize || height > maxSize) {
                sampleSize *= 2
                width /= 2
                height /= 2
            }

            // Second pass: decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            runOnUiThread { showSnackbar("Image error: ${e.message}") }
            null
        }
    }

    private fun showSnackbar(message: String) {
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
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

    private fun resetToSlot1() {
        viewFinder1.visibility        = View.VISIBLE
        snapshot1.visibility          = View.GONE
        slot1CapturedLabel.visibility = View.GONE
        btnGroup1.visibility          = View.VISIBLE
        viewFinder2.visibility           = View.GONE
        snapshot2.visibility             = View.GONE
        slot2Placeholder.visibility      = View.VISIBLE
        slot2PlaceholderLabel.visibility = View.VISIBLE
        btnGroup2.visibility             = View.GONE
    }

    private fun startCamera(targetView: PreviewView) {
        ProcessCameraProvider.getInstance(this).addListener({
            cameraProvider = ProcessCameraProvider.getInstance(this).get()
            bindCamera(targetView)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera(targetView: PreviewView) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(targetView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    averageColor(imageProxy)
                    imageProxy.close()
                }
            }

        try {
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
        } catch (e: Exception) {
            showSnackbar("Camera error: ${e.message}")
        }
    }

    private fun averageColor(imageProxy: androidx.camera.core.ImageProxy) {
        val plane  = imageProxy.planes[0]
        val buffer = plane.buffer
        val bytes  = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width       = imageProxy.width
        val height      = imageProxy.height
        val rowStride   = plane.rowStride
        val pixelStride = plane.pixelStride

        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0

        val gridSize = 50
        val startX   = width  / 2 - gridSize / 2
        val startY   = height / 2 - gridSize / 2

        for (dy in 0 until gridSize) {
            for (dx in 0 until gridSize) {
                val x = (startX + dx).coerceIn(0, width  - 1)
                val y = (startY + dy).coerceIn(0, height - 1)
                val idx = y * rowStride + x * pixelStride
                if (idx + 2 < bytes.size) {
                    rSum += bytes[idx].toInt()     and 0xFF
                    gSum += bytes[idx + 1].toInt() and 0xFF
                    bSum += bytes[idx + 2].toInt() and 0xFF
                    count++
                }
            }
        }

        if (count > 0) {
            latestR = (rSum / count).toInt()
            latestG = (gSum / count).toInt()
            latestB = (bSum / count).toInt()
        }
    }

    override fun onResume() {
        super.onResume()
        capturedColor1 = null
        latestR = 128; latestG = 128; latestB = 128
        resetToSlot1()

        val provider = cameraProvider
        if (provider != null) bindCamera(viewFinder1)
        else startCamera(viewFinder1)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}