package com.example.cheapaschip.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Path
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.ui.graphics.Color

enum class AppThemeColor(
    val displayName: String,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val traits: String
) {
    BLUE("Blue", Color(0xFF2563EB), Color(0xFF1E3A8A), Color(0xFFEFF6FF), "peace, stability, calmness, confidence, tranquility, sincerity, affection, integrity"),
    GREEN("Green", Color(0xFF10B981), Color(0xFF047857), Color(0xFFECFDF5), "life, growth, environment, healing, money, safety, relaxation, freshness"),
    PURPLE("Purple", Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFFF5F3FF), "royalty, luxury, dignity, wisdom, spirituality, passion, vision, magic"),
    YELLOW("Yellow", Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFFFEF3C7), "joy, cheerfulness, friendliness, intellect, energy, warmth, caution, cowardice"),
    PINK("Pink", Color(0xFFEC4899), Color(0xFFBE185D), Color(0xFFFDF2F8), "romance, compassion, faithfulness, beauty, love, friendship, sensitivity"),
    RED("Red", Color(0xFFEF4444), Color(0xFFB91C1C), Color(0xFFFEF2F2), "danger, passion, daring, romance, style, excitement, urgency, energetic"),
    BLACK("Black", Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFFF1F5F9), "sophistication, power, mystery, formality, evil, death"),
    GRAY("Gray", Color(0xFF6B7280), Color(0xFF374151), Color(0xFFF9FAFB), "stability, security, strength of character, authority, maturity")
}

data class ModelRentStats(
    val modelName: String,
    val rentalCount: Int,
    val totalDays: Int,
    val totalRevenue: Double
)

data class PeriodReportStats(
    val totalRevenue: Double,
    val rentalCount: Int,
    val averageDays: Double,
    val activeRentals: Int,
    val completedRentals: Int,
    val cashDepositsHeld: Double,
    val modelStats: List<ModelRentStats>,
    val contracts: List<RentalContract>
)

data class DashboardStats(
    val totalRevenue: Double,
    val weeklyRevenue: Double,
    val monthlyRevenue: Double,
    val activeRentals: Int,
    val availableScooters: Int,
    val rentedScooters: Int,
    val dailyEarnings: Map<String, Double>,
    val weeklyEarnings: Map<String, Double>,
    val modelRentalCounts: Map<String, Int>,
    val modelRentStats: List<ModelRentStats>
)

object RentalRepository {
    var activeTheme by mutableStateOf(AppThemeColor.BLUE)

    var standardDepositAmount by mutableStateOf(3000.0)
    var maxiDepositAmount by mutableStateOf(10000.0)
    var isHighSeasonMode by mutableStateOf(false)
    var highSeasonPercentage by mutableStateOf(10.0)
    
    var globalReplacementValue by mutableStateOf(60000.0)
    var globalExtraCostPerHour by mutableStateOf(50.0)
    var globalFuelCostPerBar by mutableStateOf(100.0)
    var globalDeliveryCost by mutableStateOf(0.0)

    // Store signature paths keyed by contract ID
    val signaturePaths: MutableMap<String, Path> = mutableMapOf()

