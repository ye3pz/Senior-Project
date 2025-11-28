package com.example.shade.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.shade.R
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shade.ThreatsList
import com.example.shade.data.FirebaseClient
import com.example.shade.ui.adapters.ThreatsAdapter
import android.util.Log


class ThreatsFragment : Fragment() {
    val log_tag = "ThreatsFragment"
    val threatList = ThreatsList.threatMap
    // Called to create and return the view hierarchy associated with this fragment // Inflates and returns the view for this fragment
       override fun onCreateView(
         inflater: LayoutInflater,
           container: ViewGroup?,
           savedInstanceState: Bundle?
       ): View? {
            // Use fragment_threats.xml layout for this screen
           return inflater.inflate(R.layout.fragment_threats, container, false)
       }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            setupRecyclerView(view)

    }

    private fun setupRecyclerView(fragmentView: View) {
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.recyclerView_threats)
        val noThreatsTextView: TextView = fragmentView.findViewById(R.id.txt_no_threats)

        if (ThreatsList.threatMap.isNotEmpty()) {
            recyclerView.layoutManager = LinearLayoutManager(context)
            // *** Adapter uses the UI-specific list and ThreatItem data class ***
            val adapter = ThreatsAdapter()
            recyclerView.adapter = adapter
            recyclerView.visibility = View.VISIBLE
            noThreatsTextView.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            noThreatsTextView.visibility = View.VISIBLE
        }
    }

}