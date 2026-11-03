package com.s23ultra.aidirector.vision

import android.content.Context
import android.graphics.Bitmap
import com.s23ultra.aidirector.model.S23ProRecipe
import com.s23ultra.aidirector.model.SceneTelemetry
import kotlin.math.roundToInt

class AllAroundSceneClassifier(private val context: Context) {

    fun classifyAndCalculateS23Recipe(frame: Bitmap): SceneTelemetry {
        var lumaSum = 0L
        var rSum = 0L
        var bSum = 0L
        var clippedHigh = 0
        var crushedShadow = 0
        val totalPx = frame.width * frame.height
        val pixels = IntArray(totalPx)
        frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)

        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val y = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
            lumaSum += y
            rSum += r
            bSum += b
            if (r > 248 || g > 248 || b > 248) clippedHigh++
            if (y < 10) crushedShadow++
        }

        val meanLuma = (lumaSum / totalPx).toInt()
        val highPct = (clippedHigh.toFloat() / totalPx) * 100f
        val shadPct = (crushedShadow.toFloat() / totalPx) * 100f
        val warmthRatio = if (bSum > 0) rSum.toFloat() / bSum.toFloat() else 1.0f

        val estimatedKelvin = when {
            warmthRatio > 1.4f -> 6200
            warmthRatio > 1.15f -> 5500
            warmthRatio < 0.85f -> 3800
            else -> 4800
        }

        val recipe = when {
            meanLuma < 75 -> S23ProRecipe(
                sceneCategory = "Night Neon & Wet Street",
                recommendedLens = "1x",
                sensorName = "ISOCELL HP2 (1x 24mm f/1.7)",
                iso = 100,
                shutterSpeed = "1/15s",
                evComp = if (highPct > 2.0f) -0.7f else -0.3f,
                kelvinWb = 4200,
                saturationOffset = 14,
                contrastOffset = 18,
                targetPitchDeg = -1.0f,
                lutName = "CineStill 800T",
                coachingPrompt = "Use 1x f/1.7 lens • Dial EV -0.7 to protect neon signs",
                whyReason = "The f/1.7 200MP ISOCELL HP2 sensor uses 16:1 Tetra2pixel binning (2.4um effective) and large OIS range to keep low-light neon crisp without high ISO noise."
            )
            warmthRatio > 1.2f && meanLuma > 115 -> S23ProRecipe(
                sceneCategory = "Golden Hour Sunset Landscape",
                recommendedLens = "1x",
                sensorName = "Expert RAW 50MP (ISOCELL HP2)",
                iso = 50,
                shutterSpeed = "1/125s",
                evComp = -0.4f,
                kelvinWb = 5800,
                saturationOffset = 16,
                contrastOffset = 14,
                targetPitchDeg = 0.0f,
                lutName = "Fuji Velvia 100",
                coachingPrompt = "Shoot in Expert RAW 50MP at ISO 50 • Level horizon to 0.0°",
                whyReason = "Expert RAW 50MP delivers 16-bit linear DNG highlights with maximum dynamic range for sky gradation, while base ISO 50 eliminates sensor noise."
            )
            meanLuma > 155 -> S23ProRecipe(
                sceneCategory = "Culinary & Tabletop Macro",
                recommendedLens = "3x",
                sensorName = "Sony IMX754 (3x 70mm Telephoto)",
                iso = 80,
                shutterSpeed = "1/160s",
                evComp = 0.2f,
                kelvinWb = 5300,
                saturationOffset = 10,
                contrastOffset = 10,
                targetPitchDeg = -18.0f,
                lutName = "Culinary Crisp",
                coachingPrompt = "Step back & use 3x lens at -18° angle to avoid phone shadow",
                whyReason = "The 3x telephoto prevents close-up wide-angle field curvature blur and phone casting shadows over the meal."
            )
            else -> S23ProRecipe(
                sceneCategory = "Environmental Portrait",
                recommendedLens = "3x",
                sensorName = "Sony IMX754 (3x 70mm f/2.4 OIS)",
                iso = 64,
                shutterSpeed = "1/250s",
                evComp = -0.3f,
                kelvinWb = 5500,
                saturationOffset = 8,
                contrastOffset = 12,
                targetPitchDeg = -2.0f,
                lutName = "Kodak Portra 400",
                coachingPrompt = "Switch to 3x (70mm) • Drop camera to -2° eye-line",
                whyReason = "70mm optical perspective flatters facial geometry without the bulbous distortion of the 24mm wide lens."
            )
        }

        return SceneTelemetry(meanLuma, highPct, shadPct, estimatedKelvin, recipe)
    }
}