    val modelDrafts: SnapshotStateList<ScooterModelDraft> = mutableStateListOf(
        ScooterModelDraft("D1", "Honda", "Click 125i", 125, 2023, false, false, false, 400.0, 1100.0, 2400.0, 4200.0, 7500.0, 3000.0, "Popular lightweight scooter, extremely fuel-efficient and easy to ride."),
        ScooterModelDraft("D2", "Honda", "Click 160cc", 160, 2024, true, true, false, 500.0, 1400.0, 3000.0, 5200.0, 9000.0, 3000.0, "Modern sporty commuter, 160cc fuel-injected engine with great agility."),
        ScooterModelDraft("D3", "Honda", "PCX 150cc", 150, 2022, true, true, false, 500.0, 1400.0, 3000.0, 5200.0, 9000.0, 3000.0, "Comfortable premium commuter, spacious storage and smooth engine."),
        ScooterModelDraft("D4", "Honda", "PCX 160cc", 160, 2024, true, true, true, 600.0, 1700.0, 3600.0, 6200.0, 11000.0, 3000.0, "Luxurious commuter scooter with smart key system and large under-seat space."),
        ScooterModelDraft("D5", "Honda", "ADV 160cc", 160, 2023, true, true, true, 700.0, 2000.0, 4200.0, 7200.0, 13000.0, 3000.0, "Adventure-styled premium scooter offering excellent comfort and travel suspension."),
        ScooterModelDraft("D6", "Honda", "ADV 350cc", 350, 2024, true, true, true, 1200.0, 3400.0, 7200.0, 12500.0, 22000.0, 10000.0, "High-performance adventure scooter with long travel suspension and smart tech."),
        ScooterModelDraft("D7", "Honda", "Forza 350cc", 350, 2024, true, true, true, 1200.0, 3400.0, 7200.0, 12500.0, 22000.0, 10000.0, "High-performance touring scooter with electric windshield and spacious comfort.")
    )

    val scooters: SnapshotStateList<Scooter> = mutableStateListOf(
        Scooter("1", "Click 125i", "Honda", "Phuket 88-888", 400.0, ScooterStatus.AVAILABLE, "Popular lightweight scooter, extremely fuel-efficient and easy to ride.", 125, 2023, hasPhoneCharger = false, hasKeyless = false, hasAbs = false, priceDaily = 400.0, price3Day = 1100.0, priceWeekly = 2400.0, price2Week = 4200.0, priceMonthly = 7500.0, depositAmount = 3000.0),
        Scooter("2", "Click 160cc", "Honda", "Phuket 12-345", 500.0, ScooterStatus.AVAILABLE, "Modern sporty commuter, 160cc fuel-injected engine with great agility.", 160, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = false, priceDaily = 500.0, price3Day = 1400.0, priceWeekly = 3000.0, price2Week = 5200.0, priceMonthly = 9000.0, depositAmount = 3000.0),
        Scooter("3", "PCX 150cc", "Honda", "Phuket 66-777", 500.0, ScooterStatus.RENTED, "Comfortable premium commuter, spacious storage and smooth engine.", 150, 2022, hasPhoneCharger = true, hasKeyless = true, hasAbs = false, priceDaily = 500.0, price3Day = 1400.0, priceWeekly = 3000.0, price2Week = 5200.0, priceMonthly = 9000.0, depositAmount = 3000.0),
        Scooter("4", "PCX 160cc", "Honda", "Phuket 44-555", 600.0, ScooterStatus.AVAILABLE, "Luxurious commuter scooter with smart key system and large under-seat space.", 160, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 600.0, price3Day = 1700.0, priceWeekly = 3600.0, price2Week = 6200.0, priceMonthly = 11000.0, depositAmount = 3000.0),
        Scooter("5", "ADV 160cc", "Honda", "Phuket 55-555", 700.0, ScooterStatus.AVAILABLE, "Adventure-styled premium scooter offering excellent comfort and travel suspension.", 160, 2023, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 700.0, price3Day = 2000.0, priceWeekly = 4200.0, price2Week = 7200.0, priceMonthly = 13000.0, depositAmount = 3000.0),
        Scooter("6", "ADV 350cc", "Honda", "Phuket 99-999", 1200.0, ScooterStatus.AVAILABLE, "High-performance adventure scooter with long travel suspension and smart tech.", 350, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 1200.0, price3Day = 3400.0, priceWeekly = 7200.0, price2Week = 12500.0, priceMonthly = 22000.0, depositAmount = 10000.0),
        Scooter("7", "Forza 350cc", "Honda", "Phuket 77-777", 1200.0, ScooterStatus.AVAILABLE, "High-performance touring scooter with electric windshield and spacious comfort.", 350, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 1200.0, price3Day = 3400.0, priceWeekly = 7200.0, price2Week = 12500.0, priceMonthly = 22000.0, depositAmount = 10000.0)
    )

