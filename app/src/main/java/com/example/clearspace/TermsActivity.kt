package com.example.clearspace

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val ivBack = findViewById<ImageView>(R.id.iv_back)
        val tvTermsContent = findViewById<TextView>(R.id.tv_terms_content)

        ivBack.setOnClickListener {
            finish()
        }

        tvTermsContent.text = """
            CLEARSPACE – TERMS & CONDITIONS
            
            Last Updated: April 2026
            
            Welcome to ClearSpace. By creating an account or using this application, you agree to the following terms.
            
            
            1. Purpose of the Application
            ClearSpace is a prototype application designed to help users reduce excessive usage of selected mobile applications through time limits, interruptions, and challenges.
            This app is developed for academic and demonstration purposes only.
            
            
            2. User Responsibility
            You are responsible for how you use ClearSpace.
            The app does not guarantee behavioral change, productivity improvement, or mental health outcomes.
            
            You agree not to misuse the app or attempt to bypass its intended functionality.
            
            
            3. Account Information
            When signing up, you agree to provide accurate information including your name, email, and password.
            You are responsible for maintaining the confidentiality of your account credentials.
            
            
            4. Data Usage
            ClearSpace may store limited user data such as:
            • Name and email
            • Usage session data
            • Gamification data (points, levels, streaks)
            
            This data is used only to provide core app functionality.
            
            
            5. No Guarantees
            ClearSpace is provided “as is” without any guarantees or warranties.
            We do not guarantee uninterrupted operation, accuracy, or effectiveness.
            
            
            6. Limitation of Liability
            ClearSpace and its developers are not responsible for:
            • Loss of data
            • App interruptions or errors
            • Any consequences resulting from app usage
            
            
            7. Prototype Disclaimer
            This application is a prototype created for educational purposes.
            It is not intended for commercial use or production-level deployment.
            
            
            8. Changes to Terms
            These terms may be updated at any time without prior notice.
            Continued use of the app means you accept any changes.
            
            
            9. Contact
            For any questions regarding these terms, please contact the development team.
            
            
            By using ClearSpace, you acknowledge that you have read and agree to these Terms & Conditions.
        """.trimIndent()
    }
}