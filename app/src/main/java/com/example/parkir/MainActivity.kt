package com.example.parkir

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val serverUrl = "https://ss2rs89r-5000.asse.devtunnels.ms/predict"
    private val REQUEST_IMAGE_CAPTURE = 1

    private lateinit var imageView: ImageView
    private lateinit var autoComplete: AutoCompleteTextView
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val items = listOf("FST", "Danau", "Masjid", "FISIP", "FPK")

        autoComplete = findViewById(R.id.auto_complete)
        sendButton = findViewById(R.id.btn_send)

        val adapter = ArrayAdapter(this, R.layout.list_item, items)

        autoComplete.setAdapter(adapter)

        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val itemSelected = adapterView.getItemAtPosition(i)
            Toast.makeText(this, "Item: $itemSelected", Toast.LENGTH_SHORT).show()
        }

        imageView = findViewById(R.id.imageView)

        val takePictureButton = findViewById<Button>(R.id.btn_take)
        takePictureButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        sendButton.setOnClickListener {
            val selectedText = autoComplete.text.toString()
            val imageBitmap = (imageView.drawable as? BitmapDrawable)?.bitmap
            sendToServer(selectedText, imageBitmap)
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            showToast("No camera app available")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(imageBitmap)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendToServer(selectedText: String, imageBitmap: Bitmap?) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("selected_text", selectedText)
            .addFormDataPart(
                "image",
                "image.jpg",
                createRequestBodyFromBitmap(imageBitmap) ?: return
            )
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showToast("Request failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful) {
                        showToast("Response: $responseBody")
                    } else {
                        showToast("Request failed: ${response.code}")
                    }
                }
            }
        })
    }

    private fun createRequestBodyFromBitmap(bitmap: Bitmap?): RequestBody? {
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            return RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
        }
        return null
    }
}