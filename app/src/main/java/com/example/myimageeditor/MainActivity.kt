package com.example.myimageeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var textView: TextView? = null
    private var galleryButton: Button? = null
    private var currentPhotoPath: String? = null

    @RequiresApi(api = Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                photo
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName!!,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        println(currentPhotoPath)
        return image
    }// Error occurred while creating the File

    // Continue only if the File was successfully created
// Create the File where the photo should go
    // Ensure that there's a camera activity to handle the intent
    private val photo: Unit
        @SuppressLint("QueryPermissionsNeeded")
        get() {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    println(ex.message)
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.myimageeditor.fileprovider",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                    startActivityForResult(takePictureIntent, 1)
                }
            }
        }

    private fun makeBitmapNull() {
        EditImageActivity.croppedBitmap = null
        EditImageActivity.rotateBitmap = null
        EditImageActivity.cropThenRotateBitmap = null
        EditImageActivity.rotateThenCropBitmap = null
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        textView = findViewById(R.id.textView)
        galleryButton = findViewById(R.id.galleryButton)
        when {
            EditImageActivity.cropThenRotateBitmap != null -> {
                imageView?.setImageBitmap(EditImageActivity.cropThenRotateBitmap)
                textView?.text = getString(R.string.editedimage)
                makeBitmapNull()
            }
            EditImageActivity.rotateThenCropBitmap != null -> {
                imageView?.setImageBitmap(EditImageActivity.rotateThenCropBitmap)
                textView?.text = getString(R.string.editedimage)
                makeBitmapNull()
            }
            EditImageActivity.rotateBitmap != null -> {
                imageView?.setImageBitmap(EditImageActivity.rotateBitmap)
                textView?.text = getString(R.string.editedimage)
                makeBitmapNull()
            }
            EditImageActivity.croppedBitmap != null -> {
                imageView?.setImageBitmap(EditImageActivity.croppedBitmap)
                textView?.text = getString(R.string.editedimage)
                makeBitmapNull()
            }
            bitmap != null -> {
                imageView?.setImageBitmap(bitmap)
                textView?.text = "Edited Image"
                makeBitmapNull()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun clickSelfie(view: View?) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            photo
        }
    }

    fun clickGallery(view: View?) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            try {
                bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$imageFileName.jpg")
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES
                )
                val resolver = contentResolver
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                var imageOutStream: OutputStream? = null
                try {
                    if (uri == null) {
                        throw IOException("Failed to insert into MediaStore row")
                    }
                    imageOutStream = resolver.openOutputStream(uri!!)
                    if (bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                        throw IOException("Failed to compress bitmap")
                    }
                    Toast.makeText(this, "Image Saved to Gallery", Toast.LENGTH_SHORT).show()
                } finally {
                    imageOutStream?.close()
                    val intent = Intent(this, EditImageActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                assert(data != null)
                uri1 = data!!.data
                println(uri1.toString())
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri1)
            } catch (e: Exception) {
                println(e.message)
            }
            val intent = Intent(this, EditImageActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        var bitmap: Bitmap? = null
        var imageFileName: String? = null
        var uri: Uri? = null
        var uri1: Uri? = null
        const val PICK_IMAGE = 2
    }
}