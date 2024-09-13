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
import java.text.SimpleDateFormat
import java.util.Locale


const val TAG = "MAIN"
const val FILENAME_FORMAT ="yyyy-MM-dd'T'HH:mm:ss.SSS"
enum class CAMERA{
    FRONT,
    REAR
}

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var switchButton: ImageButton
    private lateinit var imageButtonPhoto: ImageButton
    private lateinit var imageButtonQr: ImageButton
    private lateinit var photoView: FrameLayout
    private lateinit var barView: FrameLayout
    private var imageCapture: ImageCapture? = null
    var useCase = CameraUseCase.BARCODE_SCANNING
    var camera = CAMERA.REAR

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
               setupCamera(this, useCase, camera )
            } else {
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
        imageButtonQr = findViewById(R.id.switch_button_qr)
        imageButtonPhoto = findViewById(R.id.switch_button_photo)
        previewView = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.camera_capture_button)
        switchButton = findViewById(R.id.camera_switch_button)
        barView = findViewById(R.id.bar_part)
        photoView = findViewById(R.id.photo_part)

        if(useCase == CameraUseCase.BARCODE_SCANNING){
            photoView.visibility = View.GONE
        } else {
            barView.visibility = View.GONE
        }

        captureButton.setOnClickListener { takePhoto() }
        switchButton.setOnClickListener {
                    switchCamera()
                    setupCamera(this, useCase, camera)
        }

        imageButtonPhoto.setOnClickListener {
            barView.visibility = View.GONE
            photoView.visibility = View.VISIBLE
            useCase = CameraUseCase.PHOTO_CAPTURE
            setupCamera(this, useCase, camera)
        }

        imageButtonQr.setOnClickListener {
            photoView.visibility = View.GONE
            barView.visibility = View.VISIBLE
            useCase = CameraUseCase.BARCODE_SCANNING
            setupCamera(this, useCase, camera)
        }
        setupCamera(this, useCase, camera)
    }

    override fun onResume() {
        super.onResume()
        setupCamera(this, useCase, camera)
    }

    private fun takePhoto() {
       val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

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
       camera = if(camera == CAMERA.REAR) CAMERA.FRONT else CAMERA.REAR
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
               setupCamera(this, useCase, camera)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
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
    fun setupCamera(context: Context, useCase: CameraUseCase, camera: CAMERA) {

        Log.i(TAG, "SetUp")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            when (useCase) {
                CameraUseCase.PHOTO_CAPTURE -> setupPhotoCapture(cameraProvider, camera)
                CameraUseCase.BARCODE_SCANNING -> setupBarcodeScanning(cameraProvider)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupBarcodeScanning(cameraProvider: ProcessCameraProvider?) {
        val barcodeGraphic = findViewById<GraphicOverlay>(R.id.barcodeGraphic)

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        Log.i(TAG, "BARCODE")

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeAnalyzer { qrCode ->
                    Log.d(TAG, "QR Code detected: ${qrCode.rawValue}")
                    runOnUiThread {
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
            cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalyzer, preview)
            barcodeGraphic.setImageSourceInfo(previewView.width, previewView.height, false)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }

    private fun setupPhotoCapture(cameraProvider: ProcessCameraProvider?, camera: CAMERA) {

        Log.i(TAG, "PHOTO")

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val cameraSelector = if(camera == CAMERA.REAR) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }
}