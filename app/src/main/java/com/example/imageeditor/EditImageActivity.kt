package com.example.imageeditor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.theartofdev.edmodo.cropper.CropImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class EditImageActivity : AppCompatActivity() {
    var imageView: ImageView? = null
    var rotateButton: Button? = null
    var saveButton: Button? = null
    var cropButton: Button? = null
    var mCurrRotation = 0
    var isRotate = false
    var fromRotation = 0f
    var toRotation = 0f
    fun makeBitmapNull() {
        mCurrRotation = 0
        toRotation = 0f
        fromRotation = 0f
        rotateBitmap = null
        croppedBitmap = null
        rotateThenCropBitmap = null
        cropThenRotateBitmap = null
    }

    fun undo(view: View?) {
        val matrix = Matrix()
        mCurrRotation += 90
        toRotation = mCurrRotation.toFloat()
        val rotateAnimation = RotateAnimation(
            fromRotation, 0F, (imageView!!.width / 2).toFloat(), (imageView!!.height / 2).toFloat()
        )
        rotateAnimation.duration = 1000
        rotateAnimation.fillAfter = true
        matrix.setRotate(toRotation)
        println(toRotation.toString() + "TO ROTATION")
        println(fromRotation.toString() + "FROM ROTATION")
        if (croppedBitmap != null) {
            cropThenRotateBitmap = Bitmap.createBitmap(
                croppedBitmap!!,
                0,
                0,
                croppedBitmap!!.width,
                croppedBitmap!!.height,
                matrix,
                true
            )
        } else {
            rotateBitmap = MainActivity.bitmap?.let {
                Bitmap.createBitmap(
                    it,
                    0,
                    0,
                    MainActivity.bitmap!!.width,
                    MainActivity.bitmap!!.height,
                    matrix,
                    true
                )
            }
        }
        imageView!!.setImageBitmap(MainActivity.bitmap)
        imageView!!.startAnimation(rotateAnimation)
        makeBitmapNull()
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun crop(view: View?) {
        if (rotateBitmap != null) {
            val bytes = ByteArrayOutputStream()
            rotateBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(
                contentResolver,
                rotateBitmap,
                MainActivity.imageFileName + ".jpg",
                null
            )
            //            System.out.println(Uri.parse(path));
            MainActivity.uri = Uri.parse(path)
            CropImage.activity(MainActivity.uri)
                .start(this)
        } else if (MainActivity.uri1 != null) {
            CropImage.activity(MainActivity.uri1)
                .start(this)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                resultUri = result.uri
                imageView!!.setImageURI(resultUri)
                //                Matrix matrix = new Matrix();
                val bitmapDrawable = imageView!!.drawable as BitmapDrawable
                println(imageView!!.rotation)
                croppedBitmap = bitmapDrawable.bitmap
                if (isRotate) {
                    rotateThenCropBitmap = croppedBitmap
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                println(result.error)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun save(view: View?) {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, MainActivity.imageFileName + ".jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        var imageOutStream: OutputStream? = null
        try {
            if (uri == null) {
                throw IOException("Failed to insert MediaStore row")
            }
            imageOutStream = resolver.openOutputStream(uri)
            if (cropThenRotateBitmap != null) {
                if (!cropThenRotateBitmap!!.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        imageOutStream
                    )
                ) {
                    throw IOException("Failed to compress bitmap")
                }
            } else if (rotateThenCropBitmap != null) {
                if (!rotateThenCropBitmap!!.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        imageOutStream
                    )
                ) {
                    throw IOException("Failed to compress bitmap")
                }
            } else if (croppedBitmap != null) {
                if (!croppedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw IOException("Failed to compress bitmap")
                }
            } else if (rotateBitmap != null) {
                if (!rotateBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw IOException("Failed to compress bitmap")
                }
            } else {
                if (!MainActivity.bitmap!!.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        imageOutStream
                    )
                ) {
                    throw IOException("Failed to compress bitmap")
                }
            }
            Toast.makeText(this, "Image Saved in Gallery", Toast.LENGTH_SHORT).show()
        } finally {
            if (imageOutStream != null) {
                imageOutStream.close()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                finish()
                startActivity(intent)
            }
        }
    }

    fun rotate(view: View?) {
        isRotate = true
        mCurrRotation %= 360
        val matrix = Matrix()
        println(imageView!!.rotation)
        fromRotation = mCurrRotation.toFloat()
        mCurrRotation += 90
        toRotation = mCurrRotation.toFloat()
        val rotateAnimation = RotateAnimation(
            fromRotation,
            toRotation,
            (imageView!!.width / 2).toFloat(),
            (imageView!!.height / 2).toFloat()
        )
        rotateAnimation.duration = 1000
        rotateAnimation.fillAfter = true
        matrix.setRotate(toRotation)
        println(toRotation.toString() + "TO ROTATION")
        println(fromRotation.toString() + "FROM ROTATION")
        if (croppedBitmap != null) {
            cropThenRotateBitmap = Bitmap.createBitmap(
                croppedBitmap!!,
                0,
                0,
                croppedBitmap!!.width,
                croppedBitmap!!.height,
                matrix,
                true
            )
        } else {
            rotateBitmap = Bitmap.createBitmap(
                MainActivity.bitmap!!,
                0,
                0,
                MainActivity.bitmap!!.width,
                MainActivity.bitmap!!.height,
                matrix,
                true
            )
        }
        imageView!!.startAnimation(rotateAnimation)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_image)
        imageView = findViewById(R.id.editImageView)
        rotateButton = findViewById(R.id.rotateButton)
        saveButton = findViewById(R.id.saveButton)
        cropButton = findViewById(R.id.cropButton)
        ActivityCompat.requestPermissions(
            this@EditImageActivity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )
        try {
            imageView?.setImageBitmap(MainActivity.bitmap)
        } catch (e: Exception) {
            println(e.message)
        }
    }

    companion object {
        @JvmField
        var rotateBitmap: Bitmap? = null
        @JvmField
        var cropThenRotateBitmap: Bitmap? = null
        @JvmField
        var rotateThenCropBitmap: Bitmap? = null
        @JvmField
        var croppedBitmap: Bitmap? = null
        var resultUri: Uri? = null
    }
}