    val contracts: SnapshotStateList<RentalContract> = mutableStateListOf(
        // Pre-completed and active contracts for demo statistics
        RentalContract("C001", "Somchai Somboon", "+66812345678", "DL-987654", "3", 5, 1750.0, 3000.0, "Needs size L helmet", "2026-06-08", false),
        RentalContract("C002", "Alex Mercer", "+19876543210", "DL-123456", "2", 3, 540.0, 3000.0, "Returned in perfect condition", "2026-06-05", true),
        RentalContract("C003", "Yuki Tanaka", "+8190123456", "DL-555555", "1", 2, 400.0, 3000.0, "Prepaid online", "2026-06-07", true)
    )

    fun getDueDate(contract: RentalContract): Date {
        val parser = if (contract.startDate.contains(" ")) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
        val start = try {
            parser.parse(contract.startDate)
        } catch (e: Exception) {
            null
        } ?: Date()
        
        val cal = Calendar.getInstance()
        cal.time = start
        if (!contract.startDate.contains(" ")) {
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        cal.add(Calendar.DAY_OF_YEAR, contract.days)
        return cal.time
    }

    fun scheduleDueNotification(context: Context, contract: RentalContract) {
        val scooter = scooters.find { it.id == contract.scooterId }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, RentalNotificationReceiver::class.java).apply {
            putExtra("contractId", contract.id)
            putExtra("customerName", contract.customerName)
            putExtra("scooterModel", scooter?.let { "${it.brand} ${it.model}" } ?: "Scooter")
            putExtra("plateNumber", scooter?.plateNumber ?: "")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            contract.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dueDate = getDueDate(contract)
        val triggerTime = dueDate.time - 3600 * 1000 // 1 hour before due
        val now = System.currentTimeMillis()
        if (triggerTime > now) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    fun cancelDueNotification(context: Context, contract: RentalContract) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, RentalNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            contract.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun addContract(contract: RentalContract, context: Context? = null) {
        contracts.add(contract)
        val index = scooters.indexOfFirst { it.id == contract.scooterId }
        var activeScooter: Scooter? = null
        if (index != -1) {
            val updated = scooters[index].copy(status = ScooterStatus.RENTED)
            scooters[index] = updated
            activeScooter = updated
        }
        context?.let { scheduleDueNotification(it, contract) }

        // Sync to Supabase
        if (SupabaseService.isConfigured() && activeScooter != null) {
            kotlin.concurrent.thread {
                val success = SupabaseService.saveContract(contract, activeScooter)
                Log.d("RentalRepository", "Save contract to Supabase success: $success")
            }
        }
    }

    fun returnScooter(contractId: String, context: Context? = null) {
        val contractIndex = contracts.indexOfFirst { it.id == contractId }
        if (contractIndex != -1) {
            val contract = contracts[contractIndex]
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentDateStr = dateFormat.format(Date())
            contracts[contractIndex] = contract.copy(isCompleted = true, returnDate = currentDateStr)
            
            val scooterIndex = scooters.indexOfFirst { it.id == contract.scooterId }
            var plateToReturn = ""
            if (scooterIndex != -1) {
                val updated = scooters[scooterIndex].copy(status = ScooterStatus.AVAILABLE)
                scooters[scooterIndex] = updated
                plateToReturn = updated.plateNumber
            }
            context?.let { cancelDueNotification(it, contract) }

            // Sync to Supabase
            if (SupabaseService.isConfigured() && plateToReturn.isNotEmpty()) {
                kotlin.concurrent.thread {
                    val success = SupabaseService.returnScooter(plateToReturn)
                    Log.d("RentalRepository", "Return scooter in Supabase success: $success")
                }
            }
        }
    }

    fun addScooter(scooter: Scooter) {
        scooters.add(scooter)
        if (SupabaseService.isConfigured()) {
            kotlin.concurrent.thread {
                val dbId = SupabaseService.saveScooterAndGetId(scooter)
                if (dbId != null) {
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post {
                        val index = scooters.indexOfFirst { it.plateNumber == scooter.plateNumber }
                        if (index != -1) {
                            scooters[index] = scooters[index].copy(id = dbId)
                        }
                    }
                }
            }
        }
    }

    fun updateScooter(updatedScooter: Scooter) {
        val index = scooters.indexOfFirst { it.id == updatedScooter.id }
        if (index != -1) {
            scooters[index] = updatedScooter
            if (SupabaseService.isConfigured()) {
                kotlin.concurrent.thread {
                    val success = SupabaseService.updateScooter(updatedScooter.id, updatedScooter)
                    Log.d("RentalRepository", "Update scooter in Supabase success: $success")
                }
            }
        }
    }

    fun syncWithSupabase(context: Context, onComplete: (Boolean) -> Unit = {}) {
        if (!SupabaseService.isConfigured()) {
            onComplete(false)
            return
        }

        kotlin.concurrent.thread {
            try {
                var supScooters = SupabaseService.fetchMotors()
                var supModels = SupabaseService.fetchModels()
                
                // If Supabase database fleet is empty, auto-seed with original default scooter data
                if (supScooters != null && supScooters.isEmpty()) {
                    Log.d("RentalRepository", "Supabase fleet is empty. Seeding default fleet data...")
                    
                    val defaultScooters = listOf(
                        Scooter("1", "Click 125i", "Honda", "Phuket 88-888", 400.0, ScooterStatus.AVAILABLE, "Popular lightweight scooter, extremely fuel-efficient and easy to ride.", 125, 2023, hasPhoneCharger = false, hasKeyless = false, hasAbs = false, priceDaily = 400.0, price3Day = 1100.0, priceWeekly = 2400.0, price2Week = 4200.0, priceMonthly = 7500.0),
                        Scooter("2", "Click 160cc", "Honda", "Phuket 12-345", 500.0, ScooterStatus.AVAILABLE, "Modern sporty commuter, 160cc fuel-injected engine with great agility.", 160, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = false, priceDaily = 500.0, price3Day = 1400.0, priceWeekly = 3000.0, price2Week = 5200.0, priceMonthly = 9000.0),
                        Scooter("3", "PCX 150cc", "Honda", "Phuket 66-777", 500.0, ScooterStatus.RENTED, "Comfortable premium commuter, spacious storage and smooth engine.", 150, 2022, hasPhoneCharger = true, hasKeyless = true, hasAbs = false, priceDaily = 500.0, price3Day = 1400.0, priceWeekly = 3000.0, price2Week = 5200.0, priceMonthly = 9000.0),
                        Scooter("4", "PCX 160cc", "Honda", "Phuket 44-555", 600.0, ScooterStatus.AVAILABLE, "Luxurious commuter scooter with smart key system and large under-seat space.", 160, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 600.0, price3Day = 1700.0, priceWeekly = 3600.0, price2Week = 6200.0, priceMonthly = 11000.0),
                        Scooter("5", "ADV 160cc", "Honda", "Phuket 55-555", 700.0, ScooterStatus.AVAILABLE, "Adventure-styled premium scooter offering excellent comfort and travel suspension.", 160, 2023, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 700.0, price3Day = 2000.0, priceWeekly = 4200.0, price2Week = 7200.0, priceMonthly = 13000.0),
                        Scooter("6", "ADV 350cc", "Honda", "Phuket 99-999", 1200.0, ScooterStatus.AVAILABLE, "High-performance adventure scooter with long travel suspension and smart tech.", 350, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 1200.0, price3Day = 3400.0, priceWeekly = 7200.0, price2Week = 12500.0, priceMonthly = 22000.0),
                        Scooter("7", "Forza 350cc", "Honda", "Phuket 77-777", 1200.0, ScooterStatus.AVAILABLE, "High-performance touring scooter with electric windshield and spacious comfort.", 350, 2024, hasPhoneCharger = true, hasKeyless = true, hasAbs = true, priceDaily = 1200.0, price3Day = 3400.0, priceWeekly = 7200.0, price2Week = 12500.0, priceMonthly = 22000.0)
                    )

                    for (scooter in defaultScooters) {
                        SupabaseService.saveScooter(scooter)
                    }
                    
                    supScooters = SupabaseService.fetchMotors()
                }

                // If Supabase database models list is empty, auto-seed with original default model drafts
                if (supModels != null && supModels.isEmpty()) {
                    Log.d("RentalRepository", "Supabase models list is empty. Seeding default model draft data...")
                    
                    val defaultModels = listOf(
                        ScooterModelDraft("D1", "Honda", "Click 125i", 125, 2023, false, false, false, 400.0, 1100.0, 2400.0, 4200.0, 7500.0, 3000.0, "Popular lightweight scooter, extremely fuel-efficient and easy to ride."),
                        ScooterModelDraft("D2", "Honda", "Click 160cc", 160, 2024, true, true, false, 500.0, 1400.0, 3000.0, 5200.0, 9000.0, 3000.0, "Modern sporty commuter, 160cc fuel-injected engine with great agility."),
                        ScooterModelDraft("D3", "Honda", "PCX 150cc", 150, 2022, true, true, false, 500.0, 1400.0, 3000.0, 5200.0, 9000.0, 3000.0, "Comfortable premium commuter, spacious storage and smooth engine."),
                        ScooterModelDraft("D4", "Honda", "PCX 160cc", 160, 2024, true, true, true, 600.0, 1700.0, 3600.0, 6200.0, 11000.0, 3000.0, "Luxurious commuter scooter with smart key system and large under-seat space."),
                        ScooterModelDraft("D5", "Honda", "ADV 160cc", 160, 2023, true, true, true, 700.0, 2000.0, 4200.0, 7200.0, 13000.0, 3000.0, "Adventure-styled premium scooter offering excellent comfort and travel suspension."),
                        ScooterModelDraft("D6", "Honda", "ADV 350cc", 350, 2024, true, true, true, 1200.0, 3400.0, 7200.0, 12500.0, 22000.0, 10000.0, "High-performance adventure scooter with long travel suspension and smart tech."),
                        ScooterModelDraft("D7", "Honda", "Forza 350cc", 350, 2024, true, true, true, 1200.0, 3400.0, 7200.0, 12500.0, 22000.0, 10000.0, "High-performance touring scooter with electric windshield and spacious comfort.")
                    )

                    for (model in defaultModels) {
                        SupabaseService.saveModel(model)
                    }
                    
                    supModels = SupabaseService.fetchModels()
                }

                val supContracts = SupabaseService.fetchContracts()

                if (supScooters != null) {
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post {
                        scooters.clear()
                        scooters.addAll(supScooters)
                    }
                }

                if (supModels != null) {
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post {
                        modelDrafts.clear()
                        modelDrafts.addAll(supModels)
                    }
                }

                if (supContracts != null) {
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post {
                        contracts.clear()
                        contracts.addAll(supContracts)
                    }
                }

                onComplete(supScooters != null || supContracts != null || supModels != null)
            } catch (e: Exception) {
                Log.e("RentalRepository", "Sync failed", e)
                onComplete(false)
            }
        }
    }

    fun getDashboardStats(): DashboardStats {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Date()
        val calendar = Calendar.getInstance()
        
        // 7 days ago
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = calendar.time
        
        // 30 days ago
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val thirtyDaysAgo = calendar.time
        calendar.time = now
        
        var totalRevenue = 0.0
        var weeklyRevenue = 0.0
        var monthlyRevenue = 0.0
        
        // Daily earnings map initialization (last 7 days, sorted chronologically)
        val dailyEarnings = mutableMapOf<String, Double>()
        val dayFormatter = SimpleDateFormat("EEE", Locale.US)
        for (i in 6 downTo 0) {
            calendar.time = now
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayName = dayFormatter.format(calendar.time)
            dailyEarnings[dayName] = 0.0
        }
        
        // Weekly earnings map initialization (last 4 weeks)
        val weeklyEarnings = mutableMapOf(
            "Week 1" to 0.0,
            "Week 2" to 0.0,
            "Week 3" to 0.0,
            "Week 4" to 0.0
        )
        
        val modelRentalCounts = mutableMapOf<String, Int>()
        val modelRentStatsMap = mutableMapOf<String, Triple<Int, Int, Double>>() // modelName -> (count, days, revenue)
        
        contracts.forEach { contract ->
            val date = try {
                if (contract.startDate.contains(" ")) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(contract.startDate)
                } else {
                    dateFormat.parse(contract.startDate)
                }
            } catch (e: Exception) {
                now
            } ?: now
            
            totalRevenue += contract.totalPrice
            
            val diffTime = now.time - date.time
            val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
            
            if (diffDays in 0..6) {
                val dayName = dayFormatter.format(date)
                dailyEarnings[dayName] = (dailyEarnings[dayName] ?: 0.0) + contract.totalPrice
                weeklyRevenue += contract.totalPrice
            }
            
            if (diffDays in 0..29) {
                monthlyRevenue += contract.totalPrice
                
                when (diffDays) {
                    in 0..6 -> weeklyEarnings["Week 4"] = (weeklyEarnings["Week 4"] ?: 0.0) + contract.totalPrice
                    in 7..13 -> weeklyEarnings["Week 3"] = (weeklyEarnings["Week 3"] ?: 0.0) + contract.totalPrice
                    in 14..20 -> weeklyEarnings["Week 2"] = (weeklyEarnings["Week 2"] ?: 0.0) + contract.totalPrice
                    in 21..29 -> weeklyEarnings["Week 1"] = (weeklyEarnings["Week 1"] ?: 0.0) + contract.totalPrice
                }
            }
            
            val scooter = scooters.find { it.id == contract.scooterId }
            if (scooter != null) {
                val modelName = "${scooter.brand} ${scooter.model}"
                modelRentalCounts[modelName] = (modelRentalCounts[modelName] ?: 0) + 1
                
                val current = modelRentStatsMap[modelName] ?: Triple(0, 0, 0.0)
                modelRentStatsMap[modelName] = Triple(
                    current.first + 1,
                    current.second + contract.days,
                    current.third + contract.totalPrice
                )
            }
        }
        
        // Add default values for empty charts so that the dashboard displays nicely
        if (totalRevenue <= 0.0) {
            dailyEarnings["Mon"] = 1450.0
            dailyEarnings["Tue"] = 1600.0
            dailyEarnings["Wed"] = 1300.0
            dailyEarnings["Thu"] = 1750.0
            dailyEarnings["Fri"] = 2100.0
            dailyEarnings["Sat"] = 2800.0
            dailyEarnings["Sun"] = 2500.0
            
            weeklyEarnings["Week 1"] = 3500.0
            weeklyEarnings["Week 2"] = 4200.0
            weeklyEarnings["Week 3"] = 4800.0
            weeklyEarnings["Week 4"] = 5500.0
            
            modelRentalCounts["Honda PCX 160cc"] = 5
            modelRentalCounts["Honda Click 125i"] = 3
            modelRentalCounts["Honda ADV 160cc"] = 2
            modelRentalCounts["Honda Forza 350cc"] = 1
        }
        
        val modelRentStats = if (totalRevenue <= 0.0) {
            listOf(
                ModelRentStats("Honda PCX 160cc", 5, 24, 11000.0),
                ModelRentStats("Honda Click 125i", 3, 15, 4500.0),
                ModelRentStats("Honda ADV 160cc", 2, 10, 4200.0),
                ModelRentStats("Honda Forza 350cc", 1, 7, 7200.0)
            )
        } else {
            modelRentStatsMap.map { (modelName, triple) ->
                ModelRentStats(modelName, triple.first, triple.second, triple.third)
            }.sortedByDescending { it.rentalCount }
        }
        
        val activeRentals = contracts.count { !it.isCompleted }
        val totalScooters = scooters.size
        val rentedScooters = scooters.count { it.status == ScooterStatus.RENTED }
        val availableScooters = totalScooters - rentedScooters
        
        return DashboardStats(
            totalRevenue = totalRevenue,
            weeklyRevenue = weeklyRevenue,
            monthlyRevenue = monthlyRevenue,
            activeRentals = activeRentals,
            availableScooters = availableScooters,
            rentedScooters = rentedScooters,
            dailyEarnings = dailyEarnings,
            weeklyEarnings = weeklyEarnings,
            modelRentalCounts = modelRentalCounts,
            modelRentStats = modelRentStats
        )
    }

    fun getPeriodReportStats(daysLimit: Int): PeriodReportStats {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Date()
        
        // Filter contracts that started within the last `daysLimit` days
        val filteredContracts = contracts.filter { contract ->
            val date = try {
                if (contract.startDate.contains(" ")) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(contract.startDate)
                } else {
                    dateFormat.parse(contract.startDate)
                }
            } catch (e: Exception) {
                now
            } ?: now
            val diffTime = now.time - date.time
            val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
            diffDays in 0 until daysLimit
        }
        
        var totalRevenue = 0.0
        var totalDays = 0
        var activeRentalsCount = 0
        var completedRentalsCount = 0
        var cashDepositsHeld = 0.0
        val modelStatsMap = mutableMapOf<String, Triple<Int, Int, Double>>() // modelName -> (count, days, revenue)
        
        filteredContracts.forEach { contract ->
            totalRevenue += contract.totalPrice
            totalDays += contract.days
            if (contract.isCompleted) {
                completedRentalsCount++
            } else {
                activeRentalsCount++
                cashDepositsHeld += contract.depositAmount
            }
            
            val scooter = scooters.find { it.id == contract.scooterId }
            if (scooter != null) {
                val modelName = "${scooter.brand} ${scooter.model}"
                val current = modelStatsMap[modelName] ?: Triple(0, 0, 0.0)
                modelStatsMap[modelName] = Triple(
                    current.first + 1,
                    current.second + contract.days,
                    current.third + contract.totalPrice
                )
            }
        }
        
        val averageDays = if (filteredContracts.isNotEmpty()) totalDays.toDouble() / filteredContracts.size else 0.0
        
        val modelStats = modelStatsMap.map { (modelName, triple) ->
            ModelRentStats(modelName, triple.first, triple.second, triple.third)
        }.sortedByDescending { it.rentalCount }
        
        // Mock data fallback if no contracts exist in the period
        if (contracts.isEmpty() || totalRevenue <= 0.0) {
            val mockContracts = if (daysLimit == 7) {
                listOf(
                    RentalContract("MC001", "Somchai Somboon", "+66812345678", "DL-987654", "3", 5, 2500.0, 3000.0, "Needs L helmet", "2026-06-08", false),
                    RentalContract("MC002", "Alex Mercer", "+19876543210", "DL-123456", "2", 3, 1500.0, 3000.0, "", "2026-06-09", true)
                )
            } else {
                listOf(
                    RentalContract("MC001", "Somchai Somboon", "+66812345678", "DL-987654", "3", 5, 2500.0, 3000.0, "Needs L helmet", "2026-06-08", false),
                    RentalContract("MC002", "Alex Mercer", "+19876543210", "DL-123456", "2", 3, 1500.0, 3000.0, "", "2026-06-09", true),
                    RentalContract("MC003", "Yuki Tanaka", "+8190123456", "DL-555555", "1", 14, 4800.0, 3000.0, "", "2026-05-28", true),
                    RentalContract("MC004", "John Doe", "+1234567890", "DL-777777", "4", 7, 4200.0, 3000.0, "", "2026-05-20", true)
                )
            }
            
            var mRev = 0.0
            var mDays = 0
            var mActive = 0
            var mCompleted = 0
            var mDep = 0.0
            val mStatsMap = mutableMapOf<String, Triple<Int, Int, Double>>()
            
            mockContracts.forEach { contract ->
                mRev += contract.totalPrice
                mDays += contract.days
                if (contract.isCompleted) {
                    mCompleted++
                } else {
                    mActive++
                    mDep += contract.depositAmount
                }
                val scooter = scooters.find { it.id == contract.scooterId }
                if (scooter != null) {
                    val modelName = "${scooter.brand} ${scooter.model}"
                    val current = mStatsMap[modelName] ?: Triple(0, 0, 0.0)
                    mStatsMap[modelName] = Triple(
                        current.first + 1,
                        current.second + contract.days,
                        current.third + contract.totalPrice
                    )
                }
            }
            
            return PeriodReportStats(
                totalRevenue = mRev,
                rentalCount = mockContracts.size,
                averageDays = mDays.toDouble() / mockContracts.size,
                activeRentals = mActive,
                completedRentals = mCompleted,
                cashDepositsHeld = mDep,
                modelStats = mStatsMap.map { (modelName, triple) ->
                    ModelRentStats(modelName, triple.first, triple.second, triple.third)
                }.sortedByDescending { it.rentalCount },
                contracts = mockContracts
            )
        }
        
        return PeriodReportStats(
            totalRevenue = totalRevenue,
            rentalCount = filteredContracts.size,
            averageDays = averageDays,
            activeRentals = activeRentalsCount,
            completedRentals = completedRentalsCount,
            cashDepositsHeld = cashDepositsHeld,
            modelStats = modelStats,
            contracts = filteredContracts
        )
    }

