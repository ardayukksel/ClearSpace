package com.example.clearspace

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val ivBack = findViewById<ImageView>(R.id.iv_back)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnSendCode = findViewById<Button>(R.id.btn_send_code)
        val tvBackToLogin = findViewById<TextView>(R.id.tv_back_to_login)

        ivBack.setOnClickListener {
            finish()
        }

        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isBlank()) {
                Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Implement actual password reset logic
            Toast.makeText(this, "Verification code sent to $email", Toast.LENGTH_SHORT).show()
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }
}