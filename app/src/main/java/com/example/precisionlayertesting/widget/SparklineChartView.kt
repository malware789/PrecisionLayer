package com.example.precisionlayertesting.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * SparklineChartView
 *
 * Draws a smooth sparkline chart with a gradient fill underneath,
 * matching the blue chart area in the Precision Metrix section.
 *
 * Usage in XML:
 *   <com.yourapp.ui.widget.SparklineChartView
 *       android:id="@+id/chartView"
 *       android:layout_width="match_parent"
 *       android:layout_height="140dp" />
 *
 * Feed data from Activity/Fragment:
 *   chartView.setData(listOf(100f, 80f, 90f, 50f, 60f, 30f, 45f, 20f, 35f, 25f))
 */

class SparklineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Default sample data — replace with real deployment counts
    private var data: List<Float> = listOf(100f, 80f, 90f, 50f, 60f, 30f, 45f, 20f, 35f, 25f)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val fillPath = Path()

    fun setData(points: List<Float>) {
        data = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 16f

        val minVal = data.min()
        val maxVal = data.max()
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = (w - padding * 2) / (data.size - 1)

        // Build line path
        path.reset()
        data.forEachIndexed { i, value ->
            val x = padding + i * stepX
            val y = padding + (1f - (value - minVal) / range) * (h - padding * 2)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Build fill path (close below the line)
        fillPath.reset()
        fillPath.addPath(path)
        val lastX = padding + (data.size - 1) * stepX
        fillPath.lineTo(lastX, h)
        fillPath.lineTo(padding, h)
        fillPath.close()

        // Draw gradient fill
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.argb(60, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(path, linePaint)
    }
}