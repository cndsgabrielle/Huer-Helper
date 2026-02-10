package com.example.huerhelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent

class StyleMatcherActivity : AppCompatActivity() {

    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_style_matcher)

        // 1. Setup Permissions
        if (allPermissionsGranted()) {
            setupCameraProvider()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 10)
        }

        // 2. Back Button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // 3. Capture First Button Logic
        findViewById<Button>(R.id.btn_capture_first).setOnClickListener {
            Toast.makeText(this, "First Item Captured!", Toast.LENGTH_SHORT).show()
            startSecondCamera()
        }

        // 4. Capture Second Button Logic (Transitions to Results)
        findViewById<Button>(R.id.btn_capture_second).setOnClickListener {
            Toast.makeText(this, "Analyzing Match...", Toast.LENGTH_SHORT).show()

            // Navigate to Match Results Screen
            val intent = Intent(this, MatchResultsActivity::class.java)

            // You can pass the color data here if you have it
            // intent.putExtra("COLOR_1", "#60A5FA")
            // intent.putExtra("COLOR_2", "#FFFFFF")

            startActivity(intent)
        }
    }

    private fun setupCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            startFirstCamera() // Only start the first one initially
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFirstCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder1).surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview)
        } catch (exc: Exception) {
            Toast.makeText(this, "Failed to bind First Camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSecondCamera() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder2).surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            // We don't unbind the first one so you can still see the result,
            // but we bind the new preview for the second box
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview)
        } catch (exc: Exception) {
            Toast.makeText(this, "Failed to bind Second Camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}