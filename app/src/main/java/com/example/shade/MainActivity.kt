
/*import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.shade.databinding.ActivityMainBinding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.shade.data.FirebaseClient


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
                startService(Intent(this, Network::class.java))
            } else {
                // User denied VPN permission
            }
        }


        // Request VPN permission
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startService(Intent(this, Network::class.java))
        }


        // ✅ Set up Navigation Component
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // ✅ Connect BottomNavigationView with NavController
        binding.bottomNavigation.setupWithNavController(navController)
    }



}
*/






package com.example.shade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.shade.ui.theme.AppDetailsPage1
import com.example.shade.ui.theme.HistoryItem
import com.example.shade.ui.theme.HistoryPage
import com.example.shade.ui.theme.MenuScreen
import com.example.shade.ui.theme.ScanPage
import com.example.shade.ui.theme.UploadPage
import com.example.shade.ui.theme.ShadeTheme
import com.example.shade.ui.theme.UserGuidePage
import com.example.shade.ui.theme.HomePage
import com.example.shade.ui.theme.AboutUs



enum class ActiveScreen {
    MAIN,           // normal bottom-nav layout
    USER_GUIDE,     // User Guide page
    APP_DETAILS_1,   // App Details page

    ABOUT_US         //About Us Page
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShadeTheme {
                var isMenuOpen by remember { mutableStateOf(false) }
                var selectedTab by remember { mutableStateOf(1) }        // 0=Home, 1=Scan, 2=Upload, 3=History
                var activeScreen by remember { mutableStateOf(ActiveScreen.MAIN) }

                // currently selected app in History (for details screen)
                var selectedApp by remember { mutableStateOf<HistoryItem?>(null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // 1) Menu overlay
                        isMenuOpen -> {
                            MenuScreen(
                                onClose = { isMenuOpen = false },
                                onSettingsClick = {
                                    // TODO: open Settings later

                                },
                                onUserGuideClick = {
                                    isMenuOpen = false
                                    activeScreen = ActiveScreen.USER_GUIDE
                                },
                                onAboutUsClick = {
                                    // TODO: About Us later
                                    isMenuOpen = false
                                    activeScreen = ActiveScreen.ABOUT_US
                                }
                            )
                        }

                        // 2) User Guide full-screen
                        activeScreen == ActiveScreen.USER_GUIDE -> {
                            UserGuidePage(
                                onBackClick = {
                                    activeScreen = ActiveScreen.MAIN
                                     isMenuOpen = true
                                }
                            )
                        }

                        //About Us full-screen
                        activeScreen == ActiveScreen.ABOUT_US -> {
                            AboutUs(
                                onBackClick = {
                                    activeScreen = ActiveScreen.MAIN
                                    isMenuOpen = true
                                }
                            )
                        }

                        // 3) App Details (from History)
                        activeScreen == ActiveScreen.APP_DETAILS_1 && selectedApp != null -> {
                            AppDetailsPage1(
                                app = selectedApp!!,
                                onBackClick = { activeScreen = ActiveScreen.MAIN }
                            )
                        }

                        // 4) Normal bottom-nav screens
                        else -> {
                            Column(modifier = Modifier.fillMaxSize()) {

                                // Top part: changes per tab
                                Box(modifier = Modifier.weight(1f)) {
                                    when (selectedTab) {
                                        0 -> HomePage(
                                            onMenuClick = {isMenuOpen = true }
                                        )
                                        1 -> ScanPage(
                                            onMenuClick = { isMenuOpen = true }
                                        )
                                        2 -> UploadPage(
                                             onMenuClick = {isMenuOpen = true }
                                        )
                                        3 -> HistoryPage(
                                            onMenuClick = { isMenuOpen = true },
                                            onItemClick = { app ->
                                                selectedApp = app
                                                activeScreen = ActiveScreen.APP_DETAILS_1
                                            }
                                        )
                                    }
                                }

                                // Bottom navigation bar
                                NavigationBar(
                                    containerColor = Color(0xFF45266F)
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.Home,
                                                contentDescription = "Home"
                                            )
                                        },
                                        label = { Text("Home") }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.Security,
                                                contentDescription = "Scan"
                                            )
                                        },
                                        label = { Text("Scan") }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.CloudUpload,
                                                contentDescription = "Upload"
                                            )
                                        },
                                        label = { Text("Upload") }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 3,
                                        onClick = { selectedTab = 3 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = "History"
                                            )
                                        },
                                        label = { Text("History") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}







