package com.example.cheapaschip.data

data class RentalContract(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val licenseNumber: String,
    val scooterId: String,
    val days: Int,
    val totalPrice: Double,
    val depositAmount: Double,
    val notes: String = "",
    val startDate: String, // Format: YYYY-MM-DD
    val isCompleted: Boolean = false,
    val customerEmail: String = "",
    val customerWhatsApp: String = "",
    val returnDate: String? = null,
    
    // Fields from document.pdf
    val nationality: String = "",
    val passportNumber: String = "",
    val hotelAddress: String = "",
    val roomNumber: String = "",
    val replacementValue: Double = 60000.0,
    val extraCostPerHour: Double = 50.0,
    val fuelCostPerBar: Double = 100.0,
    val passportHold: Boolean = false,
    val helmetCount: Int = 1,
    val fuelLevel: Int = 100,
    val deliveryCost: Double = 0.0,
    val passportImage: Any? = null,
    val licenseImage: Any? = null,
    val scratchImages: List<Any> = emptyList()
)
