package com.example.cheapaschip.ui

import kotlin.reflect.KProperty
import android.widget.Toast
import android.net.Uri
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.os.Environment
import android.graphics.RectF
import androidx.compose.ui.graphics.asAndroidPath
import java.io.File
import java.io.FileOutputStream
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cheapaschip.data.RentalContract
import com.example.cheapaschip.data.RentalRepository
import com.example.cheapaschip.data.Scooter
import com.example.cheapaschip.data.ScooterStatus
import com.example.cheapaschip.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer

private val RENTAL_TERMS = listOf(
    "1. You agree to not drive under the influence of alcohol or drugs. If it is found you were under the influence while involved in an accident, you forfeit your right to any insurance coverage, legal or financial protection & assistance.",
    "2. You assume full responsibility for your legal right to operate a vehicle in Thailand. You assume all financial and legal liability for any fines or charges resulting from unlicensed operation.",
    "3. In the event of an accident with another party, you agree to contact local police immediately to establish who is at fault. You agree to contact the rental company immediately after the accident. All licensed and registered scooters in Thailand include mandatory bodily injury coverage for the rider of 30,000 THB. However both 3rd Party coverage and coverage for rental scooter damage ARE NOT provided. You are financially responsible for repair & replacement costs in the event of any accident where you are at fault or the other party is unable to pay. You are financially responsible for repair time costs while the scooter is unable to be rented.",
    "4. In the event a vehicle must be repaired, you must not repair it yourself or with an unauthorized repair facility.",
    "5. You are financially responsible in the event the vehicle is lost or stolen at the Replacement Value indicated above. You are required to lock the vehicle while not in use using the steering lock if equipped.",
    "6. You agree to wear the helmet(s) provided and obey all local traffic laws and regulations.",
    "7. You acknowledge you are at least 18 years old and legally allowed to operate the vehicle.",
    "8. You acknowledge a maximum of 2 people are to ride the vehicle at a time.",
    "9. You must stay within the Province of Rental unless expressly authorized by the rental company.",
    "10. Off-Road Driving driving will result in a fine up to 5000 THB, lost helmet up to 800 THB, lost electronic key up to 4000 THB, lost mechanical key up to 1000 THB.",
    "11. You must use 95 Gasohol from retail gas stations only, roadside gasoline from bottles is prohibited. The scooter must be returned with the same gas level as Booking Start Date or you'll be charged at the Fuel Cost per Bar rate above.",
    "12. Flat tires are the responsibility of you, the rider, not the shop. You may contact the shop for assistance but you will be charged the agreed upon repair cost.",
    "13. The first charge of a dead battery is your responsibility. If it is determined the battery will not hold a charge, it is considered mechanical failure and the shop will assist.",
    "14. Any rental extensions must be requested in advance and are subject to availability and shop discretion.",
    "15. Rental days are defined as 24hr periods. The scooter must be returned at the same time indicated above unless another End Time is agreed to. Additional cost for extra hours will be charged at the Extra Cost per Hour rate above.",
    "16. No Refunds under any circumstances after the Booking Start Date and Time has begun.",
    "17. Do not ride with more than 2 people on board.",
    "18. Lock the steering and take key with you when the bike is parked or unattended.",
    "19. Do not attempt wheel stands, skids, wheelies, donuts, or any careless acts to cause damage to the bike."
)

@Composable
fun StepperHeader(currentStep: Int) {
    val steps = listOf("Bike", "Docs", "Details & Terms", "Signature", "Finalize")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val stepNum = index + 1
            val isActive = stepNum == currentStep
            val isCompleted = stepNum < currentStep

            // Step circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isActive || isCompleted) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$stepNum",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Step label
            Text(
                text = title,
                fontSize = 11.sp,
                color = if (isActive || isCompleted) MaterialTheme.colorScheme.onBackground else Color.Gray,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )

            // Line separator
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (isCompleted) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0))
                )
            }
        }
    }
}

private fun getInitialRentalTime(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MINUTE, 30)
    val minute = cal.get(Calendar.MINUTE)
    val rounded = if (minute < 15) 0 else if (minute < 45) 30 else 60
    if (rounded == 60) {
        cal.add(Calendar.HOUR_OF_DAY, 1)
        cal.set(Calendar.MINUTE, 0)
    } else {
        cal.set(Calendar.MINUTE, rounded)
    }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val finalMinute = cal.get(Calendar.MINUTE)
    if (hour > 20 || (hour == 20 && finalMinute > 0)) {
        return "20:00"
    }
    return String.format(Locale.getDefault(), "%02d:%02d", hour, finalMinute)
}

class RefDelegate<T>(val getVal: () -> T, val setVal: (T) -> Unit) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = getVal()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setVal(value)
}

class ContactScreenState {
    var currentStep by mutableStateOf(1)
    var termsRead by mutableStateOf(false)
    var showTermsDialog by mutableStateOf(false)
    var showPasswordDialog by mutableStateOf(false)

    // Step 1
    var selectedScooterId by mutableStateOf<String?>(null)
    var dropdownExpanded by mutableStateOf(false)
    var plateDropdownExpanded by mutableStateOf(false)
    var scooterPlate by mutableStateOf("")
    var customPricePerDayStr by mutableStateOf("0")

    // Step 2
    var customerName by mutableStateOf("")
    var passportNumber by mutableStateOf("")
    var nationality by mutableStateOf("")
    var phoneCountryCode by mutableStateOf("+66")
    var phoneCountryFlag by mutableStateOf("🇹🇭")
    var customerPhone by mutableStateOf("")
    var customerWhatsApp by mutableStateOf("")
    var whatsAppSameAsPhone by mutableStateOf(false)
    var customerEmail by mutableStateOf("")
    var countryDropdownExpanded by mutableStateOf(false)
    var countrySearchQuery by mutableStateOf("")

    // Image picker
    var passportImage by mutableStateOf<Any?>(null)
    var licenseImage by mutableStateOf<Any?>(null)
    var showPassportDialog by mutableStateOf(false)
    var showLicenseDialog by mutableStateOf(false)

    // Step 3
    var fuelLevel by mutableStateOf("Full (6/6)")
    var fuelIndex by mutableStateOf(6)
    var rentalTimeStr by mutableStateOf(getInitialRentalTime())
    var helmetCount by mutableStateOf(1)
    var bodyConditionNotes by mutableStateOf("No major scratches, tires checked.")
    var notes by mutableStateOf("")
    val scratchImages = mutableStateListOf<Any>()

    // Step 4
    var rentalDaysStr by mutableStateOf("1")
    var depositType by mutableStateOf("Cash")

    // Step 5
    var signaturePath by mutableStateOf(Path())
    var signatureTrigger by mutableStateOf(0)
    var hasSigned by mutableStateOf(false)

    // Final Contract
    var generatedContract by mutableStateOf<RentalContract?>(null)

    // Additional fields
    var customerHotelAddress by mutableStateOf("")
    var customerRoomNumber by mutableStateOf("")

