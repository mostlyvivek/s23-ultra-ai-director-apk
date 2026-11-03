package com.s23ultra.aidirector.model

data class S23ProRecipe(
    val sceneCategory: String,
    val recommendedLens: String, // "0.6x", "1x", "3x", "10x"
    val sensorName: String,
    val iso: Int,
    val shutterSpeed: String,
    val evComp: Float,
    val kelvinWb: Int,
    val saturationOffset: Int,
    val contrastOffset: Int,
    val targetPitchDeg: Float,
    val targetRollDeg: Float = 0f,
    val lutName: String,
    val coachingPrompt: String,
    val whyReason: String
)

data class SceneTelemetry(
    val meanLuma: Int,
    val highlightClipPercent: Float,
    val shadowCrushPercent: Float,
    val estimatedKelvin: Int,
    val recipe: S23ProRecipe
)
