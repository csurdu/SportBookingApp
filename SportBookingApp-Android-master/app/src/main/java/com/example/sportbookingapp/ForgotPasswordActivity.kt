package com.example.sportbookingapp

import android.content.Intent
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.log

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        onResume()
    }

    override fun onResume() {
        super.onResume()

        val sendEmailProgressBar = findViewById<ProgressBar>(R.id.sendEmailProgressBar);
        val emailTextInput = findViewById<TextInputEditText>(R.id.emailTextInput)
        val firebaseAuth = FirebaseAuth.getInstance()

        val confirmButton = findViewById<Button>(R.id.sendMail)
        confirmButton.setOnClickListener {
            if (emailTextInput.text.toString() != "" || emailTextInput.text.toString() != "Email") {
                //firebase sent reset password email
                sendEmailProgressBar.visibility = View.VISIBLE
                firebaseAuth.sendPasswordResetEmail(emailTextInput.text.toString())
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this@ForgotPasswordActivity,
                                "Password reset email sent!", Toast.LENGTH_SHORT).show()
                            sendEmailProgressBar.visibility = View.GONE
                            val loginActivity = Intent(this@ForgotPasswordActivity, Login::class.java)
                            this@ForgotPasswordActivity.startActivity(loginActivity)
                        } else {
                            sendEmailProgressBar.visibility = View.GONE
                            emailTextInput.setText("")
                            Toast.makeText(this@ForgotPasswordActivity,
                                "No account found with this email", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }
        }

        val backButton = findViewById<TextView>(R.id.forgotPasswordActivityBackButton)
        backButton.setOnClickListener {
            val loginActivity = Intent(this@ForgotPasswordActivity, Login::class.java)
            this@ForgotPasswordActivity.startActivity(loginActivity)
        }
    }
}