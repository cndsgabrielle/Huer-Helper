package com.example.huerhelper

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

class MatchResultsActivity : AppCompatActivity() {

    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_results)

        rootView = findViewById(android.R.id.content)

        // ── Sparkle animation ─────────────────────────────────────────────────
        val resultSparkle: ImageView = findViewById(R.id.result_sparkle)
        resultSparkle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_glow))

        // ── Read colors ───────────────────────────────────────────────────────
        val r1 = intent.getIntExtra(StyleMatcherActivity.EXTRA_COLOR1_R, 96)
        val g1 = intent.getIntExtra(StyleMatcherActivity.EXTRA_COLOR1_G, 165)
        val b1 = intent.getIntExtra(StyleMatcherActivity.EXTRA_COLOR1_B, 250)

        val r2 = intent.getIntExtra(StyleMatcherActivity.EXTRA_COLOR2_R, 255)
        val g2 = intent.getIntExtra(StyleMatcherActivity.EXTRA_COLOR2_G, 255)
        val b2 = intent.getIntExtra(StyleMatcherActivity.EXTRA_COLOR2_B, 255)

        // ── Swatches ──────────────────────────────────────────────────────────
        findViewById<View>(R.id.swatch_color1).setBackgroundColor(Color.rgb(r1, g1, b1))
        findViewById<View>(R.id.swatch_color2).setBackgroundColor(Color.rgb(r2, g2, b2))

        // ── Harmony result ────────────────────────────────────────────────────
        val result      = analyzeHarmony(r1, g1, b1, r2, g2, b2)
        val cardResult  = findViewById<CardView>(R.id.card_result)
        val resultIcon  = findViewById<ImageView>(R.id.result_icon)
        val resultTitle = findViewById<TextView>(R.id.result_title)
        val resultSub   = findViewById<TextView>(R.id.result_subtitle)

        if (result.isMatch) {
            cardResult.setCardBackgroundColor(Color.parseColor("#4000C853"))
            resultIcon.setImageResource(R.drawable.ic_sparkle)
            resultTitle.text = "YES – Great Match!"
            resultSub.text   = result.reason
        } else {
            cardResult.setCardBackgroundColor(Color.parseColor("#40FF5252"))
            resultIcon.setImageResource(R.drawable.ic_close)
            resultTitle.text = "NO – Try Another"
            resultSub.text   = result.reason
        }

        // ── Color names ───────────────────────────────────────────────────────
        val txt1       = findViewById<TextView>(R.id.txt_color1_name)
        val txt2       = findViewById<TextView>(R.id.txt_color2_name)
        val recName1   = findViewById<TextView>(R.id.rec_name_1)
        val recName2   = findViewById<TextView>(R.id.rec_name_2)
        val recName3   = findViewById<TextView>(R.id.rec_name_3)
        val recName4   = findViewById<TextView>(R.id.rec_name_4)
        val recSwatch1 = findViewById<View>(R.id.rec_swatch_1)
        val recSwatch2 = findViewById<View>(R.id.rec_swatch_2)
        val recSwatch3 = findViewById<View>(R.id.rec_swatch_3)
        val recSwatch4 = findViewById<View>(R.id.rec_swatch_4)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (!ColorNameFinder.isReady()) {
                    ColorNameFinder.init(this@MatchResultsActivity)
                }
            }

            txt1.text = ColorNameFinder.getColorName(r1, g1, b1)
            txt2.text = ColorNameFinder.getColorName(r2, g2, b2)

            val recs     = getRecommendations(r1, g1, b1)
            val swatches = listOf(recSwatch1, recSwatch2, recSwatch3, recSwatch4)
            val names    = listOf(recName1,   recName2,   recName3,   recName4)
            recs.take(4).forEachIndexed { i, rec ->
                swatches[i].setBackgroundColor(Color.rgb(rec.r, rec.g, rec.b))
                names[i].text = ColorNameFinder.getColorName(rec.r, rec.g, rec.b)
            }
        }

        // ── Buttons ───────────────────────────────────────────────────────────
        findViewById<MaterialButton>(R.id.btn_retry).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            if (!BookmarkManager.isLoggedIn()) {
                showSnackbar("Please log in to save bookmarks.")
                return@setOnClickListener
            }

            val hex1  = String.format("#%02X%02X%02X", r1, g1, b1)
            val hex2  = String.format("#%02X%02X%02X", r2, g2, b2)
            val name1 = txt1.text.toString().let {
                if (it == "Loading..." || it.isEmpty()) ColorNameFinder.getColorName(r1, g1, b1) else it
            }
            val name2 = txt2.text.toString().let {
                if (it == "Loading..." || it.isEmpty()) ColorNameFinder.getColorName(r2, g2, b2) else it
            }

            val dialog = BookmarkDialog.newInstance(
                hex1 = hex1, colorName1 = name1,
                hex2 = hex2, colorName2 = name2
            )
            dialog.onSaved = {
                runOnUiThread { showSnackbar("Match saved to bookmarks!") }
            }
            dialog.show(supportFragmentManager, "bookmark")
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

    // ── Data classes ──────────────────────────────────────────────────────────

    data class HarmonyResult(val isMatch: Boolean, val reason: String, val category: String)
    data class RecommendedColor(val r: Int, val g: Int, val b: Int)

    // ── Harmony algorithm ─────────────────────────────────────────────────────

    private fun analyzeHarmony(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): HarmonyResult {
        val hsl1 = rgbToHsl(r1, g1, b1); val hsl2 = rgbToHsl(r2, g2, b2)
        val h1 = hsl1[0]; val s1 = hsl1[1]; val l1 = hsl1[2]
        val h2 = hsl2[0]; val s2 = hsl2[1]; val l2 = hsl2[2]

        val n1 = isNeutral(s1, l1); val n2 = isNeutral(s2, l2)
        if (n1 && n2) return HarmonyResult(true,  "Both are neutral tones — a clean, classic combo.", "neutral-neutral")
        if (n1 || n2) return HarmonyResult(true,  "Neutral tones pair well with almost any color.",   "neutral-accent")

        val hueDiff = hueDifference(h1, h2)

        if (hueDiff <= 15) return if (abs(l1 - l2) >= 0.2f)
            HarmonyResult(true,  "Same color family, different shades — very stylish.", "monochromatic")
        else
            HarmonyResult(false, "Too similar — try a lighter or darker shade.",        "too-similar")

        if (hueDiff in 150..210)                        return HarmonyResult(true, "Complementary — bold, eye-catching contrast.",  "complementary")
        if (hueDiff <= 45)                              return HarmonyResult(true, "Analogous — harmonious and easy on the eye.",   "analogous")
        if (hueDiff in 110..150 || hueDiff in 210..250) return HarmonyResult(true, "Split-complementary — dynamic but balanced.",  "split-complementary")

        return if (s1 > 0.4f && s2 > 0.4f)
            HarmonyResult(false, "These colors may clash. Check recommendations below.", "clash")
        else
            HarmonyResult(true,  "Muted tones soften the contrast — a subtle mix.",     "muted-clash-ok")
    }

    private fun getRecommendations(r: Int, g: Int, b: Int): List<RecommendedColor> {
        val hsl = rgbToHsl(r, g, b); val h = hsl[0]; val s = hsl[1]; val l = hsl[2]

        if (isNeutral(s, l)) {
            return when {
                l > 0.75f -> listOf(
                    RecommendedColor(30,  58, 138),
                    RecommendedColor(127, 29,  29),
                    RecommendedColor(20,  83,  45),
                    RecommendedColor(30,  30,  30)
                )
                l > 0.35f -> listOf(
                    RecommendedColor(255, 255, 255),
                    RecommendedColor(30,  58, 138),
                    RecommendedColor(180, 83,   9),
                    RecommendedColor(71,  85, 105)
                )
                else -> listOf(
                    RecommendedColor(255, 255, 255),
                    RecommendedColor(191, 219, 254),
                    RecommendedColor(254, 240, 138),
                    RecommendedColor(196, 181, 253)
                )
            }
        }

        val complementary = hslToRgb((h + 180f) % 360f, s, l)
        val splitComp     = hslToRgb((h + 150f) % 360f, s.coerceAtMost(0.6f), l)
        val neutral       = if (h < 105f || h > 330f) RecommendedColor(210, 180, 140)
        else RecommendedColor(71, 85, 105)
        val darkerL       = (l - 0.3f).coerceIn(0.1f, 0.9f)
        val darker        = hslToRgb(h, s, darkerL)

        return listOf(complementary, splitComp, neutral, darker)
    }

    // ── Color math ────────────────────────────────────────────────────────────

    private fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf); val delta = max - min
        val l = (max + min) / 2f
        val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
        val h = when {
            delta == 0f -> 0f
            max == rf   -> 60f * (((gf - bf) / delta) % 6f)
            max == gf   -> 60f * (((bf - rf) / delta) + 2f)
            else        -> 60f * (((rf - gf) / delta) + 4f)
        }.let { if (it < 0) it + 360f else it }
        return floatArrayOf(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): RecommendedColor {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (rf, gf, bf) = when {
            h < 60f  -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else     -> Triple(c, 0f, x)
        }
        return RecommendedColor(
            ((rf + m) * 255).roundToInt().coerceIn(0, 255),
            ((gf + m) * 255).roundToInt().coerceIn(0, 255),
            ((bf + m) * 255).roundToInt().coerceIn(0, 255)
        )
    }

    private fun hueDifference(h1: Float, h2: Float): Int {
        val diff = abs(h1 - h2) % 360f
        return minOf(diff, 360f - diff).roundToInt()
    }

    private fun isNeutral(s: Float, l: Float) = s < 0.15f || l < 0.12f || l > 0.88f
}