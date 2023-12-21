package com.manish.chatapp.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.canhub.cropper.CropImageView
import com.manish.chatapp.R
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CropImageActivity : AppCompatActivity() {
    private lateinit var cropView: CropImageView

    companion object {
        private var cropImageResult = MutableLiveData<Bitmap?>()
        public fun getCropImageResult(): LiveData<Bitmap?> {
            return cropImageResult
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)

        cropView = findViewById(R.id.cropImageView)

        val uri = intent.getStringExtra("uri")
        if (uri.isNullOrBlank())
            finish()
        else
            cropView.setImageUriAsync(Uri.parse(uri))

        cropView.setAspectRatio(1, 1)
        cropView.setFixedAspectRatio(true)

        //Back btn
        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.linearLayout).setOnClickListener {
            cropImageResult()
        }
    }

    private fun cropImageResult() {
        GlobalScope.launch(IO) {
            // Get the image & Resize it
            val cropImageOptions = CropImageView.RequestSizeOptions.RESIZE_EXACT
            val bitmap =
                cropView.getCroppedImage(300, 300, cropImageOptions)
            cropImageResult.postValue(bitmap)
        }
        finish()
    }

}