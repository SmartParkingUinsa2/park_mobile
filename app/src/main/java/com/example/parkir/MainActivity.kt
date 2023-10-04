package com.example.parkir

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val SERVER_URL = "https://z1gx2ddc-5000.asse.devtunnels.ms/upload"
    }

    private lateinit var imageView: ImageView
    private lateinit var uploadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        uploadButton = findViewById(R.id.uploadButton)

        uploadButton.setOnClickListener {
            try {
                dispatchTakePictureIntent()
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Kamera tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)

            // Upload gambar ke server Python
            uploadImageToServer(imageBitmap)
        }
    }

    private fun uploadImageToServer(bitmap: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Gagal mengunggah gambar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()

                    // Proses data JSON yang diterima dari server
                    if (!responseData.isNullOrEmpty()) {
                        if (responseData == "sukses") {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Upload gambar berhasil",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Response tidak sesuai",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Gagal mengunggah gambar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}