    fun deleteScooter(scooterId: String, onComplete: (Boolean) -> Unit = {}) {
        val scooter = scooters.find { it.id == scooterId }
        val plate = scooter?.plateNumber ?: ""
        if (SupabaseService.isConfigured()) {
            kotlin.concurrent.thread {
                val success = SupabaseService.deleteMotor(scooterId, plate)
                val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                mainHandler.post {
                    if (success) {
                        scooters.removeAll { it.id == scooterId }
                        contracts.removeAll { it.scooterId == scooterId }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
            }
        } else {
            scooters.removeAll { it.id == scooterId }
            contracts.removeAll { it.scooterId == scooterId }
            onComplete(true)
        }
    }

    fun deleteModelDraft(draftId: String, onComplete: (Boolean) -> Unit = {}) {
        if (SupabaseService.isConfigured()) {
            kotlin.concurrent.thread {
                val success = SupabaseService.deleteModel(draftId)
                val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                mainHandler.post {
                    if (success) {
                        modelDrafts.removeAll { it.id == draftId }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
            }
        } else {
            modelDrafts.removeAll { it.id == draftId }
            onComplete(true)
        }
    }

    fun addModelDraft(draft: ScooterModelDraft) {
        modelDrafts.add(draft)
        if (SupabaseService.isConfigured()) {
            kotlin.concurrent.thread {
                val dbId = SupabaseService.saveModelAndGetId(draft)
                if (dbId != null) {
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post {
                        val index = modelDrafts.indexOfFirst { it.brand == draft.brand && it.model == draft.model && it.id == draft.id }
                        if (index != -1) {
                            modelDrafts[index] = modelDrafts[index].copy(id = dbId)
                        }
                    }
                }
            }
        }
    }

    fun updateModelDraft(updatedDraft: ScooterModelDraft) {
        val index = modelDrafts.indexOfFirst { it.id == updatedDraft.id }
        if (index != -1) {
            modelDrafts[index] = updatedDraft
            if (SupabaseService.isConfigured()) {
                kotlin.concurrent.thread {
                    val success = SupabaseService.updateModel(updatedDraft.id, updatedDraft)
                    Log.d("RentalRepository", "Update model draft in Supabase success: $success")
                }
            }
        }
    }
}

