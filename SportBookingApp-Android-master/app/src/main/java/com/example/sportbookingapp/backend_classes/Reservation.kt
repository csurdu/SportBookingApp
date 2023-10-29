package com.example.sportbookingapp.backend_classes

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import com.example.sportbookingapp.ReservationStatusAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@SuppressLint("NotifyDataSetChanged")
class Reservation(
    private var date: String,
    private var endingHour: Int,
    private var fieldId: String,
    private var price: Long,
    private var startingHour: Int,
    private var status: String = "",
    private var adapter: ReservationStatusAdapter
) {
    private var sportField: SportField? = null

    init {
        // Set the value of the sportField property
        Firebase.firestore.collection("fields")
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    val id = document.id

                    if (id == fieldId) {
                        val name = document.getString("name")
                        val imageUrl = document.getString("imageUrl")
                        val sportCategory = document.getString("sportCategory")
                        val price = document.getString("price").toString().toInt()
                        val description = document.getString("description")

                        if (name != null && imageUrl != null && sportCategory != null && description != null) {
                            val sportField =
                                SportField(id, name, imageUrl, sportCategory, price, description)
                            setSportField(sportField)
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "", exception)
            }
    }

    // Getter and Setter methods for private fields
    fun getDate(): String {
        return date
    }

    fun setDate(date: String) {
        this.date = date
    }

    fun getEndingHour(): Int {
        return endingHour
    }

    fun setEndingHour(endingHour: Int) {
        this.endingHour = endingHour
    }

    fun getFieldId(): String {
        return fieldId
    }

    fun setFieldId(fieldId: String) {
        this.fieldId = fieldId
    }

    fun getPrice(): Long {
        return price
    }

    fun setPrice(price: Long) {
        this.price = price
    }

    fun getStartingHour(): Int {
        return startingHour
    }

    fun setStartingHour(startingHour: Int) {
        this.startingHour = startingHour
    }

    fun getStatus(): String {
        return status
    }

    fun setStatus(status: String) {
        this.status = status
    }

    fun getSportField(): SportField? {
        return sportField
    }

    private fun setSportField(sportField: SportField) {
        this.sportField = sportField
    }
}
