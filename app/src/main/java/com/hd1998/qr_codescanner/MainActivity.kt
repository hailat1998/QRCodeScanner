package com.hd1998.qr_codescanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hd1998.qr_codescanner.camera.BarcodeGraphic
import com.hd1998.qr_codescanner.camera.CameraUseCase
import com.hd1998.qr_codescanner.camera.GraphicOverlay
import com.hd1998.qr_codescanner.camera.QRCodeAnalyzer
import com.hd1998.qr_codescanner.camera.QRCodeScannerOverlay
import java.text.SimpleDateFormat
import java.util.Locale


const val TAG = "MAIN"
const val FILENAME_FORMAT ="yyyy-MM-dd'T'HH:mm:ss.SSS"

class MainActivity : AppCompatActivity() {



private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var switchButton: ImageButton
    private lateinit var zoomControl: SeekBar
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private var imageCapture: ImageCapture? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                startCamera()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their decision.
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            checkCameraPermission()
        }, 500)
        setContentView(R.layout.camera_preview)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cameraLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val photoLayout = findViewById<FrameLayout>(R.id.cameraLayout)
        previewView = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.camera_capture_button)
        switchButton = findViewById(R.id.camera_switch_button)
        zoomControl = findViewById(R.id.zoom_control)

        captureButton.setOnClickListener { takePhoto() }
        switchButton.setOnClickListener { switchCamera() }
        startCamera()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val qrCodeScannerOverlay = findViewById<QRCodeScannerOverlay>(R.id.qrCodeScannerOverlay)
        val barcodeGraphic = findViewById<GraphicOverlay>(R.id.barcodeGraphic)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeAnalyzer(qrCodeScannerOverlay) { qrCode ->
                        Log.d(TAG, "QR Code detected: ${qrCode.rawValue}")
                        runOnUiThread {
                            qrCodeScannerOverlay.visibility = View.GONE
                            barcodeGraphic.clear()
                            val barcode = BarcodeGraphic(barcodeGraphic, qrCode)
                            barcodeGraphic.add(barcode)
                            barcodeGraphic.invalidate()
                            barcodeGraphic.setOnButtonClickListener(object : GraphicOverlay.OnButtonClickListener {
                                override fun onButtonClicked(graphic: GraphicOverlay.Graphic) {
                                    when (graphic) {
                                        is BarcodeGraphic -> {
                                            copyToClipboard(text = qrCode.rawValue!!, label = "QR Code")
                                        }
                                    }
                                }
                            })
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

                // Set the image source info for the GraphicOverlay
                barcodeGraphic.setImageSourceInfo(previewView.width, previewView.height, false)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }
    private fun switchCamera() {
        // Implement camera switching
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, proceed with the camera operation
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                AlertDialog.Builder(this)
                    .setTitle("Camera Permission Required")
                    .setMessage("The camera permission is required to use this feature. Please grant the permission.")
                    .setPositiveButton("OK") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            else -> {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
   fun copyToClipboard(text: String, label: String ) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "Url copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    fun setupCamera(context: Context, useCase: CameraUseCase) {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            when (useCase) {
                CameraUseCase.PHOTO_CAPTURE -> setupPhotoCapture(cameraProvider)
                CameraUseCase.BARCODE_SCANNING -> setupBarcodeScanning(cameraProvider)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupBarcodeScanning(cameraProvider: ProcessCameraProvider?) {
        val qrCodeScannerOverlay = findViewById<QRCodeScannerOverlay>(R.id.qrCodeScannerOverlay)
        val barcodeGraphic = findViewById<GraphicOverlay>(R.id.barcodeGraphic)

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeAnalyzer(qrCodeScannerOverlay) { qrCode ->
                    Log.d(TAG, "QR Code detected: ${qrCode.rawValue}")
                    runOnUiThread {
                        qrCodeScannerOverlay.visibility = View.GONE
                        barcodeGraphic.clear()
                        val barcode = BarcodeGraphic(barcodeGraphic, qrCode)
                        barcodeGraphic.add(barcode)
                        barcodeGraphic.invalidate()
                        barcodeGraphic.setOnButtonClickListener(object : GraphicOverlay.OnButtonClickListener {
                            override fun onButtonClicked(graphic: GraphicOverlay.Graphic) {
                                when (graphic) {
                                    is BarcodeGraphic -> {
                                        copyToClipboard(text = qrCode.rawValue!!, label = "QR Code")
                                    }
                                }
                            }
                        })
                    }
                })
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalyzer)

            // Set the image source info for the GraphicOverlay
            barcodeGraphic.setImageSourceInfo(previewView.width, previewView.height, false)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }

    private fun setupPhotoCapture(cameraProvider: ProcessCameraProvider?) {


        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()


        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }
}