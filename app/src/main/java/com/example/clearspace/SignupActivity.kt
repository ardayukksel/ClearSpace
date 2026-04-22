package com.example.clearspace

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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clearspace.data.network.RegisterRequest
import com.example.clearspace.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale

class SignupActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val stateManager = ClearSpaceStateManager(this)

        val ivBack = findViewById<ImageView>(R.id.iv_back)
        val tvSubtitle = findViewById<TextView>(R.id.tv_subtitle)
        val etName = findViewById<EditText>(R.id.et_signup_name)
        val etEmail = findViewById<EditText>(R.id.et_signup_email)
        val etPassword = findViewById<EditText>(R.id.et_signup_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_signup_confirm_password)
        val ivTogglePassword = findViewById<ImageView>(R.id.iv_toggle_password)
        val ivToggleConfirmPassword = findViewById<ImageView>(R.id.iv_toggle_confirm_password)
        val cbTerms = findViewById<CheckBox>(R.id.cb_terms)
        val tvTerms = findViewById<TextView>(R.id.tv_terms)
        val btnCreateAccount = findViewById<Button>(R.id.btn_create_account)
        val tvBackToLogin = findViewById<TextView>(R.id.tv_back_to_login)

        // 🔥 Subtitle styling
        val subtitleText = "Start your digital wellbeing journey"
        val spannableSubtitle = SpannableString(subtitleText)
        val startIndex = subtitleText.indexOf("digital wellbeing")
        val endIndex = startIndex + "digital wellbeing".length
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

        // 🔥 Terms styling
        val termsText = "By creating an account, you agree to our Terms of Service and Privacy Policy."
        val spannableTerms = SpannableString(termsText)

        val tosStart = termsText.indexOf("Terms of Service")
        val tosEnd = tosStart + "Terms of Service".length

        val ppStart = termsText.indexOf("Privacy Policy")
        val ppEnd = ppStart + "Privacy Policy".length

        spannableTerms.setSpan(
            ForegroundColorSpan(Color.parseColor("#5D4037")),
            tosStart,
            tosEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableTerms.setSpan(
            StyleSpan(Typeface.BOLD),
            tosStart,
            tosEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableTerms.setSpan(
            ForegroundColorSpan(Color.parseColor("#5D4037")),
            ppStart,
            ppEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableTerms.setSpan(
            StyleSpan(Typeface.BOLD),
            ppStart,
            ppEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvTerms.text = spannableTerms

        // 🔙 Navigation
        ivBack.setOnClickListener { finish() }
        tvBackToLogin.setOnClickListener { finish() }

        // 👁 Password toggle
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType =
                if (isPasswordVisible)
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            etPassword.setSelection(etPassword.text.length)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.inputType =
                if (isConfirmPasswordVisible)
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        // 🚀 CREATE ACCOUNT
        btnCreateAccount.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                toast("Please fill in all fields")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Invalid email")
                return@setOnClickListener
            }

            if (password.length < 8) {
                toast("Password must be at least 8 characters")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                toast("Passwords do not match")
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                toast("Please accept the terms")
                return@setOnClickListener
            }

            val formattedName = formatDisplayName(name)

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.register(
                        RegisterRequest(
                            email = email,
                            password = password,
                            user_name = formattedName
                        )
                    )

                    // ✅ Save logged-in user
                    stateManager.saveLoggedInUser(
                        response.user_id,
                        response.email,
                        response.user_name
                    )

                    toast("Account created successfully!")

                    val intent = Intent(this@SignupActivity, OnboardingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                } catch (e: HttpException) {
                    toast("User already exists")
                } catch (e: Exception) {
                    toast("Error: ${e.message}")
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun formatDisplayName(rawName: String): String {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return "User"

        return trimmed
            .lowercase(Locale.getDefault())
            .split(" ")
            .joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
    }
}