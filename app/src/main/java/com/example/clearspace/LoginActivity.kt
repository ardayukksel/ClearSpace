package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clearspace.data.network.FindOrCreateUserRequest
import com.example.clearspace.data.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tvSubtitle = findViewById<TextView>(R.id.tv_login_subtitle)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val ivTogglePassword = findViewById<ImageView>(R.id.iv_toggle_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvForgotPassword = findViewById<TextView>(R.id.tv_forgot_password)
        val tvSignup = findViewById<TextView>(R.id.tv_signup)

        val subtitleText = "Sign in to continue your focus journey"
        val spannableSubtitle = SpannableString(subtitleText)
        val startIndex = subtitleText.indexOf("focus journey")
        val endIndex = startIndex + "focus journey".length
        spannableSubtitle.setSpan(
            ForegroundColorSpan(Color.parseColor("#4CAF50")),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableSubtitle.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvSubtitle.text = spannableSubtitle

        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val stateManager = ClearSpaceStateManager(this)
        val hasCompletedOnboarding = sharedPref.getBoolean("hasCompletedOnboarding", false)

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter your email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fallbackName = email.substringBefore("@")
                .replace(Regex("[._]"), " ")
                .trim()
                .ifBlank { "User" }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.findOrCreateUser(
                        FindOrCreateUserRequest(email = email)
                    )

                    stateManager.saveLoggedInUser(
                        response.user_id,
                        response.email,
                        response.user_name
                    )

                    val nextActivity = if (hasCompletedOnboarding) {
                        MainActivity::class.java
                    } else {
                        OnboardingActivity::class.java
                    }

                    startActivity(Intent(this@LoginActivity, nextActivity))
                    finish()
                } catch (e: Exception) {
                    sharedPref.edit()
                        .putString("userName", fallbackName)
                        .putString("userEmail", email)
                        .apply()

                    Toast.makeText(
                        this@LoginActivity,
                        "Login saved locally. Backend unavailable.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val nextActivity = if (hasCompletedOnboarding) {
                        MainActivity::class.java
                    } else {
                        OnboardingActivity::class.java
                    }

                    startActivity(Intent(this@LoginActivity, nextActivity))
                    finish()
                }
            }
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}