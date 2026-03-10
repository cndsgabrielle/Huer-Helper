package com.example.huerhelper

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import java.util.concurrent.Executors
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

class ScanActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isFrozen = false
    private var isSimpleMode = true
    private var cameraControl: CameraControl? = null

    private val cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private var currentHex = ""
    private var currentColorName = ""

    private lateinit var rootView: View

    companion object {
        private const val REQUEST_CODE_CAMERA = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        rootView = findViewById(android.R.id.content)

        cameraExecutor.execute { ColorNameFinder.init(this) }
        tts = TextToSpeech(this, this)

        if (allPermissionsGranted()) startCamera()
        else requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CODE_CAMERA)

        // ── Back ──────────────────────────────────────────────────────────────
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // ── Speak ─────────────────────────────────────────────────────────────
        findViewById<ImageButton>(R.id.btn_speak).setOnClickListener {
            if (currentColorName.isNotEmpty() && currentColorName != "Scanning...") {
                tts.speak(currentColorName, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        // ── Freeze ────────────────────────────────────────────────────────────
        findViewById<ImageButton>(R.id.btn_freeze).setOnClickListener {
            isFrozen = !isFrozen
            (it as ImageButton).imageAlpha = if (isFrozen) 128 else 255
            showSnackbar(if (isFrozen) "Paused" else "Live")
        }

        // ── Mode Toggle ───────────────────────────────────────────────────────
        val btnModeToggle = findViewById<TextView>(R.id.btn_mode_toggle)
        btnModeToggle.setOnClickListener {
            isSimpleMode = !isSimpleMode
            if (isSimpleMode) {
                btnModeToggle.text = "Detailed"
                showSnackbar("Simple Mode — basic color names")
            } else {
                btnModeToggle.text = "Simple"
                showSnackbar("Detailed Mode — precise color names")
            }
        }

        // ── Bookmark ──────────────────────────────────────────────────────────
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            if (currentHex.isEmpty() || currentColorName.isEmpty() || currentColorName == "Scanning...") {
                showSnackbar("Point at a color first!")
                return@setOnClickListener
            }
            val dialog = BookmarkDialog.newInstance(
                hex1       = currentHex,
                colorName1 = currentColorName
            )
            dialog.onSaved = {
                showSnackbar("Saved to bookmarks!")
            }
            dialog.show(supportFragmentManager, "bookmark")
        }

        // ── Zoom ──────────────────────────────────────────────────────────────
        findViewById<ImageButton>(R.id.btn_zoom_in).setOnClickListener {
            cameraControl?.setLinearZoom(0.5f)
            findViewById<TextView>(R.id.txt_zoom_level).text = "2x"
        }
        findViewById<ImageButton>(R.id.btn_zoom_out).setOnClickListener {
            cameraControl?.setLinearZoom(0.0f)
            findViewById<TextView>(R.id.txt_zoom_level).text = "1x"
        }
    }

    // ── Custom Snackbar ───────────────────────────────────────────────────────

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

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                if (!isFrozen) {
                    val plane   = imageProxy.planes[0]
                    val buffer  = plane.buffer
                    val centerX = imageProxy.width  / 2
                    val centerY = imageProxy.height / 2

                    var totalR = 0L; var totalG = 0L; var totalB = 0L
                    var count  = 0
                    val sampleSize = 25

                    for (dy in -sampleSize..sampleSize) {
                        for (dx in -sampleSize..sampleSize) {
                            val pos = ((centerY + dy) * plane.rowStride) + ((centerX + dx) * plane.pixelStride)
                            if (pos + 2 < buffer.capacity()) {
                                totalR += buffer.get(pos).toLong()     and 0xFF
                                totalG += buffer.get(pos + 1).toLong() and 0xFF
                                totalB += buffer.get(pos + 2).toLong() and 0xFF
                                count++
                            }
                        }
                    }

                    if (count > 0) {
                        val r = (totalR / count).toInt()
                        val g = (totalG / count).toInt()
                        val b = (totalB / count).toInt()

                        val hex = String.format("#%02X%02X%02X", r, g, b)

                        val colorName = if (isSimpleMode)
                            ColorNameFinder.getSimpleColorName(r, g, b)
                        else
                            ColorNameFinder.getColorName(r, g, b)

                        currentHex       = hex
                        currentColorName = colorName

                        runOnUiThread {
                            findViewById<TextView>(R.id.txt_color_name).text    = colorName
                            findViewById<TextView>(R.id.txt_color_details).text = "$hex • RGB($r, $g, $b)"
                            findViewById<ImageView>(R.id.center_crosshair).setColorFilter(Color.rgb(r, g, b))
                        }
                    }
                }
                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
                cameraControl = camera.cameraControl
            } catch (exc: Exception) {
                showSnackbar("Camera failed to start: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        cameraExecutor.shutdown()
    }
}