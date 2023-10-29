package com.example.sportbookingapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sportbookingapp.backend_classes.SportField
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class SportsRecyclerviewAdapter(private val sportsFieldsList: ArrayList<SportField>):
    RecyclerView.Adapter<SportsRecyclerviewAdapter.SportsRecyclerviewViewHolder>() {

    private var itemClickListener: OnItemClickListener? = null
    private var selectedPosition = -1 // no image is selected

    fun getSelectedPosition(): Int {
        return selectedPosition
    }

    fun setSelectedPosition(pos: Int) {
        selectedPosition = pos
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SportsRecyclerviewViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.sports_recycler_view_item, parent, false)
        return SportsRecyclerviewViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SportsRecyclerviewViewHolder,
                                  @SuppressLint("RecyclerView") position: Int) {
        val currentItem = sportsFieldsList[position]
        Picasso.get()
            .load(currentItem.getImageUrl())

            .error(R.drawable.sports_field_background) // Replace with your error placeholder image
            .into(holder.sportImage, object : Callback {
                override fun onSuccess() {
                    // Image loaded successfully
                }
                override fun onError(e: Exception?) {
                    Log.e(TAG, "Error loading image from URL: ${currentItem.getImageUrl()}", e)
                }
            })
        holder.sportName.text = currentItem.getSportCategory()

        if (position == selectedPosition) {
            holder.itemView.findViewById<RelativeLayout>(R.id.sportsRecyclerviewLayout)
                .setBackgroundColor(Color.parseColor("#66F9B0"))
        } else {
            holder.itemView.findViewById<RelativeLayout>(R.id.sportsRecyclerviewLayout)
                .setBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            if (selectedPosition != position) {
                // Deselect the previously selected item (if any)
                if (selectedPosition != -1) {
                    notifyItemChanged(selectedPosition)
                }
                // Select the clicked item
                selectedPosition = position
                notifyItemChanged(selectedPosition)
            }
            itemClickListener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return sportsFieldsList.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    class SportsRecyclerviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sportImage: ImageView = itemView.findViewById(R.id.sportFieldImage)
        val sportName: TextView = itemView.findViewById(R.id.sportFieldText)
    }
}