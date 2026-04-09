package com.example.clearspace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<EditText>(R.id.et_signup_name)
        val etEmail = findViewById<EditText>(R.id.et_signup_email)
        val etPassword = findViewById<EditText>(R.id.et_signup_password)
        val btnCreateAccount = findViewById<Button>(R.id.btn_create_account)
        val tvBackToLogin = findViewById<TextView>(R.id.tv_back_to_login)

        tvBackToLogin.setOnClickListener {
            finish() // 현재 화면 종료 후 로그인 화면으로 복귀
        }

        btnCreateAccount.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 실제 회원가입 API 호출 로직 위치

            // 임시로 SharedPreferences에 유저 정보 저장 (React Context의 login 기능 대체)
            val sharedPref = getSharedPreferences("ClearSpacePrefs", MODE_PRIVATE)
            sharedPref.edit()
                .putString("userName", name)
                .putString("userEmail", email)
                .apply()

            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

            // 가입 완료 후 온보딩 화면으로 이동 (로그인 화면은 백스택에서 제거)
            val intent = Intent(this, OnboardingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}