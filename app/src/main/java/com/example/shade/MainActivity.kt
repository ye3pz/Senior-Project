package com.example.shade

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.shade.databinding.ActivityMainBinding
import com.example.shade.ui.fragments.HomeFragment
import com.example.shade.ui.fragments.ScanFragment
import com.example.shade.ui.fragments.ThreatsFragment
import com.example.shade.ui.fragments.SettingsFragment
import com.example.shade.data.FirebaseClient
import com.example.shade.data.ThreatFeed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the launcher inside onCreate
        vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                startService(Intent(this, NetworkMonitorService::class.java))
            } else {
                // User denied VPN permission
            }
        }

        // Request VPN permission
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startService(Intent(this, NetworkMonitorService::class.java))
        }

        FirebaseClient.addIp("66.228.39.180", false) //test
        FirebaseClient.addIp("104_167_250_109", false)
        CoroutineScope(Dispatchers.IO).launch {
            val threats = ThreatFeed.fetchThreats()
            Log.d("ThreatFeedTest", "Fetched ${threats.size} threats (direct test)")
        }


        // Load default fragment (Home)
        replaceFragment(HomeFragment())

        // Set up bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_scan -> replaceFragment(ScanFragment())
                R.id.nav_threats -> replaceFragment(ThreatsFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                else -> false
            }
        }
    }


    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }
}
