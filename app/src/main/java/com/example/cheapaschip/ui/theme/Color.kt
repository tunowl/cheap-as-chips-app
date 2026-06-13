package com.example.cheapaschip.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.cheapaschip.data.RentalRepository

// Primary Accent Palette - Dynamically mapped to the active theme color psychology settings
val BrandBlueMedium: Color
    get() = RentalRepository.activeTheme.primary

val BrandBlueDark: Color
    get() = RentalRepository.activeTheme.primaryDark

val BrandBlueLight: Color
    get() = RentalRepository.activeTheme.primaryLight

// Secondary Green Palette
val BrandGreenDark = Color(0xFF047857)
val BrandGreenMedium = Color(0xFF10B981)
val BrandGreenLight = Color(0xFFECFDF5)

// Tertiary Yellow Palette
val BrandYellowDark = Color(0xFFB45309)
val BrandYellowMedium = Color(0xFFF59E0B)
val BrandYellowLight = Color(0xFFFEF3C7)

// Neutral Colors
val SurfaceWhite = Color(0xFFFFFFFF)
val BackgroundSlate = Color(0xFFF8FAFC)
val CardBorderSlate = Color(0xFFE2E8F0)
val TextDark = Color(0xFF0F172A)
val TextGray = Color(0xFF64748B)