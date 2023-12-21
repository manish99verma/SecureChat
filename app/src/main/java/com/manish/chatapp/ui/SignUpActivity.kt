package com.manish.chatapp.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.manish.chatapp.data.firebase.FirebaseRepository
import com.manish.chatapp.databinding.ActivitySignUpBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID


class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private var profileBitmap: Bitmap? = null

/*
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUser = firebaseAuth.currentUser

    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val users_ref = firebaseDatabase.getReference("Users")

    private val firebaseStorage = FirebaseStorage.getInstance()
    private val profile_pics_bucket = firebaseStorage.getReference("profile_pics")*/

    private val repository = FirebaseRepository.getInstance()
    private var authListenerActivated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check Current user
        if (repository.usesAlreadyLogined())
            finish()

        // ImageReceiver
        binding.imgBtnChoseProfilePic.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()    //User can only select image from Gallery
                .start()
        }
        cropImageReceiver()

        binding.btnSignup.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString()

            createUser(name, email, password)
        }

        binding.btnLogin.setOnClickListener {
            finish()
        }

    }

    private fun createUser(
        name: String,
        email: String,
        password: String,
    ) {
        if (authListenerActivated)
            repository.createUser(name, email, password, profileBitmap)
        else {
            authListenerActivated = true
            repository.createUser(name, email, password, profileBitmap).observe(this) {
                Log.d("TAGY", "startLogIn: Result: $it")
                if (it.successful != null) {
                    if (it.successful) {
                        completedLogin("Login Success", true)
                    } else {
                        completedLogin(it.msg, false)
                    }
                } else if (it.processing != null) {
                    if (it.processing) {
                        startProgressView()
                    } else {
                        completedLogin(it.msg, false)
                    }
                }
            }
        }
    }

    private fun cropImageReceiver() {
        CropImageActivity.getCropImageResult().observe(this@SignUpActivity) {
            if (it == null) {
                Toast.makeText(this@SignUpActivity, "Image Failed to load!", Toast.LENGTH_SHORT)
                    .show()
                return@observe
            }
            binding.profileImage.setImageBitmap(it)
            profileBitmap = it
        }
    }

    private fun completedLogin(msg: String, success: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.mainLayout.animate().alpha(1f).duration = 300

        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Toast.makeText(this@SignUpActivity, msg, Toast.LENGTH_SHORT).show()

        if (success) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun startProgressView() {
        binding.progressBar.visibility = View.VISIBLE
        binding.mainLayout.animate().alpha(0.7f).duration = 300

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            val uri: Uri = data?.data!!

            // Use Uri object instead of File to avoid storage permissions
            Log.i("TAGY", "onActivityResult: $uri")

            val cropIntent = Intent(this@SignUpActivity, CropImageActivity::class.java)
            cropIntent.putExtra("uri", uri.toString())
            startActivity(cropIntent)
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Log.i("TAGY", "onActivityResult: Task Cancelled")
        }
    }

}