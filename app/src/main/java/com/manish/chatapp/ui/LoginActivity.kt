package com.manish.chatapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.manish.chatapp.data.firebase.FirebaseRepository
import com.manish.chatapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var repository: FirebaseRepository
    private var loginResultDataInitiated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = FirebaseRepository.getInstance()
        if (repository.usesAlreadyLogined()) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        binding.btnSignup.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            startLogIn()
        }
    }

    private fun startLogIn() {
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPassword.text.toString()

        if (!loginResultDataInitiated) {
            loginResultDataInitiated = true
            repository.logIn(email, password).observe(this) {
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
        } else {
            repository.logIn(email, password)
        }
    }

    /*    private fun logInFirebase() {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString()

            val exceptionMsg =
                if (TextUtils.isEmpty(email)
                    || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                )
                    "Please enter a valid email!"
                else if (TextUtils.isEmpty(password))
                    "Please enter password!"
                else if (password.length < 6)
                    "Please enter a longer password!"
                else
                    ""

            if (exceptionMsg.isNotEmpty()) {
                Toast.makeText(this, exceptionMsg, Toast.LENGTH_SHORT)
                    .show()
                return
            }

            startProgressView()
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                it.localizedMessage?.let { it1 -> completedLogin(it1) }
            }

        }*/

    private fun completedLogin(msg: String, success: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.mainLayout.animate().alpha(1f).duration = 300

        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()

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
}