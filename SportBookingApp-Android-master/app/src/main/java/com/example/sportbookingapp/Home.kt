package com.example.sportbookingapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sportbookingapp.backend_classes.SportField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Home.newInstance] factory method to
 * create an instance of this fragment.
 */
class Home : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    //Firebase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uid: String
    private lateinit var db: FirebaseFirestore

    // RecyclerView
    private lateinit var adapter: SportsRecyclerviewAdapter
    private lateinit var recyclerView: RecyclerView
    private val sportFields: ArrayList<SportField> = ArrayList()
    private var fieldPosition = -1

    // CalendarView
    private lateinit var calendarView: CalendarView
    private lateinit var calendar: Calendar
    private lateinit var selectedDate: Date

    // SpinnerView - Starting & Ending hour
    private lateinit var startingHourSpinner: Spinner
    private lateinit var endingHourSpinner: Spinner
    private lateinit var endingHourText: TextView
    private lateinit var startingHourText: TextView

    // Lists that populate the choices in the SpinnerView
    private var availableStartingHoursList = ArrayList<String>()
    private var availableEndingHoursList = ArrayList<String>()

    private var startingHour = 0
    private var endingHour = 0
    private var totalPrice = 0

    // hint used for spinners
    val hint = "Select an hour"

    private lateinit var priceTextView: TextView
    private lateinit var makeReservationButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        availableStartingHoursList.add(hint)
        availableEndingHoursList.add(hint)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Home.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Home().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        val sharedPref = requireActivity()
            .getSharedPreferences("HomeFragment", Context.MODE_PRIVATE)
        adapter.setSelectedPosition(sharedPref.getInt("selectedPosition", -1))
        // fetch data from firebase
        fetchDataFromDB()
        if (fieldPosition != -1) {
            startingReservationHoursInit()
        }

        // Make Reservations data upload to Firebase
        makeReservationButton.setOnClickListener {
            val emailPreferences =
                requireActivity().getSharedPreferences("userEmail", Context.MODE_PRIVATE)
            val email = emailPreferences.getString("userEmail", "guest")

            val startingH: Int
            val endingH: Int

            if (startingHour == endingHour) {
                Toast.makeText(context, "Can't book a null reservation", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (startingHour < endingHour) {
                startingH = startingHour
                endingH = endingHour
            } else {
                endingH = startingHour
                startingH = endingHour
            }
            val selectedField = sportFields[adapter.getSelectedPosition()]

            // Check if the selected field has any reservations
            db.collection("reservations")
                .get()
                .addOnSuccessListener { result ->
                    // Iterate over each reservation
                    for (document in result.documents) {
                        val reservationStartingHour = document.getLong("startingHour")
                        val reservationEndingHour = document.getLong("endingHour")

                        // Check if the reservation interval overlaps with the desired reservation
                        if (document.getString("field_id") == selectedField.getId() &&
                                reservationStartingHour != null && reservationEndingHour != null) {
                            val start: Long
                            val end: Long
                            if (reservationStartingHour < reservationEndingHour) {
                                start = reservationStartingHour
                                end = reservationEndingHour
                            } else {
                                end = reservationStartingHour
                                start = reservationEndingHour
                            }

                            val reservationDate = document.getTimestamp("date")?.toDate()
                            // If the new Reservation overlaps with other reservation,
                            // we will block it
                            if (!reservationAvailable(startingH, endingH, start, end) &&
                                selectedDate == reservationDate
                            ) {
                                val overlappingInterval =
                                    if (reservationStartingHour < reservationEndingHour) {
                                        "$reservationStartingHour - $reservationEndingHour"
                                    } else {
                                        "$reservationEndingHour - $reservationStartingHour"
                                    }
                                val message = "Interval $overlappingInterval already booked."
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }
                        }
                    }

                    // No overlapping reservations found, save the reservation to Firebase
                    val reservation = hashMapOf(
                        "startingHour" to startingHour,
                        "endingHour" to endingHour,
                        "field_id" to selectedField.getId(),
                        "date" to selectedDate,
                        "booker" to email,
                        "price" to totalPrice,
                        "status" to "pending"
                    )

                    db.collection("reservations")
                        .add(reservation)
                        .addOnSuccessListener { documentReference ->
                            // Reservation added successfully
                            val reservationId = documentReference.id
                            Toast.makeText(
                                context,
                                "Reservation added successfully",
                                Toast.LENGTH_LONG
                            ).show()
                            priceTextView.visibility = View.INVISIBLE
                            makeReservationButton.visibility = View.INVISIBLE
                            startingReservationHoursInit()
                        }
                        .addOnFailureListener { exception ->
                            // Error occurred while adding reservation
                            Log.e(TAG, "Error adding reservation to Firestore", exception)
                        }
                }
                .addOnFailureListener { exception ->
                    // Error occurred while fetching reservations
                    Log.e(TAG, "Error fetching reservations from Firestore", exception)
                }
        }

    }

    private fun reservationAvailable(
        startingHour: Int,
        endingHour: Int,
        start: Long,
        end: Long
    ): Boolean {
        if (endingHour <= start) {
            return true
        }
        if (startingHour >= end) {
            return true
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        val sharedPref = requireActivity()
            .getSharedPreferences("HomeFragment", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("selectedPosition", adapter.getSelectedPosition())
            putLong("date", selectedDate.time) // date in ms
            commit()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Firebase sync and data init
        firebaseAuth = FirebaseAuth.getInstance()
        uid = firebaseAuth.currentUser?.uid.toString()
        db = Firebase.firestore

        // Recyclerview
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView = view.findViewById(R.id.sportsRecyclerView)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        adapter = SportsRecyclerviewAdapter(sportFields)
        adapter.setOnItemClickListener(object : SportsRecyclerviewAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                priceTextView.visibility = View.INVISIBLE
                startingReservationHoursInit() // it resets the spinner's view
            }
        })
        recyclerView.adapter = adapter

        // Initialize CalendarView
        calendarView = view.findViewById(R.id.calendarView)
        calendar = Calendar.getInstance()
        // Set minimum date to today
        val minDate = Calendar.getInstance()
        minDate.set(Calendar.HOUR_OF_DAY, 0)
        minDate.set(Calendar.MINUTE, 0)
        minDate.set(Calendar.SECOND, 0)
        calendarView.minDate = minDate.timeInMillis
        // Set maximum date to four weeks from now
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.WEEK_OF_YEAR, 4)
        maxDate.set(Calendar.HOUR_OF_DAY, 0)
        maxDate.set(Calendar.MINUTE, 0)
        maxDate.set(Calendar.SECOND, 0)
        calendarView.maxDate = maxDate.timeInMillis
        // init the selectedDate by default
        selectedDate = Date(calendarView.minDate)
        // Set a listener to handle when the selected date changes
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Do something with the selected date
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDate = calendar.time
        }
        startingHourSpinner = view.findViewById(R.id.startingHourSpinner)
        endingHourSpinner = view.findViewById(R.id.endingHourSpinner)
        endingHourText = view.findViewById(R.id.endingHourTextView)
        startingHourText = view.findViewById(R.id.startingHourTextView)
        priceTextView = view.findViewById(R.id.priceText)

        // Make Reservation Button
        makeReservationButton = view.findViewById(R.id.confirm_reservation_button)
    }

    private fun startingReservationHoursInit() {
        // Show the description of the selected field
        var fieldDescription = "Selected field description:\n"
        fieldDescription += sportFields[adapter.getSelectedPosition()].getDescription()
        priceTextView.setTextColor(Color.parseColor("#66F9B0"))
        priceTextView.setTypeface(null, Typeface.BOLD);
        priceTextView.text = fieldDescription
        priceTextView.visibility = View.VISIBLE

        // Add a hint or prompt to the availableStartingHoursList
        val startingHourAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            availableStartingHoursList
        )
        startingHourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        startingHourSpinner.adapter = startingHourAdapter
        startingHourSpinner.visibility = View.VISIBLE
        startingHourText.visibility = View.VISIBLE

        // onItemSelected Listener
        startingHourSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position)
                if (selectedItem is String && selectedItem != hint) {
                    startingHour = selectedItem.toInt()
                    endingReservationHoursInit()
                } else if (selectedItem is String && selectedItem == hint) {
                    endingHourSpinner.visibility = View.INVISIBLE
                    endingHourText.visibility = View.INVISIBLE
                    startingHour = -1
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                endingHourSpinner.visibility = View.INVISIBLE
                endingHourText.visibility = View.INVISIBLE
            }
        }
    }

    private fun endingReservationHoursInit() {
        val endingHourAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            availableEndingHoursList
        )
        endingHourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        endingHourSpinner.adapter = endingHourAdapter
        endingHourSpinner.visibility = View.VISIBLE
        endingHourText.visibility = View.VISIBLE

        // onItemSelected Listener
        endingHourSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position)
                if (selectedItem is String && selectedItem != hint) {
                    endingHour = selectedItem.toInt()
                    fieldPosition = adapter.getSelectedPosition()
                    if (fieldPosition != -1) {
                        totalPrice = calculateTotalPrice(sportFields[fieldPosition].getPrice())
                    }
                    val priceString = getString(R.string.price_text, totalPrice.toString())
                    priceTextView.text = priceString
                    priceTextView.visibility = View.VISIBLE
                    makeReservationButton.visibility = View.VISIBLE
                } else if (selectedItem is String && selectedItem == hint) {
                    endingHour = -1
                    priceTextView.visibility = View.INVISIBLE
                    makeReservationButton.visibility = View.INVISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun calculateTotalPrice(price: Int): Int {
        return price * abs(endingHour - startingHour)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun fetchDataFromDB() {
        GlobalScope.launch(Dispatchers.IO) {
            fetchSportFieldsFromDB()
        }
        GlobalScope.launch(Dispatchers.IO) {
            fetchAvailableReservationHours()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchSportFieldsFromDB() {
        db.collection("fields")
            .get()
            .addOnSuccessListener { result ->
                Log.d("fetch", "FETCH_RESULT : " + result.documents + "\n")
                for (document in result.documents) {
                    val id = document.id
                    val name = document.getString("name")
                    val imageUrl = document.getString("imageUrl")
                    val sportCategory = document.getString("sportCategory")
                    val description = document.getString("description")
                    val price = try {
                        document.getString("price").toString().toInt()
                    } catch (e: Exception) {
                        document.getLong("price").toString().toInt()
                    }

                    if (name != null && imageUrl != null && sportCategory != null && description != null) {
                        val sportField =
                            SportField(id, name, imageUrl, sportCategory, price, description)
                        sportFields.add(sportField)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.d("fetch", "Error getting sport fields data from Firestore.", exception)
            }
    }

    private fun fetchAvailableReservationHours() {
        val start = ArrayList<String>()
        val end = ArrayList<String>()

        // default values
        var x = 0
        var y = 24

        db.collection("open_hours")
            .get()
            .addOnSuccessListener { result ->
                if (result.documents.size >= 1) {
                    x = result.documents[0].getString("startingHour").toString().toInt()
                    y = result.documents[0].getString("endingHour").toString().toInt()
                }
            }
            .addOnCompleteListener {
                for (i in x..y) {
                    start.add(i.toString())
                    end.add(i.toString())
                }

                // update the hours for spinners
                availableStartingHoursList += start
                availableEndingHoursList += end
            }
    }
}
