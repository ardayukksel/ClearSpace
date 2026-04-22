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
import android.util.Patterns
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
import retrofit2.HttpException
import java.util.Locale

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
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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

            when {
                email.isBlank() -> {
                    etEmail.error = "Email is required"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }

                !isValidEmail(email) -> {
                    etEmail.error = "Enter a valid email address"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }

                password.isBlank() -> {
                    etPassword.error = "Password is required"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }

                !isValidPassword(password) -> {
                    etPassword.error = "Password must be at least 8 characters, include 1 uppercase, 1 lowercase, and 1 number"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.findOrCreateUser(
                        FindOrCreateUserRequest(
                            email = email,
                            password = password
                        )
                    )

                    val formattedUserName = formatDisplayName(response.user_name)

                    stateManager.saveLoggedInUser(
                        response.user_id,
                        response.email,
                        formattedUserName
                    )

                    val nextActivity = if (hasCompletedOnboarding) {
                        MainActivity::class.java
                    } else {
                        OnboardingActivity::class.java
                    }

                    startActivity(Intent(this@LoginActivity, nextActivity))
                    finish()
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${errorBody ?: "HTTP ${e.code()}"}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }

    private fun formatDisplayName(rawName: String): String {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return "User"

        return trimmed
            .lowercase(Locale.getDefault())
            .split(Regex("\\s+"))
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }
    }
}