import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sportbookingapp.ReservationStatusAdapter
import com.example.sportbookingapp.R
import com.example.sportbookingapp.backend_classes.Reservation
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReservationStatus : Fragment() {
    private lateinit var newRecyclerView: RecyclerView
    private lateinit var newArrayList: ArrayList<Reservation>
    private lateinit var adapter: ReservationStatusAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var db = Firebase.firestore
    private lateinit var currentUser: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reservation_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences =
            requireActivity().getSharedPreferences("userEmail", Context.MODE_PRIVATE)
        currentUser = sharedPreferences.getString("userEmail", "currentUser").toString()
        newRecyclerView = view.findViewById(R.id.reservationRecyclerView)
        newRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        newRecyclerView.setHasFixedSize(true)

        newArrayList = arrayListOf()
        getUserdata()

        adapter = ReservationStatusAdapter(newArrayList)
        newRecyclerView.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getUserdata() {
        db.collection("reservations")
            .get()
            .addOnSuccessListener { result ->
                Log.d("fetch", "FETCH_RESULT : " + result.documents + "\n")
                for (document in result.documents) {
                    //val id = document.id
                    val timestamp = document.getTimestamp("date")
                    val dateFormat = SimpleDateFormat("E MMM dd yyyy", Locale.getDefault())
                    val date: String = timestamp?.toDate()?.let { dateFormat.format(it) } ?: ""
                    val endingHour = document.getLong("endingHour")?.toInt()
                    val fieldId = document.getString("field_id")
                    val price = document.getLong("price") ?: 0
                    val startingHour = document.getLong("startingHour")?.toInt()
                    val status = document.getString("status")
                    val booker = document.getString("booker")

                    if (booker != null) {
                        if (booker == currentUser) {
                            if (endingHour != null && fieldId != null && startingHour != null && status != null) {
                                val reservation = Reservation(
                                    date,
                                    endingHour,
                                    fieldId,
                                    price,
                                    startingHour,
                                    status,
                                    adapter
                                )
                                newArrayList.add(reservation)
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.d("Gigel", "Error getting sport fields data from Firestore.", exception)
            }
    }
}
