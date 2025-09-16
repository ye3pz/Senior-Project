package com.example.shade.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shade.R


class ThreatsFragment : Fragment() {
    // Called to create and return the view hierarchy associated with this fragment // Inflates and returns the view for this fragment
       override fun onCreateView(
         inflater: LayoutInflater,
           container: ViewGroup?,
           savedInstanceState: Bundle?
       ): View? {
            // Use fragment_threats.xml layout for this screen
           return inflater.inflate(R.layout.fragment_threats, container, false)
       }

}