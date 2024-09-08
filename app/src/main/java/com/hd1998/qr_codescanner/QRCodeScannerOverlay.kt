package com.hd1998.qr_codescanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Region
import android.util.AttributeSet
import android.view.View

class QRCodeScannerOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rectanglePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val scrimPaint = Paint().apply {
        color = Color.parseColor("#99000000")
    }

    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the semi-transparent scrim
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        // Calculate the size of the scanner rectangle
        val rectSize = minOf(width, height) * 0.7f
        val left = (width - rectSize) / 2f
        val top = (height - rectSize) / 2f
        rectF.set(left, top, left + rectSize, top + rectSize)

        // Draw the scanner rectangle
        canvas.drawRect(rectF, rectanglePaint)

        // Clear the area inside the rectangle
        canvas.clipRect(rectF, Region.Op.DIFFERENCE)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
    }
     fun getRectF(): RectF = rectF
}