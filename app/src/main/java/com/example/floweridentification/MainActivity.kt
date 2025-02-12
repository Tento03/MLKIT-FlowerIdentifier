package com.example.floweridentification

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.floweridentification.databinding.ActivityMainBinding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageUri: Uri? = null
    private lateinit var imageLabeler: ImageLabeler

    // Launcher untuk memilih gambar dari galeri
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                binding.ivPost.setImageURI(it)
                analyzeImageFromGallery()
            }
        }

    // Launcher untuk mengambil gambar dari kamera
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri?.let { uri ->
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                    binding.ivPost.setImageBitmap(bitmap)
                    analyzeImageFromCamera(bitmap)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Image Labeler
        val localModel = LocalModel.Builder().setAssetFilePath("model_flowers.tflite").build()
        val options = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.7f)
            .setMaxResultCount(5)
            .build()
        imageLabeler = ImageLabeling.getClient(options)

        // Tombol untuk memilih gambar dari galeri
        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Tombol untuk membuka kamera
        binding.btnStartCamera.setOnClickListener {
            val photoFile = createImageFile()
            photoFile?.also {
                imageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", it)
                cameraLauncher.launch(imageUri)
            }
        }

        // Periksa izin
        checkPermissions()
    }

    // Fungsi untuk menganalisis gambar dari galeri
    private fun analyzeImageFromGallery() {
        imageUri?.let { uri ->
            try {
                val image = InputImage.fromFilePath(this, uri)
                imageLabeler.process(image)
                    .addOnSuccessListener { labels ->
                        val labelResult = StringBuilder()
                        for (label in labels) {
                            val text = label.text
                            val confidence = label.confidence
                            labelResult.append("Label: $text, Confidence: $confidence\n")
                            Log.d("ImageLabeling", "Label: $text, Confidence: $confidence")
                        }
                        binding.tvOutput.text = if (labelResult.isEmpty()) {
                            "Could not identify!!"
                        } else {
                            labelResult.toString()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ImageLabeling", "Error: ${e.message}")
                        binding.tvOutput.text = "Error analyzing image."
                    }
            } catch (e: Exception) {
                Log.e("ImageLabeling", "Failed to process image: ${e.message}")
                binding.tvOutput.text = "Failed to process image."
            }
        }
    }

    // Fungsi untuk menganalisis gambar dari kamera
    private fun analyzeImageFromCamera(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(image)
            .addOnSuccessListener { labels ->
                val labelResult = StringBuilder()
                for (label in labels) {
                    val text = label.text
                    val confidence = label.confidence
                    labelResult.append("Label: $text, Confidence: $confidence\n")
                    Log.d("ImageLabeling", "Label: $text, Confidence: $confidence")
                }
                binding.tvOutput.text = if (labelResult.isEmpty()) {
                    "Could not identify!!"
                } else {
                    labelResult.toString()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ImageLabeling", "Error: ${e.message}")
                binding.tvOutput.text = "Error analyzing image."
            }
    }

    // Fungsi untuk membuat file gambar sementara
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            Log.e("CameraError", "Failed to create image file: ${e.message}")
            null
        }
    }

    // Fungsi untuk memeriksa izin
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("ImageHelperActivity", "Permissions granted")
            } else {
                Log.e("ImageHelperActivity", "Permission denied")
            }
        }
    }

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 1
    }
}