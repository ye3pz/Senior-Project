package com.example.shade.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.shade.R
import com.example.shade.Permissions
import kotlinx.coroutines.launch
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream
import android.net.Uri
import android.content.Context
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.shade.data.AI_Client.UploadAndScanFile


// ScanFragment is a modular UI component that displays the Scan screen content
class ScanFragment : Fragment() {
    val log_tag = "scanFragment"
    val QuickScan = HomeFragment()

    private lateinit var permissions: Permissions
    // Called to create and return the view hierarchy associated with this fragment

    private var filepath: String = ""
    val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { processFileUri(uri) } ?: Log.e(log_tag, "File selection cancelled or failed to process")

        }


    override fun onCreateView(
        inflater: LayoutInflater, // Used to inflate views from XML
        container: ViewGroup?,    // Parent view the fragment UI should be attached to (optional)
        savedInstanceState: Bundle? // Previous state if fragment is being re-created
    ): View? {
        // Inflate the layout for this fragment from fragment_scan.xml
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var option = null

        permissions = Permissions(requireContext())

        val quickScanButton: Button = view.findViewById(R.id.quick_scan)
        val scanFileButton: Button = view.findViewById(R.id.file_scan)

        // single click listener instance
        val buttonClickListener = View.OnClickListener { button ->
            // Use 'when' on the View's ID (v.id) to handle different buttons
            when (button.id) {
                R.id.quick_scan -> {
                    handleQuickScan()
                }

                R.id.file_scan -> {
                    handleFileScan()
                }

                else -> {
                    Log.e(log_tag, "Button click failed")
                }
            }
        }
        // Set the single listener instance on both buttons
        quickScanButton.setOnClickListener(buttonClickListener)
        scanFileButton.setOnClickListener(buttonClickListener)
    }

    private fun handleQuickScan() {
        if (permissions.hasUsageStatsPermission()) {
            // Launch the suspending call within the fragment's lifecycle scope
            lifecycleScope.launch {
                // Assuming QuickScan is correctly initialized and startScan() is a suspend function
                QuickScan.startScan()
            }
        } else {
            permissions.requestUsageStatsPermission()
        }
    }

    private fun handleFileScan() {
        // You should check permissions here too, or structure the check differently
        if (permissions.hasUsageStatsPermission()) {
            lifecycleScope.launch {
                startFileScan()
            }
        } else {
            permissions.requestUsageStatsPermission()
        }
    }

    private fun startFileScan() {
        filePickerLauncher.launch("application/vnd.android.package-archive")  // Standard APK

    }

    private fun convertURI(uri: Uri): String?{
        val contentResolver = requireContext().contentResolver
        val fileName = getFileName(requireContext(), uri) ?: "upload_scan_${System.currentTimeMillis()}.tmp"
        // Create a unique temporary file path
        val tempFile = File(requireContext().cacheDir, fileName)

        try {
            // openInputStream reads the content from the URI
            val inputStream: InputStream? = contentResolver.openInputStream(uri)

            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Crucial step: Return the real local path
            return tempFile.absolutePath

        } catch (e: Exception) {
            Log.e("ScanFragment", "Error converting URI to file", e)
            tempFile.delete() // Clean up failed file
            return null
        }
    }
    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
    private fun processFileUri(uri: Uri) {
        lifecycleScope.launch {
            val localFilePath = withContext(Dispatchers.IO) {
                convertURI(uri)
            }

            if (localFilePath != null) {
                withContext(Dispatchers.IO) {
                    Log.i(log_tag, localFilePath.toString())
                    UploadAndScanFile(localFilePath.toString())

                    //  Delete the temporary file after successful upload
                    File(localFilePath).delete()
                }
            } else {
                Log.e("ScanFragment", "Failed to create local file for upload.")
                // TODO: Show error message to user
            }
        }
    }

}