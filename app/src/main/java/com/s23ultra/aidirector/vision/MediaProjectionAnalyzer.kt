package com.s23ultra.aidirector.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import com.s23ultra.aidirector.model.SceneTelemetry

class MediaProjectionAnalyzer(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val onFrameAnalyzed: (SceneTelemetry) -> Unit
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val bgThread = HandlerThread("S23ViewfinderStream").apply { start() }
    private val bgHandler = Handler(bgThread.looper)
    private val classifier = AllAroundSceneClassifier(context)

    fun startViewfinderStream(screenWidth: Int = 360, screenHeight: Int = 640, densityDpi: Int = 320) {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "S23UltraProDirector",
            screenWidth, screenHeight, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, bgHandler
        )

        imageReader!!.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * screenWidth
                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / plane.pixelStride,
                    screenHeight, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(plane.buffer)

                // Crop central viewfinder area inside Samsung Camera UI
                val cropTop = screenHeight / 6
                val cropHeight = (screenHeight * 0.65).toInt()
                val vfCrop = Bitmap.createBitmap(bitmap, 0, cropTop, screenWidth, cropHeight)

                val telemetry = classifier.classifyAndCalculateS23Recipe(vfCrop)
                onFrameAnalyzed(telemetry)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
            }
        }, bgHandler)
    }

    fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        bgThread.quitSafely()
    }
}
