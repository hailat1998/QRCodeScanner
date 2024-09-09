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
    }

    companion object {
        private const val TEXT_COLOR = Color.BLACK
        private const val MARKER_COLOR = Color.WHITE
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
    }
}