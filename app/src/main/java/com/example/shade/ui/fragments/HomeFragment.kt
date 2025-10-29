package com.example.shade.ui.fragments

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.shade.Permissions
import com.example.shade.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.shade.ThreatsList
import com.example.shade.data.ThreatItem


class HomeFragment : Fragment() {

    private lateinit var permissions: Permissions

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissions = Permissions(requireContext())

        val scanButton = view.findViewById<Button>(R.id.scan)
        scanButton.setOnClickListener {
            if (permissions.hasUsageStatsPermission()) {
                lifecycleScope.launch {
                    startScan()
                }
            } else {
                permissions.requestUsageStatsPermission()
            }
        }
    }

    private  suspend fun startScan() {
        permissions.requestUsageStatsPermission()
        permissions.scanAppPermissions()
        permissions.checkNumberOfPermissions()
        permissions.scanAppSignatures()

        val pm = requireContext().packageManager
        val apps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val activeThreats = ThreatsList.activeThreats



        for (app in apps) {

            try {
                    val appInfo = app.applicationInfo?: run { // makes sure appInfo !=  null
                        Log.i(tag, "ApplicationInfo  is null ")
                        return
                    }
                    val appName = pm.getApplicationLabel(appInfo);

                    val riskScore = permissions.getSafetyRating(app)
                    val label = permissions.getSafetyLabel(riskScore)
                    Log.i("AppSafety", "App: ${app.packageName}, Score: $riskScore, Label: $label")
                    Toast.makeText(
                        requireContext(),
                        "App: ${app.packageName} → $label",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (label == "Dangerous") {
                        val appThreatItem = app.toThreatItem(appName)
                        ThreatsList.activeThreats.add(appThreatItem)
                    }



            } catch (e: Exception) {
                Log.e("HomeFragment", "Error evaluating safety for ${app.packageName}", e)
            }
        }
    }
    fun PackageInfo.toThreatItem(appName: CharSequence): ThreatItem {

        // Use the package name as the primary identifier (title)
        val titleText = this.packageName

        // Construct a detailed description
        val descriptionText = "App Name: $appName (Risk: Dangerous)" +
                "\nVersion: ${this.versionName ?: "N/A"}" +
                "\nSource: App Scan"

        return ThreatItem(
            title = titleText,
            description = descriptionText
        )
    }
}

