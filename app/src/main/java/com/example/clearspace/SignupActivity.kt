package com.example.clearspace

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SignupActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

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

        // Style subtitle with colored text
        val subtitleText = "Start your digital wellbeing journey"
        val spannableSubtitle = SpannableString(subtitleText)
        val startIndex = subtitleText.indexOf("digital wellbeing")
        val endIndex = startIndex + "digital wellbeing".length
        spannableSubtitle.setSpan(ForegroundColorSpan(Color.parseColor("#4CAF50")), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableSubtitle.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvSubtitle.text = spannableSubtitle

        // Style terms text with bold links
        val termsText = "By creating an account, you agree to our Terms of Service and Privacy Policy."
        val spannableTerms = SpannableString(termsText)
        val tosStart = termsText.indexOf("Terms of Service")
        val tosEnd = tosStart + "Terms of Service".length
        val ppStart = termsText.indexOf("Privacy Policy")
        val ppEnd = ppStart + "Privacy Policy".length
        spannableTerms.setSpan(ForegroundColorSpan(Color.parseColor("#5D4037")), tosStart, tosEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableTerms.setSpan(StyleSpan(Typeface.BOLD), tosStart, tosEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableTerms.setSpan(ForegroundColorSpan(Color.parseColor("#5D4037")), ppStart, ppEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableTerms.setSpan(StyleSpan(Typeface.BOLD), ppStart, ppEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvTerms.text = spannableTerms

        ivBack.setOnClickListener {
            finish()
        }

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etConfirmPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)
            } else {
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }

        btnCreateAccount.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please agree to the Terms of Service and Privacy Policy.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("ClearSpacePrefs", MODE_PRIVATE)
            sharedPref.edit {
                putString("userName", name)
                putString("userEmail", email)
            }

            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, OnboardingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}