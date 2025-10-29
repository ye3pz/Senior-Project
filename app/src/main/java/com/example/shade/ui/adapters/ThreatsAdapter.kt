package com.example.shade.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shade.R
import com.example.shade.ThreatsList




class ThreatsAdapter : RecyclerView.Adapter<ThreatsAdapter.ThreatViewHolder>() {
        val threatList = ThreatsList.activeThreats
        // 1. ViewHolder: Holds references to the views for one list item
        class ThreatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleTextView: TextView = itemView.findViewById(R.id.threat_title)
            val descriptionTextView: TextView = itemView.findViewById(R.id.threat_description)
        }

        // 2. onCreateViewHolder: Called when the RecyclerView needs a new ViewHolder
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_threat, parent, false) // Use your list item layout
            return ThreatViewHolder(view)
        }

        // 3. onBindViewHolder: Called by RecyclerView to display the data at the specified position
        override fun onBindViewHolder(holder: ThreatViewHolder, position: Int) {
            val currentThreat = threatList[position]
            holder.titleTextView.text = currentThreat.title
            holder.descriptionTextView.text = currentThreat.description
        }

        // 4. getItemCount: Returns the total number of items in the list
        override fun getItemCount(): Int {
            return threatList.size
        }
    }