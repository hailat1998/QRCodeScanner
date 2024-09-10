package com.hd1998.qr_codescanner

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mlkit.vision.barcode.common.Barcode

class BarcodeGraphic(overlay: GraphicOverlay, private val barcode: Barcode?) :
    GraphicOverlay.Graphic(overlay) {
    private val rectPaint: Paint = Paint()
    private val barcodePaint: Paint
    private val labelPaint: Paint


    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 36f
    }

    private val buttonPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val buttonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private lateinit var buttonRect: RectF
    private var buttonText = "Copy"

    init {
        rectPaint.color = MARKER_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH
        barcodePaint = Paint()
        barcodePaint.color = TEXT_COLOR
        barcodePaint.textSize = TEXT_SIZE
        labelPaint = Paint()
        labelPaint.color = MARKER_COLOR
        labelPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        checkNotNull(barcode) { "Attempting to draw a null barcode." }
        val rect = RectF(barcode.boundingBox)
        val x0 = translateX(rect.left)
        val x1 = translateX(rect.right)
        rect.left = minOf(x0, x1)
        rect.right = maxOf(x0, x1)
        rect.top = translateY(rect.top)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, rectPaint)
        val lineHeight = TEXT_SIZE + 2 * STROKE_WIDTH
        val textWidth = barcodePaint.measureText(barcode.displayValue)
        canvas.drawRect(
            rect.left - STROKE_WIDTH,
            rect.top - lineHeight,
            rect.left + textWidth + 2 * STROKE_WIDTH,
            rect.top,
            labelPaint
        )
        barcode.displayValue?.let { canvas.drawText(it, rect.left, rect.top - STROKE_WIDTH, barcodePaint) }

        val bottom = translateY(rect.bottom)


    }

    fun isButtonClicked(x: Float, y: Float): Boolean {
        return ::buttonRect.isInitialized && buttonRect.contains(x, y)
    }

    override fun drawBtn(canvas: Canvas) {
        checkNotNull(barcode) { "Attempting to draw a null barcode." }
        val rect = RectF(barcode.boundingBox)
        val buttonWidth = 300f
        val buttonHeight = 100f
        val buttonLeft = (canvas.width - buttonWidth) / 2
        val buttonTop = translateY(rect.bottom) + 100f
        buttonRect = RectF(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight)

        canvas.drawRoundRect(buttonRect, 20f, 20f, buttonPaint)
        canvas.drawText(buttonText, buttonRect.centerX(), buttonRect.centerY() + 15f, buttonTextPaint)
    }

    companion object {
        private const val TEXT_COLOR = Color.BLACK
        private const val MARKER_COLOR = Color.WHITE
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
    }
}