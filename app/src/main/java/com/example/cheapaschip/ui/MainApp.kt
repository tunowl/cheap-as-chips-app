package com.example.cheapaschip.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cheapaschip.ui.DashboardScreen
import com.example.cheapaschip.ui.ContactScreen
import com.example.cheapaschip.ui.ScooterScreen
import com.example.cheapaschip.ui.FollowUpScreen
import com.example.cheapaschip.ui.SettingsScreen

enum class AppScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    CONTACT("Contract", Icons.Default.Edit),
    SCOOTER("Scooters", Icons.Default.ShoppingCart),
    FOLLOW_UP("Follow Up", Icons.Default.List),
    MORE("More", Icons.Default.Menu)
}

@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var preselectedScooterId by remember { mutableStateOf<String?>(null) }
    val contactState = remember { ContactScreenState() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppScreen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Modifier.padding(innerPadding)
        val modifier = Modifier.padding(innerPadding)

        when (currentScreen) {
            AppScreen.DASHBOARD -> DashboardScreen(
                modifier = modifier,
                onNavigateToScooters = { currentScreen = AppScreen.SCOOTER }
            )
            AppScreen.CONTACT -> ContactScreen(
                modifier = modifier,
                preselectedScooterId = preselectedScooterId,
                onClearSelection = { preselectedScooterId = null },
                onContractGenerated = { 
                    currentScreen = AppScreen.FOLLOW_UP 
                },
                state = contactState
            )
            AppScreen.SCOOTER -> ScooterScreen(
                modifier = modifier,
                onRentClick = { scooterId ->
                    contactState.reset()
                    preselectedScooterId = scooterId
                    currentScreen = AppScreen.CONTACT
                }
            )
            AppScreen.FOLLOW_UP -> FollowUpScreen(
                modifier = modifier,
                onNewRentalClick = { currentScreen = AppScreen.SCOOTER }
            )
            AppScreen.MORE -> SettingsScreen(
                modifier = modifier
            )
        }
    }
}
