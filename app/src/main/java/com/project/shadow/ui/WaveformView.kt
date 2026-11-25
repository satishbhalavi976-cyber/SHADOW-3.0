package com.project.shadow.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * SHADOW 3.0 - Custom Waveform Visualizer View
 * Renders animated sound bars in white on black background
 * during voice listening.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val BAR_COUNT = 32
    private val BAR_MIN_HEIGHT = 6f
    private val BAR_MAX_HEIGHT_RATIO = 0.85f

    private val amplitudes = FloatArray(BAR_COUNT) { BAR_MIN_HEIGHT }
    private var currentAmplitude = 0f
    private var isAnimating = false
    private var animPhase = 0f

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isAnimating) {
                animPhase += 0.15f
                // Create natural wave effect
                for (i in 0 until BAR_COUNT) {
                    val wave = sin((animPhase + i * 0.4f).toDouble()).toFloat()
                    val noise = Random.nextFloat() * 0.3f
                    val base = (currentAmplitude / 100f).coerceIn(0f, 1f)
                    amplitudes[i] = BAR_MIN_HEIGHT + (abs(wave) + noise) * base *
                            height * BAR_MAX_HEIGHT_RATIO
                }
                invalidate()
                postDelayed(this, 50)
            }
        }
    }

    fun startAnimation() {
        isAnimating = true
        post(updateRunnable)
    }

    fun stopAnimation() {
        isAnimating = false
        for (i in 0 until BAR_COUNT) {
            amplitudes[i] = BAR_MIN_HEIGHT
        }
        invalidate()
    }

    fun updateAmplitude(rmsDb: Float) {
        // Convert dB RMS to 0-100 range
        currentAmplitude = ((rmsDb + 2f) * 10f).coerceIn(0f, 100f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val barWidth = width.toFloat() / BAR_COUNT
        val centerY = height / 2f

        for (i in 0 until BAR_COUNT) {
            val barHeight = amplitudes[i].coerceAtMost(height * BAR_MAX_HEIGHT_RATIO)
            val x = i * barWidth + barWidth / 2f
            val alpha = (0.4f + (amplitudes[i] / (height * BAR_MAX_HEIGHT_RATIO)) * 0.6f)
                .coerceIn(0.2f, 1f)
            paint.alpha = (alpha * 255).toInt()

            canvas.drawRoundRect(
                x - barWidth * 0.3f,
                centerY - barHeight / 2,
                x + barWidth * 0.3f,
                centerY + barHeight / 2,
                8f, 8f,
                paint
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        removeCallbacks(updateRunnable)
    }
}
