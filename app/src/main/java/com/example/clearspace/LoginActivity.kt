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
import com.example.clearspace.data.network.LoginRequest
import com.example.clearspace.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val stateManager = ClearSpaceStateManager(this)
        val hasCompletedOnboarding = sharedPref.getBoolean("hasCompletedOnboarding", false)

        if (stateManager.isUserLoggedIn()) {
            val nextActivity = if (hasCompletedOnboarding) {
                MainActivity::class.java
            } else {
                OnboardingActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val tvSubtitle = findViewById<TextView>(R.id.tv_login_subtitle)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val ivTogglePassword = findViewById<ImageView>(R.id.iv_toggle_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
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

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            etPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            etPassword.setSelection(etPassword.text.length)
            ivTogglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_visibility
                else R.drawable.ic_visibility_off
            )
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!isValidEmail(email)) {
                etEmail.error = "Invalid email"
                return@setOnClickListener
            }

            if (password.length < 8) {
                etPassword.error = "Password too short"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.login(LoginRequest(email, password))
                    val formattedName = formatDisplayName(response.user_name)

                    stateManager.saveLoggedInUser(
                        userId = response.user_id,
                        email = response.email,
                        userName = formattedName,
                        points = response.points ?: 0,
                        level = response.level ?: 1,
                        currentStreak = response.current_streak ?: 0,
                        longestStreak = response.longest_streak ?: 0,
                        lastStreakDate = response.last_streak_date
                    )

                    val nextActivity = if (hasCompletedOnboarding) {
                        MainActivity::class.java
                    } else {
                        OnboardingActivity::class.java
                    }

                    startActivity(Intent(this@LoginActivity, nextActivity))
                    finish()

                } catch (e: HttpException) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Wrong email or password",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun formatDisplayName(rawName: String): String {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return "User"

        return trimmed
            .lowercase(Locale.getDefault())
            .split(Regex("\\s+"))
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(Locale.getDefault())
                    } else {
                        char.toString()
                    }
                }
            }
    }
}