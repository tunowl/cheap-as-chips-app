package com.example.cheapaschip

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cheapaschip.ui.MainApp
import com.example.cheapaschip.ui.theme.CheapAsChipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Supabase Configuration
        com.example.cheapaschip.data.SupabaseService.init(this)
        
        // Initial Sync with Supabase if configured
        if (com.example.cheapaschip.data.SupabaseService.isConfigured()) {
            com.example.cheapaschip.data.RentalRepository.syncWithSupabase(this) { success ->
                android.util.Log.d("MainActivity", "Initial Supabase sync success: $success")
            }
        }
        
        // Request notifications permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            CheapAsChipTheme {
                MainApp()
            }
        }
    }
}