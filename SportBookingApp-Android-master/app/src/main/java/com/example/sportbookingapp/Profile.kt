package com.example.sportbookingapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Profile.newInstance] factory method to
 * create an instance of this fragment.
 */
class Profile : Fragment() {
    private var db = Firebase.firestore

    @SuppressLint("CutPasteId", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val sharedPreference =
            requireActivity().getSharedPreferences("AuthenticatedUser", Context.MODE_PRIVATE)
        val authenticatedEmail = sharedPreference.getString("email", "guest")

        val logoutTextView = view.findViewById<TextView>(R.id.logout_textView)
        logoutTextView.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // clear the user session
            val loginIntent = Intent(context, Login::class.java)
            startActivity(loginIntent)
        }

        val fullNameTextView = view.findViewById<TextView>(R.id.fullNameTextView)
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    val email = document.getString("email")
                    Log.d("authUser", "Current (from DB fetch) user is $email\n")
                    if (email == authenticatedEmail) {
                        val firstName = document.getString("firstName")
                        val lastName = document.getString("lastName")
                        val phoneNumber = document.getString("phoneNumber")

                        // Set the retrieved profile data to the corresponding EditText fields
                        view.findViewById<TextView>(R.id.emailTextView).text = email
                        view.findViewById<EditText>(R.id.firstNameEditText).setText(firstName)
                        view.findViewById<EditText>(R.id.lastNameEditText).setText(lastName)
                        view.findViewById<EditText>(R.id.phoneNumberEditText).setText(phoneNumber)
                        fullNameTextView.text = "Welcome, $firstName $lastName!"
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Error occurred while retrieving profile data
                Log.e(TAG, "Error retrieving profile data: ${exception.message}")
            }

        // Get a reference to the Save button
        val saveButton = view.findViewById<Button>(R.id.saveButton)

        // Set a click listener on the Save button
        saveButton.setOnClickListener {
            // Get the updated values from the TextView fields
            val email = view.findViewById<TextView>(R.id.emailTextView).text.toString()
            val firstName = view.findViewById<TextView>(R.id.firstNameEditText).text.toString()
            val lastName = view.findViewById<TextView>(R.id.lastNameEditText).text.toString()
            val phoneNumber = view.findViewById<TextView>(R.id.phoneNumberEditText).text.toString()

            // Find the user document with the matching email
            val userQuery = db.collection("users").whereEqualTo("email", email)
            if (phoneNumber.length == 10) {
                userQuery.get().addOnSuccessListener { documents ->
                    // There should only be one document matching the query
                    if (documents.size() == 1) {
                        val userDoc = documents.documents[0]
                        // Update the existing user document with the new field values
                        userDoc.reference.update("firstName", firstName)
                        userDoc.reference.update("lastName", lastName)
                        userDoc.reference.update("phoneNumber", phoneNumber)
                    } else {
                        // Handle the case where no user document was found or multiple documents were found
                    }
                    fullNameTextView.text = "Welcome, $firstName $lastName!"
                    Toast.makeText(context, "Data successfully updated!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Phone number should have 10 digits!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        return view
    }
}
