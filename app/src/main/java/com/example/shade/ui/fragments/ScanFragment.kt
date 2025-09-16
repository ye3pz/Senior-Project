package com.example.shade.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shade.R

// ScanFragment is a modular UI component that displays the Scan screen content
class ScanFragment : Fragment() {

    // Called to create and return the view hierarchy associated with this fragment
    override fun onCreateView(
        inflater: LayoutInflater, // Used to inflate views from XML
        container: ViewGroup?,    // Parent view the fragment UI should be attached to (optional)
        savedInstanceState: Bundle? // Previous state if fragment is being re-created
    ): View? {
        // Inflate the layout for this fragment from fragment_scan.xml
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }
}