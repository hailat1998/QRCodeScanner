package com.hd1998.qr_codescanner.camera

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.hd1998.qr_codescanner.TAG

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


            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                          // Check if the barcode is within the overlay rectangle

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