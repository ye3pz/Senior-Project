package com.example.shade.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shade.R


class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use fragment_threats.xml layout for this screen
        val view =  inflater.inflate(R.layout.fragment_settings, container, false)

        val helpButton: ImageButton = view.findViewById(R.id.btn_help)
        helpButton.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_helpFragment)
        }

    try {
        Log.i("Help", "Help fagment loading")
    } catch (e :Exception){
        Log.e("Help", " Help Fragment failed to load", e)
    }
        return view
    }

}