package com.hd1998.qr_codescanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.common.base.Preconditions
import com.google.common.primitives.Ints
import kotlin.math.max
import kotlin.math.min

class GraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val lock = Any()
    private val graphics = mutableListOf<Graphic>()
    private val transformationMatrix = Matrix()

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private var scaleFactor = 1.0f
    private var postScaleWidthOffset: Float = 0f
    private var postScaleHeightOffset: Float = 0f
    private var isImageFlipped: Boolean = false
    private var needUpdateTransformation = true

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)

        protected fun drawRect(
            canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, paint: Paint
        ) {
            canvas.drawRect(left, top, right, bottom, paint)
        }

        protected fun drawText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
            canvas.drawText(text, x, y, paint)
        }

        fun scale(imagePixel: Float): Float = imagePixel * overlay.scaleFactor

        fun getApplicationContext(): Context = overlay.context.applicationContext

        fun isImageFlipped(): Boolean = overlay.isImageFlipped

        fun translateX(x: Float): Float {
            return if (overlay.isImageFlipped) {
                overlay.width - (scale(x) - overlay.postScaleWidthOffset)
            } else {
                scale(x) - overlay.postScaleWidthOffset
            }
        }

        fun translateY(y: Float): Float = scale(y) - overlay.postScaleHeightOffset

        fun getTransformationMatrix(): Matrix = overlay.transformationMatrix

        fun postInvalidate() {
            overlay.postInvalidate()
        }

        fun updatePaintColorByZValue(
            paint: Paint,
            canvas: Canvas,
            visualizeZ: Boolean,
            rescaleZForVisualization: Boolean,
            zInImagePixel: Float,
            zMin: Float,
            zMax: Float
        ) {
            if (!visualizeZ) return

            val zLowerBoundInScreenPixel: Float
            val zUpperBoundInScreenPixel: Float

            if (rescaleZForVisualization) {
                zLowerBoundInScreenPixel = min(-0.001f, scale(zMin))
                zUpperBoundInScreenPixel = max(0.001f, scale(zMax))
            } else {
                val defaultRangeFactor = 1f
                zLowerBoundInScreenPixel = -defaultRangeFactor * canvas.width
                zUpperBoundInScreenPixel = defaultRangeFactor * canvas.width
            }

            val zInScreenPixel = scale(zInImagePixel)

            if (zInScreenPixel < 0) {
                var v = (zInScreenPixel / zLowerBoundInScreenPixel * 255).toInt()
                v = Ints.constrainToRange(v, 0, 255)
                paint.setARGB(255, 255, 255 - v, 255 - v)
            } else {
                var v = (zInScreenPixel / zUpperBoundInScreenPixel * 255).toInt()
                v = Ints.constrainToRange(v, 0, 255)
                paint.setARGB(255, 255 - v, 255 - v, 255)
            }
        }
    }

    init {
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            needUpdateTransformation = true
        }
    }

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    fun remove(graphic: Graphic) {
        synchronized(lock) {
            graphics.remove(graphic)
        }
        postInvalidate()
    }

    fun setImageSourceInfo(imageWidth: Int, imageHeight: Int, isFlipped: Boolean) {
        Preconditions.checkState(imageWidth > 0, "image width must be positive")
        Preconditions.checkState(imageHeight > 0, "image height must be positive")
        synchronized(lock) {
            this.imageWidth = imageWidth
            this.imageHeight = imageHeight
            this.isImageFlipped = isFlipped
            needUpdateTransformation = true
        }
        postInvalidate()
    }

    fun getImageWidth(): Int = imageWidth

    fun getImageHeight(): Int = imageHeight

    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return
        }
        val viewAspectRatio = width.toFloat() / height.toFloat()
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = width.toFloat() / imageWidth
            postScaleHeightOffset = (width.toFloat() / imageAspectRatio - height) / 2
        } else {
            scaleFactor = height.toFloat() / imageHeight
            postScaleWidthOffset = (height.toFloat() * imageAspectRatio - width) / 2
        }

        transformationMatrix.reset()
        transformationMatrix.setScale(scaleFactor, scaleFactor)
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)

        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, width / 2f, height / 2f)
        }

        needUpdateTransformation = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            updateTransformationIfNeeded()

            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}