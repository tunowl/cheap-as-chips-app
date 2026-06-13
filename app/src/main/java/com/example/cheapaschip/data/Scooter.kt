package com.example.cheapaschip.data

enum class ScooterStatus {
    AVAILABLE,
    RENTED
}

data class Scooter(
    val id: String,
    val model: String,
    val brand: String,
    val plateNumber: String,
    val pricePerDay: Double,
    val status: ScooterStatus,
    val description: String,
    val cc: Int,
    val year: Int,
    val hasPhoneCharger: Boolean = false,
    val hasKeyless: Boolean = false,
    val hasAbs: Boolean = false,
    val priceDaily: Double = pricePerDay,
    val price3Day: Double = pricePerDay * 3.0 * 0.9,
    val priceWeekly: Double = pricePerDay * 7.0 * 0.85,
    val price2Week: Double = pricePerDay * 14.0 * 0.75,
    val priceMonthly: Double = pricePerDay * 30.0 * 0.65,
    val depositAmount: Double = 0.0,
    val images: List<Any> = emptyList(),
    val fuelBars: Int = 6,
    val fuelCostPerBar: Double = 100.0
)

data class ScooterModelDraft(
    val id: String,
    val brand: String,
    val model: String,
    val cc: Int,
    val year: Int,
    val hasPhoneCharger: Boolean,
    val hasKeyless: Boolean,
    val hasAbs: Boolean,
    val priceDaily: Double,
    val price3Day: Double,
    val priceWeekly: Double,
    val price2Week: Double,
    val priceMonthly: Double,
    val depositAmount: Double = 0.0,
    val description: String = "",
    val fuelBars: Int = 6,
    val fuelCostPerBar: Double = 100.0
)
