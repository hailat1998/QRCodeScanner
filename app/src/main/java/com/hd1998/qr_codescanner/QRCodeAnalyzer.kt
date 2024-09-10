package com.hd1998.qr_codescanner

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QRCodeAnalyzer(
    private val overlay: QRCodeScannerOverlay,
    private val onQRCodeDetected: (Barcode) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @ExperimentalGetImage
    override fun analyze(imageProxy:ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Get the overlay rectangle in image coordinates
            val overlayRect = overlay.getRectF()
            val scaleX = image.width / overlay.width.toFloat()
            val scaleY = image.height / overlay.height.toFloat()
            val scaledRect = Rect(
                (overlayRect.left * scaleX).toInt(),
                (overlayRect.top * scaleY).toInt(),
                (overlayRect.right * scaleX).toInt(),
                (overlayRect.bottom * scaleY).toInt()
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                          // Check if the barcode is within the overlay rectangle
                                    Log.d(TAG, "QR Code detected: ${barcode.rawValue}")
                                    onQRCodeDetected(barcode)
                          }

                }
                .addOnFailureListener {
                    Log.e(TAG, "QR Code scanning failed: ", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}