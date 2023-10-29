package com.example.sportbookingapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.lang.Exception
import java.util.logging.Logger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class Registration : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private var backgroundImage: ImageView? = null
    private var backgroundImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        getBackgroundImageUrl()
        onResume()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        var email: String
        var password: String
        var firstName: String
        var lastName: String
        var phoneNumber: String

        val backButton = findViewById<TextView>(R.id.backButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val additionalMessage = findViewById<TextView>(R.id.additionalMessageTextView)
        val editTextEmail = findViewById<TextInputEditText>(R.id.loginEmailTextInput)
        val editTextPassword = findViewById<TextInputEditText>(R.id.loginPasswordTextInput)
        val editTextFirstName = findViewById<TextInputEditText>(R.id.firstNameTextInput)
        val editTextLastName = findViewById<TextInputEditText>(R.id.lastNameTextInput)
        val editTextPhoneNumber = findViewById<TextInputEditText>(R.id.phoneNumberTextInput)
        backgroundImage = findViewById(R.id.backgroundRegistrationImage)

        backButton.setOnClickListener {
            val loginActivity = Intent(this@Registration, Login::class.java)
            this@Registration.startActivity(loginActivity)
        }

        registerButton.setOnClickListener {
            email = editTextEmail.text.toString()
            password = editTextPassword.text.toString()
            firstName = editTextFirstName.text.toString()
            lastName = editTextLastName.text.toString()
            phoneNumber = editTextPhoneNumber.text.toString()

            additionalMessage.isVisible = false
            var valid = true

            if (valid && !emailFormatValid(email)) {
                additionalMessage.text = "Email has invalid format. (ex.: nicusor@yahoo.com)"
                additionalMessage.isVisible = true
                valid = false
            }

            if (valid && !passwordFormatValid(password)) {
                additionalMessage.text = "Password must contain at least 6 characters."
                additionalMessage.isVisible = true
                valid = false
            }

            if (valid && !firstNameFormatValid(firstName)) {
                additionalMessage.text = "First Name can't be empty."
                additionalMessage.isVisible = true
                valid = false
            }

            if (valid && !lastNameFormatValid(lastName)) {
                additionalMessage.text = "Last Name can't be empty."
                additionalMessage.isVisible = true
                valid = false
            }

            if (valid && !phoneNumberFormatValid(phoneNumber)) {
                additionalMessage.text = "Phone number must be 10 digits long."
                additionalMessage.isVisible = true
                valid = false
            }

            if (valid) {
                try {
                    firebaseSignUp(email, password, firstName, lastName, phoneNumber, additionalMessage)
                } catch (e: Exception) {
                    val logger = Logger.getLogger(this.javaClass.name)
                    logger.warning(e.message)
                }
            }
        }
    }

    private fun emailFormatValid(email: String): Boolean {
        return email != "Email" && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun passwordFormatValid(password: String): Boolean {
        return password.length >= 6 && password != "Password"
    }

    private fun phoneNumberFormatValid(phoneNumber: String): Boolean {
        return phoneNumber.length == 10 && phoneNumber != "Phone Number"
    }

    private fun lastNameFormatValid(lastName: String): Boolean {
        return lastName != "Last Name" && lastName != ""
    }

    private fun firstNameFormatValid(firstName: String): Boolean {
        return firstName != "First Name" && firstName != ""
    }

    private fun firebaseSignUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        additionalMessage:TextView
    ) {
        val progressBar = findViewById<ProgressBar>(R.id.registrationProgressBar)
        progressBar.visibility = View.VISIBLE
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    val loginActivity = Intent(this@Registration, Login::class.java)
                    loginActivity.putExtra("email", email)
                    try {
                        addUserDataToDB(email, firstName, lastName, phoneNumber)
                    } catch (e: Exception) {
                        val logger = Logger.getLogger(this.javaClass.name)
                        logger.warning(e.message)
                    }
                    this@Registration.startActivity(loginActivity)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    additionalMessage.text = task.exception?.message
                    additionalMessage.isVisible = true
                }
            }
    }

    private fun addUserDataToDB(
        email: String,
        firstName: String,
        lastName: String,
        phoneNumber: String
    ) {
        val user = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "email" to email
        )

        db.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                // User added successfully
                val userId = documentReference.id
            }
            .addOnFailureListener { exception ->
                // Error occurred while adding user
                Log.e(TAG, "Error adding userData to Firestore", exception)
            }
    }

    private fun getBackgroundImageUrl() {
        Firebase.firestore.collection("backgroundImage")
            .get()
            .addOnSuccessListener { result ->
                backgroundImageUrl = result.documents[0].getString("imageUrl")
            }
            .addOnCompleteListener {
                updateBackgroundImage()
            }
    }

    private fun updateBackgroundImage() {
        Picasso.get()
            .load(backgroundImageUrl)
            .error(R.drawable.sports_field_background) // Replace with your error placeholder image
            .into(backgroundImage, object : Callback {
                override fun onSuccess() {
                    // Image loaded successfully
                }
                override fun onError(e: Exception?) {
                    Log.e(
                        "BackgroundImageError",
                        "Error loading image from URL: $backgroundImageUrl",
                        e
                    )
                }
            })
    }
}