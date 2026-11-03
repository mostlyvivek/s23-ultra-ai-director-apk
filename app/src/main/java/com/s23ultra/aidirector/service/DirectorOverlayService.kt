package com.s23ultra.aidirector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.s23ultra.aidirector.automation.SamsungProAutoDialerService
import com.s23ultra.aidirector.model.S23ProRecipe
import com.s23ultra.aidirector.model.SceneTelemetry
import com.s23ultra.aidirector.ui.FloatingDirectorOverlayContent
import com.s23ultra.aidirector.vision.MediaProjectionAnalyzer

class DirectorOverlayService : Service(), SensorEventListener, LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        var mediaProjection: MediaProjection? = null
        const val CHANNEL_ID = "S23UltraDirectorServiceChannel"
    }

    private lateinit var windowManager: WindowManager
    private var overlayComposeView: ComposeView? = null
    private var projectionAnalyzer: MediaProjectionAnalyzer? = null
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private var currentPitch = 0f
    private var currentRoll = 0f
    private var currentTelemetry: SceneTelemetry? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        createNotificationChannel()
        startForeground(101, buildNotification())

        setupFloatingOverlay()

        mediaProjection?.let { mp ->
            projectionAnalyzer = MediaProjectionAnalyzer(this, mp) { telemetry ->
                currentTelemetry = telemetry
                updateOverlayContent()
            }
            projectionAnalyzer?.startViewfinderStream()
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun setupFloatingOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DirectorOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DirectorOverlayService)
            setViewTreeViewModelStoreOwner(this@DirectorOverlayService)
            setContent {
                FloatingDirectorOverlayContent(
                    telemetry = currentTelemetry,
                    currentPitch = currentPitch,
                    currentRoll = currentRoll,
                    onAutoDialRequested = { recipe ->
                        SamsungProAutoDialerService.instance?.applyRecipeToSamsungCamera(recipe)
                    },
                    onCloseService = {
                        stopSelf()
                    }
                )
            }
        }

        windowManager.addView(overlayComposeView, params)
    }

    private fun updateOverlayContent() {
        overlayComposeView?.post {
            overlayComposeView?.setContent {
                FloatingDirectorOverlayContent(
                    telemetry = currentTelemetry,
                    currentPitch = currentPitch,
                    currentRoll = currentRoll,
                    onAutoDialRequested = { recipe ->
                        SamsungProAutoDialerService.instance?.applyRecipeToSamsungCamera(recipe)
                    },
                    onCloseService = {
                        stopSelf()
                    }
                )
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert to degrees
            currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            currentRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            updateOverlayContent()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "S23 Ultra AI Director Active",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("S23 Ultra Pro AI Director")
            .setContentText("Overlay active over Samsung Camera & Expert RAW")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        store.clear()

        sensorManager.unregisterListener(this)
        projectionAnalyzer?.stop()
        mediaProjection?.stop()
        mediaProjection = null

        overlayComposeView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
