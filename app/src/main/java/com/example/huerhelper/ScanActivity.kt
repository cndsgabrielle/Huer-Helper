package com.example.huerhelper

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.content.Intent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Class-level variables for access across all functions
    private lateinit var tts: TextToSpeech
    private var isFrozen = false
    private var cameraControl: CameraControl? = null

    private fun allPermissionsGranted() = arrayOf(android.Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(baseContext, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Initialize Text-to-Speech and Camera
        tts = TextToSpeech(this, this)
        startCamera()

        // --- UI Click Listeners ---

        // Back Button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // Speak Button: Announces the identified color
        findViewById<ImageButton>(R.id.btn_speak).setOnClickListener {
            val colorName = findViewById<TextView>(R.id.txt_color_name).text.toString()
            if (colorName.isNotEmpty()) {
                tts.speak(colorName, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        // Save Button: Bookmark the current color
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            Toast.makeText(this, "Color saved to Bookmarks!", Toast.LENGTH_SHORT).show()
        }

        // Zoom Controls: Links to the camera lens
        findViewById<ImageButton>(R.id.btn_zoom_in).setOnClickListener {
            cameraControl?.setLinearZoom(0.5f) // Zoom to 50%
            findViewById<TextView>(R.id.txt_zoom_level).text = "2x"
        }

        findViewById<ImageButton>(R.id.btn_zoom_out).setOnClickListener {
            cameraControl?.setLinearZoom(0.0f) // Reset to 1x
            findViewById<TextView>(R.id.txt_zoom_level).text = "1x"
        }

        // Freeze Button: Pauses the live updates on screen
        findViewById<ImageButton>(R.id.btn_freeze).setOnClickListener {
            isFrozen = !isFrozen
            val status = if (isFrozen) "Paused" else "Live"
            Toast.makeText(this, "Detection $status", Toast.LENGTH_SHORT).show()
        }
        // Inside onCreate, before startCamera()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 10)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 1. Preview Setup
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
                }

            // 2. Image Analyzer Setup (RGBA_8888 for easy color reading)
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                if (!isFrozen) {
                    val plane = imageProxy.planes[0]
                    val buffer = plane.buffer

                    // Find the center pixel coordinates
                    val centerX = imageProxy.width / 2
                    val centerY = imageProxy.height / 2

                    // Calculate memory position
                    val pixelPos = (centerY * plane.rowStride) + (centerX * plane.pixelStride)

                    // Extract RGB values
                    val r = buffer.get(pixelPos).toInt() and 0xFF
                    val g = buffer.get(pixelPos + 1).toInt() and 0xFF
                    val b = buffer.get(pixelPos + 2).toInt() and 0xFF

                    val hex = String.format("#%02X%02X%02X", r, g, b)
                    val colorName = getBasicColorName(r, g, b)

                    // Update UI
                    runOnUiThread {
                        findViewById<TextView>(R.id.txt_color_name).text = colorName
                        findViewById<TextView>(R.id.txt_color_details).text = "$hex • RGB($r, $g, $b)"
                    }
                }
                imageProxy.close() // Release frame
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // Bind BOTH preview and analyzer at once
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

                // Save the control object to our class variable
                cameraControl = camera.cameraControl
            } catch(exc: Exception) {
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getBasicColorName(r: Int, g: Int, b: Int): String {
        val basicColors = mapOf(
            "Red" to Triple(255, 0, 0),
            "Green" to Triple(0, 255, 0),
            "Blue" to Triple(0, 0, 255),
            "White" to Triple(255, 255, 255),
            "Black" to Triple(0, 0, 0),
            "Yellow" to Triple(255, 255, 0),
            "Sky Blue" to Triple(135, 206, 235)
        )

        return basicColors.minByOrNull { (_, rgb) ->
            val dr = r - rgb.first
            val dg = g - rgb.second
            val db = b - rgb.third
            dr * dr + dg * dg + db * db
        }?.key ?: "Unknown"
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}