    fun reset() {
        currentStep = 1
        termsRead = false
        showTermsDialog = false
        selectedScooterId = null
        dropdownExpanded = false
        plateDropdownExpanded = false
        scooterPlate = ""
        customPricePerDayStr = "0"
        customerName = ""
        passportNumber = ""
        nationality = ""
        phoneCountryCode = "+66"
        phoneCountryFlag = "🇹🇭"
        customerPhone = ""
        customerWhatsApp = ""
        whatsAppSameAsPhone = false
        customerEmail = ""
        countryDropdownExpanded = false
        countrySearchQuery = ""
        passportImage = null
        licenseImage = null
        showPassportDialog = false
        showLicenseDialog = false
        fuelLevel = "Full (6/6)"
        fuelIndex = 6
        rentalTimeStr = getInitialRentalTime()
        helmetCount = 1
        bodyConditionNotes = "No major scratches, tires checked."
        notes = ""
        scratchImages.clear()
        rentalDaysStr = "1"
        depositType = "Cash"
        signaturePath = Path()
        signatureTrigger = 0
        hasSigned = false
        generatedContract = null
        customerHotelAddress = ""
        customerRoomNumber = ""
        showPasswordDialog = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    modifier: Modifier = Modifier,
    preselectedScooterId: String? = null,
    onClearSelection: () -> Unit,
    onContractGenerated: () -> Unit,
    state: ContactScreenState = remember { ContactScreenState() }
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Stepper state
    var currentStep by RefDelegate({ state.currentStep }, { state.currentStep = it })
    var termsRead by RefDelegate({ state.termsRead }, { state.termsRead = it })
    var showTermsDialog by RefDelegate({ state.showTermsDialog }, { state.showTermsDialog = it })
    var showPasswordDialog by RefDelegate({ state.showPasswordDialog }, { state.showPasswordDialog = it })

    LaunchedEffect(currentStep) {
        scrollState.scrollTo(0)
    }

    // Step 1 states: Bike
    val availableScooters = RentalRepository.scooters.filter { it.status == ScooterStatus.AVAILABLE }
    var selectedScooterId by RefDelegate({ state.selectedScooterId }, { state.selectedScooterId = it })
    var dropdownExpanded by RefDelegate({ state.dropdownExpanded }, { state.dropdownExpanded = it })
    var plateDropdownExpanded by RefDelegate({ state.plateDropdownExpanded }, { state.plateDropdownExpanded = it })
    var scooterPlate by RefDelegate({ state.scooterPlate }, { state.scooterPlate = it })
    var customPricePerDayStr by RefDelegate({ state.customPricePerDayStr }, { state.customPricePerDayStr = it })

    // Step 2 states: Customer
    var customerName by RefDelegate({ state.customerName }, { state.customerName = it })
    var passportNumber by RefDelegate({ state.passportNumber }, { state.passportNumber = it })
    var nationality by RefDelegate({ state.nationality }, { state.nationality = it })
    var phoneCountryCode by RefDelegate({ state.phoneCountryCode }, { state.phoneCountryCode = it })
    var phoneCountryFlag by RefDelegate({ state.phoneCountryFlag }, { state.phoneCountryFlag = it })
    var customerPhone by RefDelegate({ state.customerPhone }, { state.customerPhone = it })
    var customerWhatsApp by RefDelegate({ state.customerWhatsApp }, { state.customerWhatsApp = it })
    var whatsAppSameAsPhone by RefDelegate({ state.whatsAppSameAsPhone }, { state.whatsAppSameAsPhone = it })
    var customerEmail by RefDelegate({ state.customerEmail }, { state.customerEmail = it })
    var countryDropdownExpanded by RefDelegate({ state.countryDropdownExpanded }, { state.countryDropdownExpanded = it })
    var countrySearchQuery by RefDelegate({ state.countrySearchQuery }, { state.countrySearchQuery = it })

    // Image picker states (can be Uri or Bitmap)
    var passportImage by RefDelegate({ state.passportImage }, { state.passportImage = it })
    var licenseImage by RefDelegate({ state.licenseImage }, { state.licenseImage = it })
    var showPassportDialog by RefDelegate({ state.showPassportDialog }, { state.showPassportDialog = it })
    var showLicenseDialog by RefDelegate({ state.showLicenseDialog }, { state.showLicenseDialog = it })
    var maximizedDocImage by remember { mutableStateOf<Any?>(null) }
    var pdfFileToShow by remember { mutableStateOf<File?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }

    // Photo launchers
    val passportGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            passportImage = uri
            isOcrRunning = true
            Toast.makeText(context, "Scanning Passport details...", Toast.LENGTH_SHORT).show()
            runPassportOcr(context, uri, { name, num, nat ->
                if (name.isNotEmpty()) customerName = name
                if (num.isNotEmpty()) passportNumber = num
                if (nat.isNotEmpty()) nationality = nat
                Toast.makeText(context, "Passport details loaded!", Toast.LENGTH_SHORT).show()
            }, {
                isOcrRunning = false
            })
        }
    }

    var tempPassportUriStr by remember { mutableStateOf<String?>(null) }
    var tempLicenseUriStr by remember { mutableStateOf<String?>(null) }
    var tempScratchUriStr by remember { mutableStateOf<String?>(null) }

    fun createTempImageUri(prefix: String): Uri {
        val tempFile = File.createTempFile(prefix, ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    val passportCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uriStr = tempPassportUriStr
        if (success && uriStr != null) {
            val uri = Uri.parse(uriStr)
            passportImage = uri
            isOcrRunning = true
            Toast.makeText(context, "Scanning Passport details...", Toast.LENGTH_SHORT).show()
            runPassportOcr(context, uri, { name, num, nat ->
                if (name.isNotEmpty()) customerName = name
                if (num.isNotEmpty()) passportNumber = num
                if (nat.isNotEmpty()) nationality = nat
                Toast.makeText(context, "Passport details loaded!", Toast.LENGTH_SHORT).show()
            }, {
                isOcrRunning = false
            })
        }
    }

    val licenseGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) licenseImage = uri
    }

    val licenseCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uriStr = tempLicenseUriStr
        if (success && uriStr != null) {
            licenseImage = Uri.parse(uriStr)
        }
    }

    // Step 3 states: Condition
    var fuelLevel by RefDelegate({ state.fuelLevel }, { state.fuelLevel = it })
    var fuelIndex by RefDelegate({ state.fuelIndex }, { state.fuelIndex = it })
    var rentalTimeStr by RefDelegate({ state.rentalTimeStr }, { state.rentalTimeStr = it })
    var helmetCount by RefDelegate({ state.helmetCount }, { state.helmetCount = it })
    var bodyConditionNotes by RefDelegate({ state.bodyConditionNotes }, { state.bodyConditionNotes = it })
    var notes by RefDelegate({ state.notes }, { state.notes = it })

    val scratchImages = state.scratchImages
    val scratchCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uriStr = tempScratchUriStr
        if (success && uriStr != null && scratchImages.size < 8) {
            scratchImages.add(Uri.parse(uriStr))
        }
    }

    // Step 4 states: Terms
    var rentalDaysStr by RefDelegate({ state.rentalDaysStr }, { state.rentalDaysStr = it })
    var depositType by RefDelegate({ state.depositType }, { state.depositType = it }) // "Cash" or "Passport"

    // Step 5 states: Signature
    var signaturePath by RefDelegate({ state.signaturePath }, { state.signaturePath = it })
    var signatureTrigger by RefDelegate({ state.signatureTrigger }, { state.signatureTrigger = it })
    var hasSigned by RefDelegate({ state.hasSigned }, { state.hasSigned = it })

    // Final Contract
    var generatedContract by RefDelegate({ state.generatedContract }, { state.generatedContract = it })

    // New fields states from document.pdf
    var customerHotelAddress by RefDelegate({ state.customerHotelAddress }, { state.customerHotelAddress = it })
    var customerRoomNumber by RefDelegate({ state.customerRoomNumber }, { state.customerRoomNumber = it })

    fun getTierDailyRate(bike: Scooter, daysCount: Int): Double {
        val baseRate = when {
            daysCount >= 30 -> bike.priceMonthly / daysCount
            daysCount >= 14 -> bike.price2Week / daysCount
            daysCount >= 7 -> bike.priceWeekly / daysCount
            daysCount >= 3 -> bike.price3Day / daysCount
            else -> bike.priceDaily
        }
        return if (RentalRepository.isHighSeasonMode) {
            baseRate * (1.0 + RentalRepository.highSeasonPercentage / 100.0)
        } else {
            baseRate
        }
    }

    // Synchronize preselectedScooterId from parent navigation
    LaunchedEffect(preselectedScooterId, rentalDaysStr, RentalRepository.isHighSeasonMode, RentalRepository.highSeasonPercentage) {
        if (preselectedScooterId != null) {
            selectedScooterId = preselectedScooterId
            val bike = RentalRepository.scooters.find { it.id == preselectedScooterId }
            if (bike != null) {
                scooterPlate = bike.plateNumber
                val days = rentalDaysStr.toIntOrNull() ?: 1
                val rate = getTierDailyRate(bike, days)
                customPricePerDayStr = rate.toInt().toString()
            }
        }
    }

    // Synchronize WhatsApp with phone when toggle is checked
    LaunchedEffect(customerPhone, phoneCountryCode, whatsAppSameAsPhone) {
        if (whatsAppSameAsPhone) {
            customerWhatsApp = "$phoneCountryCode $customerPhone".trim()
        }
    }

    val selectedScooter = RentalRepository.scooters.find { it.id == selectedScooterId }

    // Synchronize custom price when selected bike changes
    LaunchedEffect(selectedScooterId, rentalDaysStr, RentalRepository.isHighSeasonMode, RentalRepository.highSeasonPercentage) {
        if (selectedScooterId != null) {
            val bike = RentalRepository.scooters.find { it.id == selectedScooterId }
            if (bike != null) {
                val days = rentalDaysStr.toIntOrNull() ?: 1
                val rate = getTierDailyRate(bike, days)
                customPricePerDayStr = rate.toInt().toString()
            }
        }
    }

    LaunchedEffect(selectedScooterId) {
        if (selectedScooterId != null) {
            val bike = RentalRepository.scooters.find { it.id == selectedScooterId }
            if (bike != null) {
                val blueprint = RentalRepository.modelDrafts.find { it.brand == bike.brand && it.model == bike.model }
                val bikeFuelBars = blueprint?.fuelBars ?: bike.fuelBars
                fuelIndex = bikeFuelBars
                fuelLevel = "Full ($bikeFuelBars/$bikeFuelBars)"
            }
        }
    }

    val pricePerDay = customPricePerDayStr.toDoubleOrNull() ?: 0.0
    val days = rentalDaysStr.toIntOrNull() ?: 1
    
    val defaultTierRate = if (selectedScooter != null) {
        getTierDailyRate(selectedScooter, days)
    } else {
        0.0
    }
    
    val isUsingDefaultRate = selectedScooter != null && Math.abs(pricePerDay - defaultTierRate) < 2.0
    
    val baseDailyRate = if (selectedScooter != null) {
        val baseRate = selectedScooter.priceDaily
        if (RentalRepository.isHighSeasonMode) {
            baseRate * (1.0 + RentalRepository.highSeasonPercentage / 100.0)
        } else {
            baseRate
        }
    } else {
        0.0
    }
    
    val normalBaseTotal = baseDailyRate * days
    
    val tierTotal = if (selectedScooter != null) {
        val baseTierPrice = when {
            days >= 30 -> selectedScooter.priceMonthly
            days >= 14 -> selectedScooter.price2Week
            days >= 7 -> selectedScooter.priceWeekly
            days >= 3 -> selectedScooter.price3Day
            else -> selectedScooter.priceDaily * days
        }
        if (RentalRepository.isHighSeasonMode) {
            baseTierPrice * (1.0 + RentalRepository.highSeasonPercentage / 100.0)
        } else {
            baseTierPrice
        }
    } else {
        0.0
    }

    val totalPrice = if (isUsingDefaultRate) {
        tierTotal
    } else {
        pricePerDay * days
    }
    
    val discountAmount = if (isUsingDefaultRate) {
        Math.max(0.0, normalBaseTotal - tierTotal)
    } else {
        0.0
    }

    val deposit = if (depositType == "Cash") {
        if (selectedScooter != null) {
            if (selectedScooter.depositAmount > 0.0) {
                selectedScooter.depositAmount
            } else if (selectedScooter.cc >= 300) {
                RentalRepository.maxiDepositAmount
            } else {
                RentalRepository.standardDepositAmount
            }
        } else {
            0.0
        }
    } else {
        0.0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Digital Rental Desk Header
        Column {
            Text(
                text = "Digital Rental Desk",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Step $currentStep of 5 — ${
                    when(currentStep) {
                        1 -> "Bike"
                        2 -> "Employee Input"
                        3 -> "Details & Terms"
                        4 -> "Signature"
                        else -> "Finalize"
                    }
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Horizontal Stepper Bar
        StepperHeader(currentStep = currentStep)

        // Main Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = borderStroke()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Step $currentStep · ${
                        when(currentStep) {
                            1 -> "Bike Selection"
                            2 -> "Employee Input"
                            3 -> "Customer, Condition & Terms"
                            4 -> "Digital Signature"
                            else -> "Agreement Preview"
                        }
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Render Content based on currentStep
                when (currentStep) {
                    1 -> {
                        // STEP 1: BIKE SELECTION
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Available bikes", fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { dropdownExpanded = true }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedScooter?.let { "${it.brand} ${it.model}" }
                                            ?: "Choose a bike model...",
                                        color = if (selectedScooter != null) Color.Unspecified else Color.Gray
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    val itemsList = if (selectedScooter != null && selectedScooter.status == ScooterStatus.RENTED) {
                                        listOf(selectedScooter) + availableScooters
                                    } else {
                                        availableScooters
                                    }

                                    val uniqueModels = itemsList.distinctBy { "${it.brand} ${it.model}" }

                                    if (uniqueModels.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No scooters available") },
                                            onClick = { dropdownExpanded = false }
                                        )
                                    } else {
                                        uniqueModels.forEach { scooter ->
                                            DropdownMenuItem(
                                                text = { Text("${scooter.brand} ${scooter.model}") },
                                                onClick = {
                                                    selectedScooterId = scooter.id
                                                    scooterPlate = ""
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            val isModelSelected = selectedScooterId != null
                            OutlinedTextField(
                                value = scooterPlate,
                                onValueChange = { input ->
                                    scooterPlate = input
                                    // Dynamically check if we can resolve the scooter ID
                                    val bestMatch = findBestPlateMatch(input, selectedScooter)
                                    if (bestMatch != null && input.trim().lowercase() == bestMatch.plateNumber.trim().lowercase()) {
                                        selectedScooterId = bestMatch.id
                                    }
                                },
                                enabled = isModelSelected,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        if (isModelSelected) "Search or enter plate number..." 
                                        else "Select a bike model first..."
                                    ) 
                                },
                                label = { Text("Scooter Plate Number") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            // Auto-correct / Autocomplete suggestions row
                            val suggestions = remember(scooterPlate, selectedScooter) {
                                if (!isModelSelected || selectedScooter == null) {
                                    emptyList()
                                } else {
                                    val query = scooterPlate.trim().lowercase()
                                    val pool = RentalRepository.scooters.filter {
                                        it.brand == selectedScooter.brand &&
                                        it.model == selectedScooter.model &&
                                        (it.status == ScooterStatus.AVAILABLE || it.id == selectedScooter.id)
                                    }
                                    if (query.isEmpty()) {
                                        pool.take(3)
                                    } else {
                                        pool.filter { 
                                            it.plateNumber.lowercase().contains(query)
                                        }.take(3)
                                    }
                                }
                            }
                            
                            if (suggestions.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Available Plates:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    suggestions.forEach { scooter ->
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    scooterPlate = scooter.plateNumber
                                                    selectedScooterId = scooter.id
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = scooter.plateNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // Scratch Photos Section (Max 8 photos) relocated from Step 2
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Scooter Scratch Photos (${scratchImages.size}/8)",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (scratchImages.size < 8) {
                                        TextButton(
                                             onClick = {
                                                 val uri = createTempImageUri("scratch_")
                                                 tempScratchUriStr = uri.toString()
                                                 scratchCameraLauncher.launch(uri)
                                             }
                                        ) {
                                            Text("Add Photo")
                                        }
                                    }
                                }
                                
                                if (scratchImages.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        scratchImages.forEachIndexed { index, bitmap ->
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clip(RoundedCornerShape(8.dp))
                                            ) {
                                                DocumentImage(
                                                    image = bitmap,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable { maximizedDocImage = bitmap }
                                                )
                                                // Small delete button at top-right
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(18.dp)
                                                        .background(Color.Red, shape = CircleShape)
                                                        .clickable { scratchImages.removeAt(index) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No scratch photos captured yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // STEP 2: EMPLOYEE INPUT (DOCUMENT CAPTURE)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Please capture the customer's Passport and Driver's License pictures to proceed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                             // Passport Photo Selector
                             Column(
                                 verticalArrangement = Arrangement.spacedBy(6.dp)
                             ) {
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                                 ) {
                                     Text(text = "Passport Document", fontWeight = FontWeight.SemiBold)
                                     if (isOcrRunning) {
                                         CircularProgressIndicator(
                                             modifier = Modifier.size(16.dp),
                                             strokeWidth = 2.dp,
                                             color = MaterialTheme.colorScheme.primary
                                         )
                                         Text("Scanning...", fontSize = 12.sp, color = Color.Gray)
                                     }
                                 }
                                 if (passportImage != null) {
                                     Row(
                                         verticalAlignment = Alignment.CenterVertically,
                                         horizontalArrangement = Arrangement.spacedBy(12.dp),
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                             .padding(12.dp)
                                     ) {
                                         DocumentImage(
                                             image = passportImage,
                                             modifier = Modifier
                                                 .size(80.dp)
                                                 .clip(RoundedCornerShape(8.dp))
                                                 .clickable { maximizedDocImage = passportImage }
                                         )
                                         Column(modifier = Modifier.weight(1f)) {
                                             Text("Passport Image Attached", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                             Text("Click below to clear or replace", fontSize = 12.sp, color = Color.Gray)
                                         }
                                         TextButton(onClick = { 
                                             passportImage = null 
                                             isOcrRunning = false
                                         }) {
                                             Text("Remove", color = Color.Red)
                                         }
                                     }
                                 } else {
                                     PassportScanViewfinder(
                                         isScanning = isOcrRunning,
                                         onScanClick = {
                                             val uri = createTempImageUri("passport_")
                                             tempPassportUriStr = uri.toString()
                                             passportCameraLauncher.launch(uri)
                                         }
                                     )
                                 }
                             }

                            // Driver's License Photo Selector
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "Driver's License Document", fontWeight = FontWeight.SemiBold)
                                if (licenseImage != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        DocumentImage(
                                            image = licenseImage,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { maximizedDocImage = licenseImage }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Driver's License Image Attached", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Click below to clear or replace", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        TextButton(onClick = { licenseImage = null }) {
                                            Text("Remove", color = Color.Red)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                             val uri = createTempImageUri("license_")
                                             tempLicenseUriStr = uri.toString()
                                             licenseCameraLauncher.launch(uri)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Take Driver's License Photo")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Rental Duration & Custom Pricing", fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = rentalDaysStr,
                                    onValueChange = { rentalDaysStr = it },
                                    label = { Text("Rental Days") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = customPricePerDayStr,
                                    onValueChange = { customPricePerDayStr = it },
                                    label = { Text("Daily Rate (฿)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = rentalTimeStr,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    if (digits.length <= 4) {
                                        rentalTimeStr = if (digits.length >= 3) {
                                            "${digits.substring(0, 2)}:${digits.substring(2)}"
                                        } else {
                                            digits
                                        }
                                    }
                                },
                                label = { Text("Start Time (HH:mm)") },
                                placeholder = { Text("e.g. 14:30") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                border = borderStroke()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Fuel Level",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = fuelLevel,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Outlined container for blocks
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color.LightGray.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                            .background(Color.White, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val blueprint = selectedScooter?.let { bike ->
                                                RentalRepository.modelDrafts.find { it.brand == bike.brand && it.model == bike.model }
                                            }
                                            val maxBars = blueprint?.fuelBars ?: selectedScooter?.fuelBars ?: 6
                                            for (i in 1..maxBars) {
                                                val isFilled = fuelIndex >= i
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isFilled) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.2f))
                                                        .clickable {
                                                            val newIndex = if (fuelIndex == 1 && i == 1) 0 else i
                                                            fuelIndex = newIndex
                                                            fuelLevel = if (newIndex == 0) "Empty (0/$maxBars)" else if (newIndex == maxBars) "Full ($maxBars/$maxBars)" else "$newIndex/$maxBars"
                                                        }
                                                )
                                            }
                                        }
                                    }

                                    // Bottom labels E and F
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "E", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Text(text = "F", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Estimated Price:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "${totalPrice.toInt()} ฿", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    if (discountAmount > 0.0) {
                                        Text(
                                            text = "Discount applied: Saved ${discountAmount.toInt()} ฿!",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // STEP 3: CUSTOMER DETAILS
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Summarized data card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedScooter?.let { "${it.brand} ${it.model}" } ?: "Selected Vehicle",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Plate: $scooterPlate",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.DarkGray
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "Rental Duration: $rentalDaysStr days", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text(text = "Start Time: $rentalTimeStr", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Passport: $passportNumber",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                            Text(
                                                text = "Nationality: $nationality",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Passport: Captured",
                                            fontSize = 11.sp,
                                            color = BrandGreenDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(text = "•", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            text = "License: Captured",
                                            fontSize = 11.sp,
                                            color = BrandGreenDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(text = "•", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            text = "Photos: ${scratchImages.size} attached",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = passportNumber,
                                onValueChange = { passportNumber = it },
                                label = { Text("Passport Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = nationality,
                                onValueChange = { nationality = it },
                                label = { Text("Nationality") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Phone Number with Country Code Dropdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val countries = listOf(
                                    Triple("+66", "TH", "🇹🇭"),
                                    Triple("+1", "US", "🇺🇸"),
                                    Triple("+44", "GB", "🇬🇧"),
                                    Triple("+81", "JP", "🇯🇵"),
                                    Triple("+7", "RU", "🇷🇺"),
                                    Triple("+86", "CN", "🇨🇳"),
                                    Triple("+49", "DE", "🇩🇪"),
                                    Triple("+33", "FR", "🇫🇷"),
                                    Triple("+65", "SG", "🇸🇬"),
                                    Triple("+61", "AU", "🇦🇺"),
                                    Triple("+91", "IN", "🇮🇳"),
                                    Triple("+60", "MY", "🇲🇾"),
                                    Triple("+84", "VN", "🇻🇳"),
                                    Triple("+82", "KR", "🇰🇷"),
                                    Triple("+62", "ID", "🇮🇩"),
                                    Triple("+886", "TW", "🇹🇼"),
                                    Triple("+31", "NL", "🇳🇱"),
                                    Triple("+39", "IT", "🇮🇹"),
                                    Triple("+34", "ES", "🇪🇸"),
                                    Triple("+63", "PH", "🇵🇭"),
                                    Triple("+95", "MM", "🇲🇲"),
                                    Triple("+856", "LA", "🇱🇦"),
                                    Triple("+855", "KH", "🇰🇭"),
                                    Triple("+852", "HK", "🇭🇰")
                                )

                                Box(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .clickable { countryDropdownExpanded = true }
                                        .padding(vertical = 16.dp, horizontal = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "$phoneCountryFlag $phoneCountryCode", fontSize = 14.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", modifier = Modifier.size(16.dp))
                                    }

                                    DropdownMenu(
                                        expanded = countryDropdownExpanded,
                                        onDismissRequest = {
                                            countryDropdownExpanded = false
                                            countrySearchQuery = ""
                                        },
                                        modifier = Modifier
                                            .width(280.dp)
                                            .heightIn(max = 350.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = countrySearchQuery,
                                            onValueChange = { countrySearchQuery = it },
                                            placeholder = { Text("Search country...") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                            trailingIcon = {
                                                if (countrySearchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { countrySearchQuery = "" }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        val filteredCountries = countries.filter { (code, country, flag) ->
                                            country.contains(countrySearchQuery, ignoreCase = true) ||
                                            code.contains(countrySearchQuery)
                                        }

                                        if (filteredCountries.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No results found", fontSize = 14.sp, color = Color.Gray) },
                                                onClick = {},
                                                enabled = false
                                            )
                                        } else {
                                            filteredCountries.forEach { (code, country, flag) ->
                                                DropdownMenuItem(
                                                    text = { Text("$flag $code ($country)", fontSize = 14.sp) },
                                                    onClick = {
                                                        phoneCountryCode = code
                                                        phoneCountryFlag = flag
                                                        countryDropdownExpanded = false
                                                        countrySearchQuery = ""
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { input ->
                                         customerPhone = input
                                         val cleanInput = input.trim()
                                         if (cleanInput.isNotEmpty()) {
                                             val countriesList = listOf(
                                                 Triple("+66", "TH", "🇹🇭"),
                                                 Triple("+1", "US", "🇺🇸"),
                                                 Triple("+44", "GB", "🇬🇧"),
                                                 Triple("+81", "JP", "🇯🇵"),
                                                 Triple("+7", "RU", "🇷🇺"),
                                                 Triple("+86", "CN", "🇨🇳"),
                                                 Triple("+49", "DE", "🇩🇪"),
                                                 Triple("+33", "FR", "🇫🇷"),
                                                 Triple("+65", "SG", "🇸🇬"),
                                                 Triple("+61", "AU", "🇦🇺"),
                                                 Triple("+91", "IN", "🇮🇳"),
                                                 Triple("+60", "MY", "🇲🇾"),
                                                 Triple("+84", "VN", "🇻🇳"),
                                                 Triple("+82", "KR", "🇰🇷"),
                                                 Triple("+62", "ID", "🇮🇩"),
                                                 Triple("+886", "TW", "🇹🇼"),
                                                 Triple("+31", "NL", "🇳🇱"),
                                                 Triple("+39", "IT", "🇮🇹"),
                                                 Triple("+34", "ES", "🇪🇸"),
                                                 Triple("+63", "PH", "🇵🇭"),
                                                 Triple("+95", "MM", "🇲🇲"),
                                                 Triple("+856", "LA", "🇱🇦"),
                                                 Triple("+855", "KH", "🇰🇭"),
                                                 Triple("+852", "HK", "🇭🇰")
                                             )
                                             val sortedCountries = countriesList.sortedByDescending { it.first.length }
                                             for (country in sortedCountries) {
                                                 val code = country.first
                                                 val codeDigits = code.removePrefix("+")
                                                 
                                                 if (cleanInput.startsWith(code)) {
                                                     phoneCountryCode = code
                                                     phoneCountryFlag = country.third
                                                     customerPhone = cleanInput.substring(code.length).trim()
                                                     break
                                                 } else if (cleanInput.startsWith("+$codeDigits")) {
                                                     phoneCountryCode = code
                                                     phoneCountryFlag = country.third
                                                     customerPhone = cleanInput.substring(codeDigits.length + 1).trim()
                                                     break
                                                 } else if (cleanInput.startsWith(codeDigits) && codeDigits.length > 1 && (cleanInput.length > codeDigits.length || codeDigits.length >= 3)) {
                                                     phoneCountryCode = code
                                                     phoneCountryFlag = country.third
                                                     customerPhone = cleanInput.substring(codeDigits.length).trim()
                                                     break
                                                 }
                                             }
                                         }
                                    },
                                    label = { Text("Phone Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }

                            // WhatsApp Number input
                            OutlinedTextField(
                                value = customerWhatsApp,
                                onValueChange = {
                                    customerWhatsApp = it
                                    whatsAppSameAsPhone = false
                                },
                                label = { Text("WhatsApp Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = whatsAppSameAsPhone,
                                    onCheckedChange = { checked ->
                                        whatsAppSameAsPhone = checked
                                        if (checked) {
                                            customerWhatsApp = "$phoneCountryCode $customerPhone".trim()
                                        }
                                    }
                                )
                                Text("WhatsApp is same as phone number", style = MaterialTheme.typography.bodyMedium)
                            }

                            OutlinedTextField(
                                value = customerEmail,
                                onValueChange = { customerEmail = it },
                                label = { Text("Customer Email") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )



                            OutlinedTextField(
                                value = customerHotelAddress,
                                onValueChange = { customerHotelAddress = it },
                                label = { Text("Address / Hotel / Villa") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customerRoomNumber,
                                onValueChange = { customerRoomNumber = it },
                                label = { Text("Room Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Show Document Previews/Details
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                border = borderStroke()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Attached Documents (Captured by Employee)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Passport", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (passportImage != null) {
                                                DocumentImage(
                                                    image = passportImage,
                                                    modifier = Modifier
                                                        .size(70.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { maximizedDocImage = passportImage }
                                                )
                                            } else {
                                                Text("Missing", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Driver's License", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (licenseImage != null) {
                                                DocumentImage(
                                                    image = licenseImage,
                                                    modifier = Modifier
                                                        .size(70.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { maximizedDocImage = licenseImage }
                                                )
                                            } else {
                                                Text("Missing", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (scratchImages.isNotEmpty()) {
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                        Text("Scooter Scratch Photos", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            scratchImages.forEachIndexed { index, bitmap ->
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    DocumentImage(
                                                        image = bitmap,
                                                        modifier = Modifier
                                                            .size(60.dp)
                                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { maximizedDocImage = bitmap }
                                                    )
                                                    Text("Scratch ${index + 1}", fontSize = 8.sp, color = Color.DarkGray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                            // Helmet Included Count
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Helmets Included", fontWeight = FontWeight.SemiBold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(1, 2).forEach { count ->
                                        ElevatedFilterChip(
                                            selected = helmetCount == count,
                                            onClick = { helmetCount = count },
                                            label = { Text("$count Helmet(s)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Body Condition Notes
                            OutlinedTextField(
                                value = bodyConditionNotes,
                                onValueChange = { bodyConditionNotes = it },
                                label = { Text("Physical Condition Notes") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Text(
                            text = "Rental Deposit & Terms",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Deposit Type", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Cash", "Passport").forEach { type ->
                                        ElevatedFilterChip(
                                            selected = depositType == type,
                                            onClick = { depositType = type },
                                            label = { Text(type, fontSize = 13.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            if (depositType == "Cash") {
                                OutlinedTextField(
                                    value = "${deposit.toInt()} ฿",
                                    onValueChange = { },
                                    enabled = false,
                                    label = { Text("Deposit Amount (Fixed by Fleet CC)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Original Customer Passport will be held as the security deposit.",
                                        modifier = Modifier.padding(12.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }



                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Custom Rental Terms / Notes") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )

                            // Terms Acceptance Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (termsRead) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(1.dp, if (termsRead) MaterialTheme.colorScheme.secondary else Color.LightGray)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Terms & Conditions",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Renter must read and accept the terms and conditions before they can sign and finalize.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { showTermsDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (termsRead) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text(if (termsRead) "Review Terms" else "Read Terms & Conditions")
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (termsRead) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Read",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Accepted",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontSize = 12.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "Not accepted yet",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Summary Box
                            if (selectedScooter != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Price Summary",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Scooter Rate:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(text = "${pricePerDay} ฿ / day", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Total Days:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(text = "$days days", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val startCal = Calendar.getInstance()
                                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val todayStr = dateFormat.format(startCal.time)
                                            startCal.add(Calendar.DAY_OF_YEAR, days)
                                            val finishDateStr = dateFormat.format(startCal.time)

                                            Text(text = "Rental Period:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(
                                                text = "$todayStr $rentalTimeStr to $finishDateStr $rentalTimeStr",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                        if (discountAmount > 0.0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "Base Rent Price:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text(text = "${normalBaseTotal.toInt()} ฿", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "Discount Applied:", color = MaterialTheme.colorScheme.secondary)
                                                Text(text = "-${discountAmount.toInt()} ฿", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Total Rent Price:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(text = "${totalPrice.toInt()} ฿", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Refundable Deposit:", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(
                                                text = if (depositType == "Cash") "${deposit.toInt()} ฿" else "Original Passport Held",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Total Payment Required:", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(
                                                text = if (depositType == "Cash") "${(totalPrice + deposit).toInt()} ฿" else "${totalPrice.toInt()} ฿ + Passport",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // STEP 4: DIGITAL SIGNATURE
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Please draw your signature below:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Interactive Signature Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    val clampedX = offset.x.coerceIn(0f, size.width.toFloat())
                                                    val clampedY = offset.y.coerceIn(0f, size.height.toFloat())
                                                    signaturePath.moveTo(clampedX, clampedY)
                                                    hasSigned = true
                                                },
                                                onDrag = { change, _ ->
                                                    val clampedX = change.position.x.coerceIn(0f, size.width.toFloat())
                                                    val clampedY = change.position.y.coerceIn(0f, size.height.toFloat())
                                                    signaturePath.lineTo(clampedX, clampedY)
                                                    signatureTrigger++
                                                }
                                            )
                                        }
                                ) {
                                    val dummy = signatureTrigger // Triggers Canvas redraw on state change
                                    drawPath(
                                        path = signaturePath,
                                        color = Color.Black,
                                        style = Stroke(width = 5f)
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    signaturePath = Path()
                                    signatureTrigger = 0
                                    hasSigned = false
                                }
                            ) {
                                Text("Clear Signature")
                            }
                        }
                    }
                    else -> {
                        // STEP 5: FINALIZE
                        if (generatedContract == null) {
                            // Setup final contract details before showing receipt
                            val newContractId = "C" + System.currentTimeMillis().toString().takeLast(5)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val currentDate = dateFormat.format(Date())

                            val contractNotes = buildString {
                                append(notes)
                                if (notes.isNotEmpty()) append("\n")
                                append("Fuel: $fuelLevel | Helmets: $helmetCount | Scratches: ${scratchImages.size} photos\n")
                                append("Condition: $bodyConditionNotes\n")
                                append("Deposit: ${if (depositType == "Cash") "Cash ${deposit.toInt()} ฿" else "Original Passport Held"}")
                            }

                            val fullPhone = "$phoneCountryCode $customerPhone"
                            val fullIdDetails = buildString {
                                 if (licenseImage != null) {
                                     append("License Photo Attached")
                                 }
                                 if (passportImage != null) {
                                     if (isNotEmpty()) append(" | ")
                                     append("Passport Photo Attached")
                                 }
                            }

                            val blueprint = selectedScooter?.let { bike ->
                                RentalRepository.modelDrafts.find { it.brand == bike.brand && it.model == bike.model }
                            }
                            val maxBars = blueprint?.fuelBars ?: selectedScooter?.fuelBars ?: 6
                            val costPerBar = blueprint?.fuelCostPerBar ?: selectedScooter?.fuelCostPerBar ?: RentalRepository.globalFuelCostPerBar

                            generatedContract = RentalContract(
                                id = newContractId,
                                customerName = customerName,
                                customerPhone = fullPhone,
                                licenseNumber = fullIdDetails,
                                scooterId = selectedScooterId!!,
                                days = days,
                                totalPrice = totalPrice,
                                depositAmount = deposit,
                                notes = contractNotes,
                                startDate = "$currentDate $rentalTimeStr",
                                isCompleted = false,
                                customerEmail = customerEmail.trim(),
                                customerWhatsApp = customerWhatsApp.trim(),
                                nationality = nationality.trim(),
                                passportNumber = passportNumber.trim(),
                                hotelAddress = customerHotelAddress.trim(),
                                roomNumber = customerRoomNumber.trim(),
                                replacementValue = RentalRepository.globalReplacementValue,
                                extraCostPerHour = RentalRepository.globalExtraCostPerHour,
                                fuelCostPerBar = costPerBar,
                                passportHold = (depositType == "Passport"),
                                helmetCount = helmetCount,
                                fuelLevel = ((fuelIndex.toDouble() / maxBars.toDouble()) * 100).toInt(),
                                deliveryCost = RentalRepository.globalDeliveryCost,
                                passportImage = passportImage,
                                licenseImage = licenseImage,
                                scratchImages = scratchImages.toList()
                            )

                            // Also update the scooter's plate number in the repository
                            val scooterIndex = RentalRepository.scooters.indexOfFirst { it.id == selectedScooterId }
                            if (scooterIndex != -1) {
                                val existingScooter = RentalRepository.scooters[scooterIndex]
                                RentalRepository.scooters[scooterIndex] = existingScooter.copy(plateNumber = scooterPlate)
                            }
                        }

                        // Display agreement preview with signature path embedded
                        ContractReceiptPreview(
                            contract = generatedContract!!,
                            scooter = selectedScooter!!,
                            signaturePath = signaturePath,
                            signatureTrigger = signatureTrigger,
                            passportImage = passportImage,
                            licenseImage = licenseImage,
                            scratchImages = scratchImages,
                            depositType = depositType
                        )
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Footer Buttons (Next & Back)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back / Cancel Button
                    if (currentStep == 3) {
                        OutlinedButton(
                            onClick = {
                                state.reset()
                                onClearSelection()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (currentStep > 1) {
                                    currentStep--
                                }
                            },
                            enabled = currentStep > 1,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("← Back")
                        }
                    }

                    // Next Button
                    if (currentStep < 5) {
                        Button(
                            onClick = {
                                // Validation logic per step
                                when (currentStep) {
                                    1 -> {
                                        val query = scooterPlate.trim()
                                        if (query.isEmpty()) {
                                            Toast.makeText(context, "Please enter scooter plate number", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val bestMatch = findBestPlateMatch(query, selectedScooter)
                                        if (bestMatch != null) {
                                            scooterPlate = bestMatch.plateNumber
                                            selectedScooterId = bestMatch.id
                                        } else {
                                            Toast.makeText(context, "Plate number does not exist in our system", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (scratchImages.isEmpty()) {
                                            Toast.makeText(context, "Please attach at least 1 scooter scratch photo", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                    2 -> {
                                        if (isOcrRunning) {
                                            Toast.makeText(context, "Please wait for passport scanning to complete", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (passportImage == null) {
                                            Toast.makeText(context, "Please capture Passport photo", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (licenseImage == null) {
                                            Toast.makeText(context, "Please capture Driver's License photo", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val daysInput = rentalDaysStr.toIntOrNull()
                                        if (daysInput == null || daysInput < 1) {
                                            Toast.makeText(context, "Please enter a valid rental duration (at least 1 day)", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val rateInput = customPricePerDayStr.toDoubleOrNull()
                                        if (rateInput == null || rateInput <= 0.0) {
                                            Toast.makeText(context, "Please enter a valid custom daily rate", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val timeParts = rentalTimeStr.trim().split(":")
                                        if (timeParts.size != 2) {
                                            Toast.makeText(context, "Please enter start time in HH:mm format (e.g. 14:30)", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val hourVal = timeParts[0].toIntOrNull()
                                        val minuteVal = timeParts[1].toIntOrNull()
                                        if (hourVal == null || minuteVal == null || hourVal !in 0..23 || minuteVal !in 0..59) {
                                            Toast.makeText(context, "Please enter a valid start time (00:00 to 23:59)", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (hourVal > 20 || (hourVal == 20 && minuteVal > 0)) {
                                            Toast.makeText(context, "Start time cannot be later than 8:00 PM (20:00)", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                    3 -> {
                                        if (customerName.trim().isEmpty()) {
                                            Toast.makeText(context, "Please enter customer name", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val digitsOnly = customerPhone.filter { it.isDigit() }
                                        if (digitsOnly.length !in 7..15) {
                                            Toast.makeText(context, "Please enter a valid phone number (7-15 digits)", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (customerWhatsApp.trim().isEmpty()) {
                                            Toast.makeText(context, "Please enter WhatsApp number", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val emailTrimmed = customerEmail.trim()
                                        if (emailTrimmed.isEmpty()) {
                                            Toast.makeText(context, "Please enter customer email", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()
                                        if (!isEmailValid) {
                                            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (!termsRead) {
                                            Toast.makeText(context, "Please read and accept the Terms & Conditions before signing", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                    }
                                    4 -> {
                                        if (!hasSigned) {
                                            Toast.makeText(context, "Please sign the document before proceeding", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                }
                                currentStep++
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Next →")
                        }
                    } else {
                        // Print & Save Contract button (inside Step 7)
                        Button(
                            onClick = {
                                showPasswordDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Print & Track Rental")
                        }
                    }
                }
            }
            
            // Full Terms Dialog
            if (showTermsDialog) {
                AlertDialog(
                    onDismissRequest = { showTermsDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                termsRead = true
                                showTermsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("I Accept")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTermsDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    title = {
                        Text(
                            text = "Terms & Conditions\nข้อตกลงและเงื่อนไขการเช่า",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(RENTAL_TERMS) { term ->
                                Text(
                                    text = term,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                )
            }
            
            // Password Authorization Dialog
            if (showPasswordDialog) {
                var passwordInput by remember { mutableStateOf("") }
                var isError by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showPasswordDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (passwordInput == "1234") {
                                    isError = false
                                    showPasswordDialog = false
                                    generatedContract?.let { contract ->
                                        RentalRepository.addContract(contract, context)
                                        // Save signature path (deep copy)
                                        val savedPath = Path()
                                        savedPath.addPath(signaturePath)
                                        RentalRepository.signaturePaths[contract.id] = savedPath
                                        
                                        // Auto-generate and save PDF contract
                                        generateContractPdf(
                                            context = context,
                                            contract = contract,
                                            scooter = selectedScooter!!,
                                            signaturePath = signaturePath,
                                            passportImage = passportImage,
                                            licenseImage = licenseImage
                                        ) { generatedFile ->
                                            pdfFileToShow = generatedFile
                                            // Automatically trigger WhatsApp and Email share intent
                                            sharePdfToCustomer(context, generatedFile, contract)
                                        }
                                        
                                        Toast.makeText(context, "Contract finalized successfully!", Toast.LENGTH_LONG).show()

                                        // Reset all fields
                                        customerName = ""
                                        phoneCountryCode = "+66"
                                        customerPhone = ""
                                        passportImage = null
                                        licenseImage = null
                                        rentalDaysStr = "1"
                                        depositType = "Cash"
                                        notes = ""
                                        selectedScooterId = null
                                        scooterPlate = ""
                                        customerWhatsApp = ""
                                        whatsAppSameAsPhone = false
                                        customPricePerDayStr = "0"
                                        fuelLevel = "Full (6/6)"
                                        fuelIndex = 6
                                        rentalTimeStr = getInitialRentalTime()
                                        helmetCount = 1
                                        bodyConditionNotes = ""
                                        scratchImages.clear()
                                        customerHotelAddress = ""
                                        customerRoomNumber = ""
                                        signaturePath = Path()
                                        signatureTrigger = 0
                                        hasSigned = false
                                        onClearSelection()
                                        generatedContract = null
                                        currentStep = 1 // Reset step to 1
                                        onContractGenerated()
                                    }
                                } else {
                                    isError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Confirm Payment", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showPasswordDialog = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    },
                    title = {
                        Text(
                            text = "Authorize Payment Receipt",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Please enter the employee authorization password to verify payment receipt.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password (Default: 1234)") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                                isError = isError,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            if (isError) {
                                Text(
                                    text = "Incorrect password. Please try again.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (maximizedDocImage != null) {
                Dialog(onDismissRequest = { maximizedDocImage = null }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .background(Color.Black, shape = RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        ZoomableDocumentImage(
                            image = maximizedDocImage!!,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                        IconButton(
                            onClick = { maximizedDocImage = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            if (pdfFileToShow != null) {
                PdfViewerDialog(file = pdfFileToShow!!) {
                    pdfFileToShow = null
                }
            }
        }
    }
}

@Composable
fun ContractReceiptPreview(
    contract: RentalContract,
    scooter: Scooter,
    signaturePath: Path,
    signatureTrigger: Int,
    passportImage: Any? = null,
    licenseImage: Any? = null,
    scratchImages: List<Any> = emptyList(),
    depositType: String = "Cash"
) {
    var maximizedDocImageReceipt by remember { mutableStateOf<Any?>(null) }
    var pdfFileToShowReceipt by remember { mutableStateOf<File?>(null) }
    val actualPassportImage = passportImage ?: contract.passportImage
    val actualLicenseImage = licenseImage ?: contract.licenseImage
    val actualScratchImages = if (scratchImages.isNotEmpty()) scratchImages else contract.scratchImages
    val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val finishDate = remember(contract) {
        try {
            val start = parser.parse(contract.startDate) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = start
            cal.add(Calendar.DAY_OF_YEAR, contract.days)
            parser.format(cal.time)
        } catch (e: Exception) {
            ""
        }
    }
    val startDateOnly = contract.startDate.split(" ").firstOrNull() ?: ""
    val startTimeOnly = contract.startDate.split(" ").getOrNull(1) ?: ""
    val finishDateOnly = finishDate.split(" ").firstOrNull() ?: ""
    val finishTimeOnly = finishDate.split(" ").getOrNull(1) ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandYellowLight) // Soft themed yellow paper background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(2.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Logo
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cheap as Chips",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.Black
                )
                Text(
                    text = "195/7 Soi Phrabaramee, Patong, Phuket 83150",
                    fontSize = 10.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "+66 87 188 9047 | info@motorbikerentalphuket.com",
                    fontSize = 9.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Motorbike Rental Agreement / สัญญาเช่ารถจักรยานยนต์",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }

            HorizontalDivider(color = Color.Black, thickness = 1.dp)

            // Vehicle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "License Plate Number", fontSize = 10.sp, color = Color.Gray)
                    Text(text = scooter.plateNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Make / Model", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "${scooter.brand} ${scooter.model}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))

            // Customer
            Text(text = "CUSTOMER DETAILS / ข้อมูลผู้เช่า", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(text = "Customer Name", fontSize = 9.sp, color = Color.Gray)
                    Text(text = contract.customerName, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(0.8f)) {
                    Text(text = "Nationality", fontSize = 9.sp, color = Color.Gray)
                    Text(text = contract.nationality.ifEmpty { "N/A" }, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Phone No.", fontSize = 9.sp, color = Color.Gray)
                    Text(text = contract.customerPhone, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Passport Number", fontSize = 9.sp, color = Color.Gray)
                    Text(text = contract.passportNumber.ifEmpty { "N/A" }, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(text = "Address / Hotel / Villa", fontSize = 9.sp, color = Color.Gray)
                    Text(text = contract.hotelAddress.ifEmpty { "N/A" }, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(0.5f)) {
                    Text(text = "Room", fontSize = 9.sp, color = Color.Gray)
                    Text(text = contract.roomNumber.ifEmpty { "N/A" }, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }

            HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))

            // Booking Details
            Text(text = "BOOKING DETAILS / ข้อมูลการจอง", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Booking Start Date", fontSize = 9.sp, color = Color.Gray)
                    Text(text = startDateOnly, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Start Time", fontSize = 9.sp, color = Color.Gray)
                    Text(text = startTimeOnly, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Booking End Date", fontSize = 9.sp, color = Color.Gray)
                    Text(text = finishDateOnly, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "End Time", fontSize = 9.sp, color = Color.Gray)
                    Text(text = finishTimeOnly, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    val rate = if (contract.days > 0) (contract.totalPrice / contract.days).toInt() else scooter.pricePerDay.toInt()
                    Text(text = "Price Per Day", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "$rate ฿", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Total Days", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.days}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Total Price", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.totalPrice.toInt()} ฿", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Deposit", fontSize = 9.sp, color = Color.Gray)
                    Text(text = if (contract.passportHold) "Passport Held" else "${contract.depositAmount.toInt()} ฿", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (contract.passportHold) Color(0xFFC62828) else Color.Unspecified)
                }
            }

            HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))

            // Fees and Conditions
            Text(text = "ADDITIONAL DETAILS / ข้อมูลเพิ่มเติม", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Replacement Value", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.replacementValue.toInt()} ฿", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Extra Cost / Hour", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.extraCostPerHour.toInt()} ฿", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Fuel Cost / Bar", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.fuelCostPerBar.toInt()} ฿", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Passport Hold", fontSize = 9.sp, color = Color.Gray)
                    Text(text = if (contract.passportHold) "Yes" else "No", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "No. of Helmets", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.helmetCount}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Fuel Level", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.fuelLevel}%", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Delivery Cost", fontSize = 9.sp, color = Color.Gray)
                    Text(text = "${contract.deliveryCost.toInt()} ฿", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
            }

            HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))

            // Attached Documents
            if (actualPassportImage != null || actualLicenseImage != null) {
                Text(text = "ATTACHED DOCUMENTS / เอกสารแนบ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (actualPassportImage != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DocumentImage(
                                image = actualPassportImage,
                                modifier = Modifier
                                    .size(70.dp)
                                    .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { maximizedDocImageReceipt = actualPassportImage }
                            )
                            Text("Passport", fontSize = 9.sp, color = Color.DarkGray)
                        }
                    }
                    if (actualLicenseImage != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DocumentImage(
                                image = actualLicenseImage,
                                modifier = Modifier
                                    .size(70.dp)
                                    .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { maximizedDocImageReceipt = actualLicenseImage }
                            )
                            Text("License", fontSize = 9.sp, color = Color.DarkGray)
                        }
                    }
                }
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
            }

            // Scratch Previews
            if (actualScratchImages.isNotEmpty()) {
                Text(
                    text = "SCOOTER SCRATCH IMAGES / รูปภาพรอยขีดข่วนรถ (${actualScratchImages.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actualScratchImages.forEachIndexed { index, item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DocumentImage(
                                image = item,
                                modifier = Modifier
                                    .size(50.dp)
                                    .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { maximizedDocImageReceipt = item }
                            )
                            Text("Scratch ${index + 1}", fontSize = 8.sp, color = Color.DarkGray)
                        }
                    }
                }
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
            }

            // Terms and Conditions Section (Scrollable card block)
            var showFullTerms by remember { mutableStateOf(false) }
            val terms = RENTAL_TERMS

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "TERMS & CONDITIONS / ข้อตกลงและเงื่อนไขการเช่า", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val context = LocalContext.current
                    TextButton(onClick = {
                        generateContractPdf(context, contract, scooter, signaturePath, actualPassportImage, actualLicenseImage) { generatedFile ->
                            pdfFileToShowReceipt = generatedFile
                        }
                    }) {
                        Text("Download PDF", fontSize = 10.sp)
                    }
                    TextButton(onClick = { showFullTerms = true }) {
                        Text("View Full Terms", fontSize = 10.sp)
                    }
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(terms) { term ->
                        Text(
                            text = term,
                            fontSize = 9.sp,
                            color = Color.DarkGray,
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            // Full Terms Dialog
            if (showFullTerms) {
                AlertDialog(
                    onDismissRequest = { showFullTerms = false },
                    confirmButton = {
                        TextButton(onClick = { showFullTerms = false }) {
                            Text("Close")
                        }
                    },
                    title = {
                        Text(
                            text = "Terms & Conditions\nข้อตกลงและเงื่อนไขการเช่า",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 500.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(terms) { term ->
                                Text(
                                    text = term,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                )
            }

            HorizontalDivider(color = Color.Black, thickness = 1.dp)

            // Signature (scaled to fit within the box)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .padding(4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dummy = signatureTrigger

                            val bounds = signaturePath.getBounds()

                            if (!bounds.isEmpty && bounds.width > 0f && bounds.height > 0f) {

                                val padding = 4.dp.toPx()

                                val availableWidth = size.width - padding * 2
                                val availableHeight = size.height - padding * 2

                                val scale = minOf(
                                    availableWidth / bounds.width,
                                    availableHeight / bounds.height
                                )

                                val translateX =
                                    padding - bounds.left * scale

                                val translateY =
                                    (size.height - bounds.height * scale) / 2f - bounds.top * scale

                                withTransform({
                                    translate(translateX, translateY)
                                    scale(scale, scale, pivot = Offset.Zero)
                                }) {
                                    drawPath(
                                        path = signaturePath,
                                        color = Color.Black,
                                        style = Stroke(
                                            width = 3f / scale
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Renter Signature / ผู้เช่า",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = "Date: $startDateOnly", fontSize = 10.sp, color = Color.Black)
                    Text(text = "Time: $startTimeOnly", fontSize = 10.sp, color = Color.Black)
                }
            }
        }
    }

    if (maximizedDocImageReceipt != null) {
        Dialog(onDismissRequest = { maximizedDocImageReceipt = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .background(Color.Black, shape = RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                ZoomableDocumentImage(
                    image = maximizedDocImageReceipt!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                IconButton(
                    onClick = { maximizedDocImageReceipt = null },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (pdfFileToShowReceipt != null) {
        PdfViewerDialog(file = pdfFileToShowReceipt!!) {
            pdfFileToShowReceipt = null
        }
    }
}

private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))

@Composable
fun rememberUriBitmap(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    if (uri == null) return null
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (uri.scheme == "http" || uri.scheme == "https") {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(uri.toString()).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null) {
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                bmp?.asImageBitmap()
                            } else null
                        } else null
                    }
                } else {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    bitmap.asImageBitmap()
                }
            } catch (e: Exception) {
                Log.e("rememberUriBitmap", "Error loading uri: $uri", e)
                null
            }
        }
    }
    return bitmapState.value
}

@Composable
fun ZoomableDocumentImage(
    image: Any?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x * scale
                        offsetY += pan.y * scale
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        DocumentImage(
            image = image,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .fillMaxSize()
        )
    }
}

@Composable
fun DocumentImage(
    image: Any?,
    modifier: Modifier = Modifier
) {
    if (image == null) return
    when (image) {
        is Uri -> {
            val bitmap = rememberUriBitmap(image)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = modifier
                )
            }
        }
        is String -> {
            val uri = remember(image) { Uri.parse(image) }
            val bitmap = rememberUriBitmap(uri)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = modifier
                )
            }
        }
        is android.graphics.Bitmap -> {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = modifier
            )
        }
    }
}

fun findBestPlateMatch(input: String, selectedScooter: Scooter?): Scooter? {
    val query = input.trim().lowercase()
    if (query.isEmpty()) return null
    
    val pool = if (selectedScooter != null) {
        RentalRepository.scooters.filter {
            it.brand == selectedScooter.brand &&
            it.model == selectedScooter.model &&
            (it.status == ScooterStatus.AVAILABLE || it.id == selectedScooter.id)
        }
    } else {
        RentalRepository.scooters.filter { it.status == ScooterStatus.AVAILABLE }
    }
    
    // 1. Try to find exact match (ignoring case)
    val exactMatch = pool.find { it.plateNumber.trim().lowercase() == query }
    if (exactMatch != null) return exactMatch
    
    // 2. Try to find a match that ends with the query (e.g. typing "88-888" matches "Phuket 88-888")
    val endsWithMatch = pool.find { it.plateNumber.trim().lowercase().endsWith(query) }
    if (endsWithMatch != null) return endsWithMatch
    
    // 3. Try to find a match that contains the query
    val containsMatch = pool.find { it.plateNumber.trim().lowercase().contains(query) }
    return containsMatch
}

fun runPassportOcr(
    context: Context,
    imageSource: Any?,
    onResult: (name: String, passportNum: String, nationality: String) -> Unit,
    onComplete: () -> Unit
) {
    if (imageSource == null) {
        onComplete()
        return
    }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    val inputImage = try {
        when (imageSource) {
            is Bitmap -> InputImage.fromBitmap(imageSource, 0)
            is Uri -> InputImage.fromFilePath(context, imageSource)
            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    
    if (inputImage == null) {
        onComplete()
        return
    }
    
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val text = visionText.text
            var extractedPassportNumber = ""
            var extractedName = ""
            var extractedNationality = ""
            
            val lines = text.split("\n").map { it.trim() }
            val passportRegex = Regex("[A-Z0-9]{8,12}")
            val nameKeywords = listOf("surname", "given name", "name", "full name", "names", "first name", "last name")
            val nationalityKeywords = listOf("nationality", "national", "citizen", "citizenship")
            
            // Look for MRZ (Machine Readable Zone)
            var mrzLine1 = ""
            var mrzLine2 = ""
            for (line in lines) {
                val cleaned = line.replace(" ", "")
                if (cleaned.length >= 30 && (cleaned.startsWith("P<") || cleaned.startsWith("PCH") || cleaned.startsWith("PD<") || cleaned.startsWith("PM<"))) {
                    mrzLine1 = cleaned
                } else if (cleaned.length >= 30 && cleaned.contains(Regex("[A-Z0-9<]{20}"))) {
                    if (mrzLine1.isNotEmpty() && mrzLine2.isEmpty() && cleaned != mrzLine1) {
                        mrzLine2 = cleaned
                    }
                }
            }
            
            if (mrzLine1.isNotEmpty()) {
                val country = mrzLine1.substring(2, 5).replace("<", "")
                extractedNationality = country
                
                val namePart = mrzLine1.substring(5)
                val parts = namePart.split("<<").filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) {
                    val surname = parts.firstOrNull()?.replace("<", " ")?.trim() ?: ""
                    val givenNames = parts.getOrNull(1)?.replace("<", " ")?.trim() ?: ""
                    extractedName = if (givenNames.isNotEmpty()) "$givenNames $surname" else surname
                }
            }
            
            if (mrzLine2.isNotEmpty()) {
                val num = mrzLine2.take(9).replace("<", "").trim()
                if (passportRegex.matches(num)) {
                    extractedPassportNumber = num
                }
            }
            
            // Fallback heuristics
            if (extractedPassportNumber.isEmpty()) {
                for (i in lines.indices) {
                    val currentLine = lines[i].lowercase()
                    if (currentLine.contains("passport") || currentLine.contains("pass") || currentLine.contains("no.") || currentLine.contains("number")) {
                        val words = lines[i].split(" ", "/", "-") + (lines.getOrNull(i + 1)?.split(" ", "/", "-") ?: emptyList())
                        for (w in words) {
                            val cleanW = w.replace(":", "").replace(".", "").trim().uppercase()
                            if (passportRegex.matches(cleanW) && cleanW.length >= 8 && !cleanW.contains("PASSPORT") && !cleanW.contains("TRAVEL")) {
                                extractedPassportNumber = cleanW
                                break
                            }
                        }
                    }
                    if (extractedPassportNumber.isNotEmpty()) break
                }
            }
            
            if (extractedPassportNumber.isEmpty()) {
                for (line in lines) {
                    val words = line.split(" ", "/", "-")
                    for (w in words) {
                        val cleanW = w.replace(":", "").replace(".", "").trim().uppercase()
                        if (passportRegex.matches(cleanW) && cleanW.length >= 9 && cleanW.any { it.isDigit() } && cleanW.any { it.isLetter() }) {
                            extractedPassportNumber = cleanW
                            break
                        }
                    }
                    if (extractedPassportNumber.isNotEmpty()) break
                }
            }
            
            if (extractedName.isEmpty()) {
                for (i in lines.indices) {
                    val currentLine = lines[i].lowercase()
                    if (nameKeywords.any { currentLine.contains(it) }) {
                        val colonIndex = lines[i].indexOf(":")
                        if (colonIndex != -1 && colonIndex < lines[i].length - 2) {
                            extractedName = lines[i].substring(colonIndex + 1).trim()
                        } else {
                            val nextLine = lines.getOrNull(i + 1) ?: ""
                            if (nextLine.isNotEmpty() && !nameKeywords.any { nextLine.lowercase().contains(it) }) {
                                extractedName = nextLine
                            }
                        }
                    }
                    if (extractedName.isNotEmpty()) break
                }
            }
            
            if (extractedNationality.isEmpty()) {
                for (i in lines.indices) {
                    val currentLine = lines[i].lowercase()
                    if (nationalityKeywords.any { currentLine.contains(it) }) {
                        val colonIndex = lines[i].indexOf(":")
                        if (colonIndex != -1 && colonIndex < lines[i].length - 2) {
                            extractedNationality = lines[i].substring(colonIndex + 1).trim()
                        } else {
                            val nextLine = lines.getOrNull(i + 1) ?: ""
                            if (nextLine.isNotEmpty()) {
                                extractedNationality = nextLine
                            }
                        }
                    }
                    if (extractedNationality.isNotEmpty()) break
                }
            }
            
            extractedPassportNumber = extractedPassportNumber.trim().uppercase()
            extractedName = extractedName.trim().split(" ").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }
            extractedNationality = extractedNationality.trim().uppercase()
            
            onResult(extractedName, extractedPassportNumber, extractedNationality)
            onComplete()
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
            onComplete()
        }
}

@Composable
fun PassportScanViewfinder(
    isScanning: Boolean,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isScanning) { onScanClick() },
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(120.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = if (isScanning) "SCANNING..." else "ALIGN PASSPORT HERE\nแตะเพื่อเริ่มสแกน",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

fun loadBitmapFromImageSource(context: Context, imageSource: Any?): Bitmap? {
    if (imageSource == null) return null
    return try {
        when (imageSource) {
            is Bitmap -> imageSource
            is Uri -> {
                if (imageSource.scheme == "http" || imageSource.scheme == "https") {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(imageSource.toString()).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null) {
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } else null
                        } else null
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, imageSource)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, imageSource)
                    }
                }
            }
            is String -> {
                if (imageSource.startsWith("http")) {
                    loadBitmapFromImageSource(context, Uri.parse(imageSource))
                } else {
                    loadBitmapFromImageSource(context, Uri.parse(imageSource))
                }
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.e("loadBitmapFromImage", "Error loading bitmap", e)
        null
    }
}

fun generateContractPdf(
    context: Context,
    contract: RentalContract,
    scooter: Scooter,
    signaturePath: Path?,
    passportImage: Any? = null,
    licenseImage: Any? = null,
    onPdfGenerated: ((File) -> Unit)? = null
) {
    Toast.makeText(context, "Generating PDF...", Toast.LENGTH_SHORT).show()
    kotlin.concurrent.thread {
        try {
            val pdfDocument = PdfDocument()
            val scaleFactor = 3f
            val pageWidthPdf = 1785
            val pageHeightPdf = 2526
            val pageWidth = 595f

            // ---------------- PAGE 1 ----------------
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidthPdf, pageHeightPdf, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            val canvas1 = page1.canvas
            canvas1.scale(scaleFactor, scaleFactor)

            val paint = Paint().apply {
                color = AndroidColor.BLACK
                isAntiAlias = true
            }

            val bitmapPaint = Paint().apply {
                isFilterBitmap = true
                isDither = true
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = AndroidColor.LTGRAY
                strokeWidth = 1f
                isAntiAlias = true
            }

            // Header
            paint.textSize = 24f
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true
            canvas1.drawText("Cheap as Chips", (pageWidth / 2).toFloat(), 50f, paint)

            paint.textSize = 9f
            paint.isFakeBoldText = false
            canvas1.drawText("195/7 Soi Phrabaramee, Patong, Phuket 83150", (pageWidth / 2).toFloat(), 68f, paint)
            canvas1.drawText("+66 87 188 9047  |  info@motorbikerentalphuket.com", (pageWidth / 2).toFloat(), 82f, paint)

            // Underlined Title
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas1.drawText("Motorbike Rental Agreement / สัญญาเช่ารถจักรยานยนต์", (pageWidth / 2).toFloat(), 110f, paint)
            val titleWidth = paint.measureText("Motorbike Rental Agreement / สัญญาเช่ารถจักรยานยนต์")
            canvas1.drawLine(
                (pageWidth / 2) - titleWidth / 2, 114f,
                (pageWidth / 2) + titleWidth / 2, 114f,
                paint
            )

            // Draw field helper function
            fun drawField(canvas: AndroidCanvas, label: String, value: String, x: Float, y: Float, width: Float) {
                val labelPaint = Paint().apply {
                    color = AndroidColor.BLACK
                    textSize = 8.5f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val valuePaint = Paint().apply {
                    color = AndroidColor.DKGRAY
                    textSize = 8.5f
                    isAntiAlias = true
                }

                canvas.drawText(label, x, y, labelPaint)
                val labelWidth = labelPaint.measureText(label)
                val valueX = x + labelWidth + 4f
                canvas.drawText(value, valueX, y, valuePaint)
                canvas.drawLine(valueX, y + 2f, x + width, y + 2f, linePaint)
            }

            // Scooter Image and Plate Slot on the right (matching document.pdf page 1)
            val slotX1 = 370f
            val slotX2 = 465f
            val slotY = 125f
            val slotW = 80f
            val slotH = 55f

            val borderPaint = Paint().apply {
                color = AndroidColor.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            val scooterBitmap = scooter.images.firstOrNull()?.let { loadBitmapFromImageSource(context, it) }
            if (scooterBitmap != null) {
                val srcRect = android.graphics.Rect(0, 0, scooterBitmap.width, scooterBitmap.height)
                val destRect = RectF(slotX1, slotY, slotX1 + slotW, slotY + slotH)
                canvas1.drawBitmap(scooterBitmap, srcRect, destRect, bitmapPaint)
                canvas1.drawRect(destRect, borderPaint)
            } else {
                canvas1.drawRect(slotX1, slotY, slotX1 + slotW, slotY + slotH, borderPaint)
                val placeholderPaint = Paint().apply {
                    color = AndroidColor.GRAY
                    textSize = 8f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas1.drawText("Motor Photo", slotX1 + slotW / 2, slotY + slotH / 2 + 3f, placeholderPaint)
            }

            val scratchImageSource = contract.scratchImages.firstOrNull() ?: contract.licenseImage
            val scratchBitmap = scratchImageSource?.let { loadBitmapFromImageSource(context, it) }
            if (scratchBitmap != null) {
                val srcRect = android.graphics.Rect(0, 0, scratchBitmap.width, scratchBitmap.height)
                val destRect = RectF(slotX2, slotY, slotX2 + slotW, slotY + slotH)
                canvas1.drawBitmap(scratchBitmap, srcRect, destRect, bitmapPaint)
                canvas1.drawRect(destRect, borderPaint)
            } else {
                canvas1.drawRect(slotX2, slotY, slotX2 + slotW, slotY + slotH, borderPaint)
                val placeholderPaint = Paint().apply {
                    color = AndroidColor.GRAY
                    textSize = 8f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas1.drawText("Plate Photo", slotX2 + slotW / 2, slotY + slotH / 2 + 3f, placeholderPaint)
            }

            drawField(canvas1, "License Plate Number", scooter.plateNumber, 36f, 140f, 150f)
            drawField(canvas1, "Make/Model", "${scooter.brand} ${scooter.model}", 195f, 140f, 160f)

            drawField(canvas1, "Customer Name", contract.customerName, 36f, 195f, 230f)
            drawField(canvas1, "Nationality", contract.nationality.ifBlank { "Foreigner" }, 280f, 195f, 130f)
            drawField(canvas1, "Phone No.", contract.customerPhone, 420f, 195f, 139f)

            drawField(canvas1, "Passport Number", contract.passportNumber, 36f, 225f, 200f)
            drawField(canvas1, "Address/Hotel/Villa", contract.hotelAddress, 250f, 225f, 230f)
            drawField(canvas1, "Room", contract.roomNumber, 490f, 225f, 69f)

            val startDateOnly = contract.startDate.split(" ").firstOrNull() ?: ""
            val startTimeOnly = contract.startDate.split(" ").getOrNull(1) ?: ""

            val pdfParser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            var finishDateOnly = ""
            var finishTimeOnly = ""
            try {
                val start = pdfParser.parse(contract.startDate)
                if (start != null) {
                    val cal = Calendar.getInstance()
                    cal.time = start
                    cal.add(Calendar.DAY_OF_YEAR, contract.days)
                    val finishStr = pdfParser.format(cal.time)
                    finishDateOnly = finishStr.split(" ").firstOrNull() ?: ""
                    finishTimeOnly = finishStr.split(" ").getOrNull(1) ?: ""
                }
            } catch (e: Exception) {}

            drawField(canvas1, "Booking Start Date", startDateOnly, 36f, 255f, 130f)
            drawField(canvas1, "Start Time", startTimeOnly, 180f, 255f, 90f)
            drawField(canvas1, "Booking End Date", finishDateOnly, 280f, 255f, 150f)
            drawField(canvas1, "End Time", finishTimeOnly, 440f, 255f, 119f)

            val pricePerDay = if (contract.days > 0) contract.totalPrice / contract.days else scooter.priceDaily
            drawField(canvas1, "Price Per Day", "฿${String.format(Locale.US, "%.0f", pricePerDay)}", 36f, 285f, 110f)
            drawField(canvas1, "Total Days", "${contract.days}", 160f, 285f, 90f)
            drawField(canvas1, "Total Price", "฿${String.format(Locale.US, "%.0f", contract.totalPrice)}", 260f, 285f, 130f)
            drawField(canvas1, "Deposit", "฿${String.format(Locale.US, "%.0f", contract.depositAmount)}", 400f, 285f, 159f)

            drawField(canvas1, "Replacement Value", "฿${String.format(Locale.US, "%.0f", contract.replacementValue)}", 36f, 315f, 150f)
            drawField(canvas1, "Extra Cost per Hour", "฿${String.format(Locale.US, "%.0f", contract.extraCostPerHour)}", 200f, 315f, 160f)
            drawField(canvas1, "Fuel Cost per Bar", "฿${String.format(Locale.US, "%.0f", contract.fuelCostPerBar)}", 380f, 315f, 179f)

            drawField(canvas1, "Passport Hold", if (contract.passportHold) "Yes" else "No", 36f, 345f, 130f)
            drawField(canvas1, "No. of Helmets", "${contract.helmetCount}", 180f, 345f, 110f)
            drawField(canvas1, "Fuel Level", "${contract.fuelLevel}%", 300f, 345f, 120f)
            drawField(canvas1, "Delivery Cost", "฿${String.format(Locale.US, "%.0f", contract.deliveryCost)}", 430f, 345f, 129f)

            val labelPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas1.drawText("Terms & Conditions of the Rental Agreement / โปรดอ่านสัญญาเช่าให้ละเอียดเพื่อประโยชน์ของผู้เช่า", 36f, 375f, labelPaint)
            canvas1.drawLine(36f, 380f, (pageWidth - 36).toFloat(), 380f, paint)

            var currentY = 392f
            val termPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 7f
                isAntiAlias = true
            }

            fun drawWrappedText(canvas: AndroidCanvas, text: String, x: Float, y: Float, width: Float, paint: Paint): Float {
                val words = text.split(" ")
                var currentLine = ""
                var cy = y
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val testWidth = paint.measureText(testLine)
                    if (testWidth > width) {
                        canvas.drawText(currentLine, x, cy, paint)
                        cy += paint.textSize * 1.25f
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, x, cy, paint)
                    cy += paint.textSize * 1.25f
                }
                return cy
            }

            for (i in 0 until 16) {
                val termText = RENTAL_TERMS[i]
                currentY = drawWrappedText(canvas1, termText, 36f, currentY, 523f, termPaint) + 3f
            }

            paint.textSize = 10f
            paint.textAlign = Paint.Align.LEFT
            paint.isFakeBoldText = false
            canvas1.drawText("Renter Signature __________________________", 36f, 800f, paint)
            canvas1.drawText("Date  $startDateOnly", 350f, 800f, paint)
            canvas1.drawText("Time  $startTimeOnly", 480f, 800f, paint)

            if (signaturePath != null && !signaturePath.isEmpty) {
                val androidPath = android.graphics.Path(signaturePath.asAndroidPath())
                val sigBounds = android.graphics.RectF()
                androidPath.computeBounds(sigBounds, true)
                if (!sigBounds.isEmpty) {
                    val targetWidth = 100f
                    val targetHeight = 25f
                    val scaleX = targetWidth / sigBounds.width()
                    val scaleY = targetHeight / sigBounds.height()
                    val scale = minOf(scaleX, scaleY)

                    val matrix = android.graphics.Matrix()
                    matrix.postTranslate(-sigBounds.left, -sigBounds.top)
                    matrix.postScale(scale, scale)
                    val finalX = 116f + (120f - sigBounds.width() * scale) / 2f
                    val finalY = 798f - sigBounds.height() * scale - 2f
                    matrix.postTranslate(finalX, finalY)
                    androidPath.transform(matrix)

                    val sigPaint = Paint().apply {
                        color = AndroidColor.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                        isAntiAlias = true
                    }
                    canvas1.drawPath(androidPath, sigPaint)
                }
            }
            pdfDocument.finishPage(page1)

            // ---------------- PAGE 2 ----------------
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidthPdf, pageHeightPdf, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            val canvas2 = page2.canvas
            canvas2.scale(scaleFactor, scaleFactor)

            currentY = 40f
            canvas2.drawText("Additional Terms & Conditions / เงื่อนไขเพิ่มเติม", 36f, currentY, labelPaint)
            currentY += 8f
            canvas2.drawLine(36f, currentY, (pageWidth - 36).toFloat(), currentY, paint)
            currentY += 12f

            for (i in 16 until 19) {
                val termText = RENTAL_TERMS[i]
                currentY = drawWrappedText(canvas2, termText, 36f, currentY, 523f, termPaint) + 3f
            }

            currentY += 15f

            val docBoxW = 400f
            val docBoxH = 240f
            val docBoxX = (pageWidth - docBoxW) / 2f

            val passBmp = loadBitmapFromImageSource(context, passportImage ?: contract.passportImage)
            val passBoxY = currentY
            if (passBmp != null) {
                val srcRect = android.graphics.Rect(0, 0, passBmp.width, passBmp.height)
                val destRect = RectF(docBoxX, passBoxY, docBoxX + docBoxW, passBoxY + docBoxH)
                canvas2.drawBitmap(passBmp, srcRect, destRect, bitmapPaint)
                canvas2.drawRect(destRect, borderPaint)
            } else {
                canvas2.drawRect(docBoxX, passBoxY, docBoxX + docBoxW, passBoxY + docBoxH, borderPaint)
                val textPaint = Paint().apply {
                    color = AndroidColor.GRAY
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas2.drawText("Passport / ID Photo (Not Attached)", pageWidth / 2f, passBoxY + docBoxH / 2f + 4f, textPaint)
            }

            val licBmp = loadBitmapFromImageSource(context, licenseImage ?: contract.licenseImage)
            val licBoxY = passBoxY + docBoxH + 30f
            if (licBmp != null) {
                val srcRect = android.graphics.Rect(0, 0, licBmp.width, licBmp.height)
                val destRect = RectF(docBoxX, licBoxY, docBoxX + docBoxW, licBoxY + docBoxH)
                canvas2.drawBitmap(licBmp, srcRect, destRect, bitmapPaint)
                canvas2.drawRect(destRect, borderPaint)
            } else {
                canvas2.drawRect(docBoxX, licBoxY, docBoxX + docBoxW, licBoxY + docBoxH, borderPaint)
                val textPaint = Paint().apply {
                    color = AndroidColor.GRAY
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas2.drawText("Driver License Photo (Not Attached)", pageWidth / 2f, licBoxY + docBoxH / 2f + 4f, textPaint)
            }

            paint.textAlign = Paint.Align.LEFT
            canvas2.drawText("Renter Signature __________________________", 36f, 800f, paint)
            canvas2.drawText("Date  $startDateOnly", 350f, 800f, paint)
            canvas2.drawText("Time  $startTimeOnly", 480f, 800f, paint)

            if (signaturePath != null && !signaturePath.isEmpty) {
                val androidPath = android.graphics.Path(signaturePath.asAndroidPath())
                val sigBounds = android.graphics.RectF()
                androidPath.computeBounds(sigBounds, true)
                if (!sigBounds.isEmpty) {
                    val targetWidth = 100f
                    val targetHeight = 25f
                    val scaleX = targetWidth / sigBounds.width()
                    val scaleY = targetHeight / sigBounds.height()
                    val scale = minOf(scaleX, scaleY)

                    val matrix = android.graphics.Matrix()
                    matrix.postTranslate(-sigBounds.left, -sigBounds.top)
                    matrix.postScale(scale, scale)
                    val finalX = 116f + (120f - sigBounds.width() * scale) / 2f
                    val finalY = 798f - sigBounds.height() * scale - 2f
                    matrix.postTranslate(finalX, finalY)
                    androidPath.transform(matrix)

                    val sigPaint = Paint().apply {
                        color = AndroidColor.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                        isAntiAlias = true
                    }
                    canvas2.drawPath(androidPath, sigPaint)
                }
            }
            pdfDocument.finishPage(page2)

            val filename = "CheapAsChips_Contract_${contract.id}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var file = File(downloadsDir, filename)
            try {
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                FileOutputStream(file).use { fos ->
                    pdfDocument.writeTo(fos)
                }
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Saved to Downloads/${filename}", Toast.LENGTH_LONG).show()
                    openPdfFile(context, file)
                    onPdfGenerated?.invoke(file)
                }
            } catch (e: Exception) {
                Log.e("generateContractPdf", "Failed to write to public downloads, using fallback", e)
                try {
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    file = File(fallbackDir, filename)
                    FileOutputStream(file).use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Saved to app folder: ${filename}", Toast.LENGTH_LONG).show()
                        openPdfFile(context, file)
                        onPdfGenerated?.invoke(file)
                    }
                } catch (e2: Exception) {
                    Log.e("generateContractPdf", "All writes failed", e2)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Error saving contract PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                pdfDocument.close()
            }
        } catch (e: Exception) {
            Log.e("generateContractPdf", "Error generating PDF", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Error compiling PDF contract", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun openPdfFile(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Could not open PDF directly. File saved in Downloads.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun PdfViewerDialog(
    file: File,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val bitmaps = remember(file) {
        val list = mutableListOf<android.graphics.Bitmap>()
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val bitmap = android.graphics.Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                list.add(bitmap)
                page.close()
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rental Agreement PDF",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                if (bitmaps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Unable to render PDF preview.", color = Color.Gray)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        bitmaps.forEach { bitmap ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun sharePdfToCustomer(context: Context, file: File, contract: com.example.cheapaschip.data.RentalContract) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            
            val msgBody = "Hello ${contract.customerName},\n\nHere is your rental contract PDF from CheapAsChips.\n- WhatsApp: ${contract.customerWhatsApp}\n- Email: ${contract.customerEmail}"
            putExtra(android.content.Intent.EXTRA_TEXT, msgBody)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "CheapAsChips Rental Contract - ${contract.id}")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Contract PDF to WhatsApp/Email").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
