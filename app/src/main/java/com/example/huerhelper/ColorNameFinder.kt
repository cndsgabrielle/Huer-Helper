package com.example.huerhelper

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.pow
import kotlin.math.sqrt

data class ColorEntry(val name: String, val hex: String)
data class LabColor(val name: String, val L: Double, val a: Double, val b: Double)

object ColorNameFinder {

    private var colorDataset: List<LabColor>? = null

    fun isReady(): Boolean = colorDataset != null

    fun init(context: Context) {
        if (colorDataset != null) return
        try {
            val jsonString = context.assets.open("colors.json")
                .bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<ColorEntry>>() {}.type
            val rawList: List<ColorEntry> = Gson().fromJson(jsonString, type)
            colorDataset = rawList.map { entry ->
                val colorInt = Color.parseColor(entry.hex)
                val r = Color.red(colorInt)
                val g = Color.green(colorInt)
                val b = Color.blue(colorInt)
                val (L, a, lab_b) = rgbToLab(r, g, b)
                LabColor(entry.name, L, a, lab_b)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            colorDataset = emptyList()
        }
    }

    // ─── Detailed Mode — Delta-E LAB matching against colors.json ────────────

    fun getColorName(r: Int, g: Int, b: Int): String {
        val dataset = colorDataset ?: return "Loading..."
        if (dataset.isEmpty()) return "Dataset Error"
        val (inputL, inputA, inputB) = rgbToLab(r, g, b)
        val best = dataset.minByOrNull { color ->
            deltaE(inputL, inputA, inputB, color.L, color.a, color.b)
        }?.name ?: "Unknown"

        val rDiff = maxOf(r, g, b) - minOf(r, g, b)
        val isWarmDominant = r > b && r > g        // red channel strongest = warm tone
        val hasWarmHue = r > (b + 10) && r >= g   // red noticeably above blue = brownish

        // Gray in dim light → could be White
        if (best == "Gray") {
            if (rDiff <= 15 && inputL > 45.0) return "White"
            // Gray but warm-toned → Light Brown or Beige
            if (hasWarmHue && inputL in 35.0..60.0) return "Light Brown"
        }

        // Black but has warm hue → Dark Brown
        if (best == "Black" && hasWarmHue && inputL > 8.0) return "Dark Brown"

        // Dark Gray but warm → Brown
        if (best == "Dark Gray" && hasWarmHue) return "Brown"

        return best
    }

    // ─── Simple Mode — HSL range matching ────────────────────────────────────

    fun getSimpleColorName(r: Int, g: Int, b: Int): String {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val l = (max + min) / 2f
        val s = if (delta == 0f) 0f
        else delta / (1f - kotlin.math.abs(2f * l - 1f))

        val hRaw = when {
            delta == 0f -> 0f
            max == rf   -> 60f * (((gf - bf) / delta) % 6f)
            max == gf   -> 60f * (((bf - rf) / delta) + 2f)
            else        -> 60f * (((rf - gf) / delta) + 4f)
        }
        val h = if (hRaw < 0) hRaw + 360f else hRaw

        // ── Achromatic: Black / Gray / White ──────────────────────────────────
        // But first: low-saturation warm hues (browns/tans) should not be called Gray
        if (s < 0.20f) {
            val isWarmHue = h in 15f..55f
            return when {
                l < 0.25f            -> "Black"
                isWarmHue && l < 0.45f -> "Brown"
                isWarmHue            -> "Beige"
                l < 0.55f            -> "Gray"
                else                 -> "White"
            }
        }

        // ── Near-neutral warm tones: Beige / Cream ────────────────────────────
        if (s < 0.30f && l > 0.70f && h in 20f..60f) return "Cream"
        if (s < 0.35f && l in 0.55f..0.80f && h in 20f..55f) return "Beige"

        // ── Chromatic colors ──────────────────────────────────────────────────
        return when {
            h < 10f || h >= 345f -> if (l < 0.30f) "Brown" else "Red"

            h in 10f..20f -> when {
                l < 0.40f -> "Brown"
                else      -> "Red"
            }

            h in 20f..45f -> when {
                l < 0.40f -> "Brown"
                else      -> "Orange"
            }

            h in 45f..65f -> when {
                l < 0.35f               -> "Brown"
                l < 0.45f && s < 0.35f -> "Brown"
                l > 0.80f               -> "Cream"
                else                    -> "Yellow"
            }

            h in 65f..90f -> "Green"

            h in 90f..150f -> "Green"

            h in 150f..195f ->
                if (l < 0.30f) "Green" else "Teal"

            h in 195f..250f -> when {
                l > 0.70f -> "Light Blue"
                else      -> "Blue"
            }

            h in 250f..270f -> "Blue"

            h in 270f..305f -> "Purple"

            h in 305f..345f -> "Pink"

            else -> "Unknown"
        }
    }

    // ─── Color Math ──────────────────────────────────────────────────────────

    private fun deltaE(
        L1: Double, a1: Double, b1: Double,
        L2: Double, a2: Double, b2: Double
    ): Double = sqrt(
        (L1 - L2).pow(2) + (a1 - a2).pow(2) + (b1 - b2).pow(2)
    )

    private fun rgbToLab(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        val rLin = srgbToLinear(r / 255.0)
        val gLin = srgbToLinear(g / 255.0)
        val bLin = srgbToLinear(b / 255.0)

        val x = rLin * 0.4124564 + gLin * 0.3575761 + bLin * 0.1804375
        val y = rLin * 0.2126729 + gLin * 0.7151522 + bLin * 0.0721750
        val z = rLin * 0.0193339 + gLin * 0.1191920 + bLin * 0.9503041

        val fx = xyzToLab(x / 0.95047)
        val fy = xyzToLab(y / 1.00000)
        val fz = xyzToLab(z / 1.08883)

        return Triple(
            116.0 * fy - 16.0,
            500.0 * (fx - fy),
            200.0 * (fy - fz)
        )
    }

    private fun srgbToLinear(c: Double): Double =
        if (c <= 0.04045) c / 12.92
        else ((c + 0.055) / 1.055).pow(2.4)

    private fun xyzToLab(t: Double): Double {
        val delta = 6.0 / 29.0
        return if (t > delta.pow(3)) t.pow(1.0 / 3.0)
        else t / (3.0 * delta.pow(2)) + 4.0 / 29.0
    }
}