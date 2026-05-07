package com.example.pdjissen.ui.dashboard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast

class PedometerManager(
    private val context: Context,
    private val onStepCounted: (Int) -> Unit
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var initialSteps = -1
    private var isTracking = false

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor == null) {
            Toast.makeText(context, "歩数センサーが見つかりません", Toast.LENGTH_SHORT).show()
        }
    }

    fun start() {
        if (!isTracking) {
            isTracking = true
            initialSteps = -1
            stepCounterSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        if (isTracking) {
            isTracking = false
            sensorManager?.unregisterListener(this, stepCounterSensor)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking) return
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialSteps == -1) initialSteps = totalSteps
            val stepsSinceStart = totalSteps - initialSteps
            onStepCounted(stepsSinceStart)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
