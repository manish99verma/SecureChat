package com.manish.chatapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.manish.chatapp.R
import com.manish.chatapp.data.firebase.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {
    private lateinit var imgSplash: ImageView
    private lateinit var txtTitle: TextView
    private lateinit var txtDevelopedBy: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        imgSplash = findViewById(R.id.img_splash)
        txtTitle = findViewById(R.id.txt_title_splash)
        txtDevelopedBy = findViewById(R.id.txt_developedBy)

        imgSplash.animation = AnimationUtils.loadAnimation(this, R.anim.splash_img_anim)
        txtTitle.animation = AnimationUtils.loadAnimation(this, R.anim.splash_title_anim)
        txtDevelopedBy.animation =
            AnimationUtils.loadAnimation(this, R.anim.splash_developed_txt_anim)

        CoroutineScope(Dispatchers.IO).launch {
            val repository = FirebaseRepository.getInstance()
            val intent = if (!repository.usesAlreadyLogined())
                Intent(this@LauncherActivity, LoginActivity::class.java)
            else
                Intent(this@LauncherActivity, HomeActivity::class.java)

            delay(2000)

            startActivity(intent)
            finish()
        }
    }
}