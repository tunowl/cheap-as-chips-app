package com.example.cheapaschip.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cheapaschip.data.RentalRepository
import com.example.cheapaschip.data.Scooter
import com.example.cheapaschip.data.ScooterStatus
import com.example.cheapaschip.data.ScooterModelDraft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Path
import com.example.cheapaschip.data.RentalContract
import com.example.cheapaschip.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Rates & Config", "Returned History", "Model Blueprints", "Fleet Manager")

    // --- TAB 0: Rates & Deposits States ---
    var standardDepStr by remember { mutableStateOf(RentalRepository.standardDepositAmount.toInt().toString()) }
    var maxiDepStr by remember { mutableStateOf(RentalRepository.maxiDepositAmount.toInt().toString()) }
    var isHighSeason by remember { mutableStateOf(RentalRepository.isHighSeasonMode) }
    var highSeasonPctStr by remember { mutableStateOf(RentalRepository.highSeasonPercentage.toInt().toString()) }
    var replacementValueStr by remember { mutableStateOf(RentalRepository.globalReplacementValue.toInt().toString()) }
    var extraCostPerHourStr by remember { mutableStateOf(RentalRepository.globalExtraCostPerHour.toInt().toString()) }


    // --- TAB 2: Model Blueprints States ---
    var draftStep by remember { mutableStateOf(0) } // 0 = list, 1 = specs, 2 = pricing, 3 = review
    var draftBrand by remember { mutableStateOf("") }
    var draftModel by remember { mutableStateOf("") }
    var draftCcStr by remember { mutableStateOf("") }
    var draftYearStr by remember { mutableStateOf("2024") }
    var draftHasPhoneCharger by remember { mutableStateOf(false) }
    var draftHasKeyless by remember { mutableStateOf(false) }
    var draftHasAbs by remember { mutableStateOf(false) }
    var draftDescription by remember { mutableStateOf("") }
    var draftPriceDailyStr by remember { mutableStateOf("") }
    var draftPrice3DayStr by remember { mutableStateOf("") }
    var draftPriceWeeklyStr by remember { mutableStateOf("") }
    var draftPrice2WeekStr by remember { mutableStateOf("") }
    var draftPriceMonthlyStr by remember { mutableStateOf("") }
    var draftDepositAmountStr by remember { mutableStateOf("") }
    var draftFuelBarsStr by remember { mutableStateOf("6") }
    var draftFuelCostPerBarStr by remember { mutableStateOf("100") }
    var editingDraftId by remember { mutableStateOf<String?>(null) }
    var showTemplatesMenu by remember { mutableStateOf(false) }
    var draftToDelete by remember { mutableStateOf<ScooterModelDraft?>(null) }

    // --- TAB 3: Fleet Manager States ---
    var scooterStep by remember { mutableStateOf(0) } // 0 = list, 1 = details, 2 = photos, 3 = review
    var selectedDraftId by remember { mutableStateOf<String?>(null) }
    var concretePlate by remember { mutableStateOf("") }
    val concretePhotos = remember { mutableStateListOf<Any?>(null, null, null, null, null, null, null, null) }
    var activePhotoIndex by remember { mutableStateOf(-1) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var scooterToDelete by remember { mutableStateOf<Scooter?>(null) }
    var isDeletingScooter by remember { mutableStateOf(false) }
    var editingScooter by remember { mutableStateOf<Scooter?>(null) }
    var fleetSearchQuery by remember { mutableStateOf("") }

    // --- TAB 1: Returned History States ---
    var returnedSearchQuery by remember { mutableStateOf("") }

    val editPhotos = remember { mutableStateListOf<Any?>(null, null, null, null, null, null, null, null) }

    val photoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activePhotoIndex in 0..7) {
            if (editingScooter != null) {
                editPhotos[activePhotoIndex] = uri
            } else {
                concretePhotos[activePhotoIndex] = uri
            }
        }
    }

    var tempPhotoUriStr by remember { mutableStateOf<String?>(null) }

    fun createTempImageUri(prefix: String): Uri {
        val tempFile = java.io.File.createTempFile(prefix, ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    val photoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uriStr = tempPhotoUriStr
        if (success && uriStr != null && activePhotoIndex in 0..7) {
            if (editingScooter != null) {
                editPhotos[activePhotoIndex] = Uri.parse(uriStr)
            } else {
                concretePhotos[activePhotoIndex] = Uri.parse(uriStr)
            }
        }
    }

    fun isWithinPastWeek(dateStr: String): Boolean {
        val formatters = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )
        for (formatter in formatters) {
            try {
                val date = formatter.parse(dateStr)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    val sevenDaysAgo = cal.time
                    return date.after(sevenDaysAgo) && date.before(Date(System.currentTimeMillis() + 86400000))
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        Text(
            text = "System Setup & Configuration",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Custom Pill-based Tab Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Render Content based on Tab Selection
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: RATES & DEPOSITS CONFIG
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // High Season Pricing Mode Settings
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "High Season Pricing Mode",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Increase all bike daily rates by percentage",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = isHighSeason,
                                        onCheckedChange = { checked ->
                                            isHighSeason = checked
                                            RentalRepository.isHighSeasonMode = checked
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = highSeasonPctStr,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() }
                                            highSeasonPctStr = digits
                                            val pct = digits.toDoubleOrNull() ?: 0.0
                                            RentalRepository.highSeasonPercentage = pct
                                        },
                                        label = { Text("Season Rate Increase (%)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        enabled = isHighSeason,
                                        suffix = { Text("%") }
                                    )

                                    // Preview calculation helper
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "Preview calculation:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        val pct = highSeasonPctStr.toDoubleOrNull() ?: 0.0
                                        val examplePrice = 500.0
                                        val sampleHighSeasonPrice = if (isHighSeason) {
                                            examplePrice * (1.0 + pct / 100.0)
                                        } else {
                                            examplePrice
                                        }
                                        Text(
                                            text = "500 ฿ → ${sampleHighSeasonPrice.toInt()} ฿",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHighSeason) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // Global Fee & Policy Settings
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Global Fee & Policy Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Configure global defaults for the rental agreements.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )

                                OutlinedTextField(
                                    value = replacementValueStr,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        replacementValueStr = digits
                                        RentalRepository.globalReplacementValue = digits.toDoubleOrNull() ?: 60000.0
                                    },
                                    label = { Text("Default Scooter Replacement Value (฿)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = extraCostPerHourStr,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        extraCostPerHourStr = digits
                                        RentalRepository.globalExtraCostPerHour = digits.toDoubleOrNull() ?: 50.0
                                    },
                                    label = { Text("Default Extra Cost Per Hour (฿)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: RETURNED HISTORY
                    val returnedContracts = remember(RentalRepository.contracts.toList(), returnedSearchQuery) {
                        RentalRepository.contracts.filter { contract ->
                            val isReturned = contract.isCompleted && (
                                contract.returnDate?.let { isWithinPastWeek(it) } ?: isWithinPastWeek(contract.startDate)
                            )
                            if (!isReturned) return@filter false
                            
                            if (returnedSearchQuery.trim().isNotEmpty()) {
                                val scooter = RentalRepository.scooters.find { it.id == contract.scooterId }
                                val plate = scooter?.plateNumber ?: ""
                                plate.contains(returnedSearchQuery.trim(), ignoreCase = true)
                            } else {
                                true
                            }
                        }.sortedByDescending { RentalRepository.getDueDate(it).time }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = returnedSearchQuery,
                            onValueChange = { returnedSearchQuery = it },
                            label = { Text("Search by Plate Number") },
                            placeholder = { Text("e.g. Phuket 88-888") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (returnedSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { returnedSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )

                        if (returnedContracts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (returnedSearchQuery.isNotEmpty()) {
                                            "No returned scooters match this plate number."
                                        } else {
                                            "No scooters returned within the past week."
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Completed rentals will appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                returnedContracts.forEach { contract ->
                                    val scooter = RentalRepository.scooters.find { it.id == contract.scooterId }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = scooter?.let { "${it.brand} ${it.model}" } ?: "Unknown Scooter",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Completed",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            Text(
                                                text = "Plate: ${scooter?.plateNumber ?: "Unknown"}",
                                                fontSize = 13.sp,
                                                color = Color.Gray
                                            )

                                            HorizontalDivider()
                                            Text(text = "Customer: ${contract.customerName}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Text(text = "Phone: ${contract.customerPhone}", fontSize = 13.sp)
                                            
                                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                            val dueDateStr = try {
                                                sdf.format(RentalRepository.getDueDate(contract))
                                            } catch (e: Exception) {
                                                "N/A"
                                            }
                                            Text(text = "Return Due Date: $dueDateStr", fontSize = 13.sp, color = Color.Gray)
                                            
                                            contract.returnDate?.let {
                                                Text(text = "Returned On: $it", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                            } ?: run {
                                                Text(text = "Start Date: ${contract.startDate} (${contract.days} days)", fontSize = 13.sp)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "Revenue: ${contract.totalPrice} ฿", fontWeight = FontWeight.Bold)
                                                Text(text = "Deposit Refunded: ${contract.depositAmount} ฿", color = Color.Gray, fontSize = 13.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    if (scooter != null) {
                                                        val sigPath = RentalRepository.signaturePaths[contract.id]
                                                        val actualPassport = contract.passportImage
                                                        val actualLicense = contract.licenseImage
                                                        generateContractPdf(context, contract, scooter, sigPath, actualPassport, actualLicense)
                                                    } else {
                                                        Toast.makeText(context, "Cannot generate PDF: Scooter details not found", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Download PDF",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: MODEL BLUEPRINT TEMPLATES
                    if (draftStep == 0) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { draftStep = 1 },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create New Blueprint Draft", fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "Active Blueprint Templates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (RentalRepository.modelDrafts.isEmpty()) {
                                    Text("No blueprint templates available.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    RentalRepository.modelDrafts.forEach { draft ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = "${draft.brand} ${draft.model}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                        Text(text = "${draft.cc}cc | Year: ${draft.year} | Deposit: ${draft.depositAmount.toInt()} ฿", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                    
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                                .clickable {
                                                                    editingDraftId = draft.id
                                                                    draftBrand = draft.brand
                                                                    draftModel = draft.model
                                                                    draftCcStr = draft.cc.toString()
                                                                    draftYearStr = draft.year.toString()
                                                                    draftHasPhoneCharger = draft.hasPhoneCharger
                                                                    draftHasKeyless = draft.hasKeyless
                                                                    draftHasAbs = draft.hasAbs
                                                                    draftDescription = draft.description
                                                                    draftPriceDailyStr = draft.priceDaily.toInt().toString()
                                                                    draftPrice3DayStr = draft.price3Day.toInt().toString()
                                                                    draftPriceWeeklyStr = draft.priceWeekly.toInt().toString()
                                                                    draftPrice2WeekStr = draft.price2Week.toInt().toString()
                                                                    draftPriceMonthlyStr = draft.priceMonthly.toInt().toString()
                                                                    draftDepositAmountStr = draft.depositAmount.toInt().toString()
                                                                    draftFuelBarsStr = draft.fuelBars.toString()
                                                                    draftFuelCostPerBarStr = draft.fuelCostPerBar.toInt().toString()
                                                                    draftStep = 1
                                                                }
                                                                .padding(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Draft Blueprint",
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                                .clickable { draftToDelete = draft }
                                                                .padding(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Draft Blueprint",
                                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                
                                                // Pricing grid
                                                Text(
                                                    text = "Prices: 1D: ${draft.priceDaily}฿ | 3D: ${draft.price3Day}฿ | 7D: ${draft.priceWeekly}฿ | 14D: ${draft.price2Week}฿ | 30D: ${draft.priceMonthly}฿",
                                                    fontSize = 12.sp,
                                                    color = Color.DarkGray
                                                )

                                                // Features Row
                                                val features = listOfNotNull(
                                                    if (draft.hasPhoneCharger) "Charger" else null,
                                                    if (draft.hasKeyless) "Keyless" else null,
                                                    if (draft.hasAbs) "ABS" else null
                                                )
                                                if (features.isNotEmpty()) {
                                                    Text(
                                                        text = "Features: " + features.joinToString(" • "),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Blueprint Creation Wizard
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header & Progress Indicator
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (draftStep) {
                                                1 -> if (editingDraftId != null) "Edit Specs & Details" else "Specs & Details"
                                                2 -> if (editingDraftId != null) "Edit Pricing Setup" else "Pricing Setup"
                                                else -> if (editingDraftId != null) "Verify & Update" else "Verify & Confirm"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Step $draftStep of 3",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    // Progress bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (step in 1..3) {
                                            val isDone = step < draftStep
                                            val isCurrent = step == draftStep
                                            val color = when {
                                                isCurrent -> MaterialTheme.colorScheme.primary
                                                isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                else -> Color.LightGray.copy(alpha = 0.5f)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(color)
                                            )
                                        }
                                    }
                                }
                            }

                            // Step screens
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (draftStep) {
                                    1 -> {
                                        // STEP 1: SPECIFICATIONS
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Specifications", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                
                                                Box {
                                                    TextButton(onClick = { showTemplatesMenu = true }) {
                                                        Text("Load Preset ▾")
                                                    }

                                                    DropdownMenu(
                                                        expanded = showTemplatesMenu,
                                                        onDismissRequest = { showTemplatesMenu = false }
                                                    ) {
                                                        RentalRepository.modelDrafts.forEach { draft ->
                                                            DropdownMenuItem(
                                                                text = { Text("${draft.brand} ${draft.model}") },
                                                                onClick = {
                                                                    draftBrand = draft.brand
                                                                    draftModel = draft.model
                                                                    draftCcStr = draft.cc.toString()
                                                                    draftYearStr = draft.year.toString()
                                                                    draftHasPhoneCharger = draft.hasPhoneCharger
                                                                    draftHasKeyless = draft.hasKeyless
                                                                    draftHasAbs = draft.hasAbs
                                                                    draftDescription = draft.description
                                                                    draftPriceDailyStr = draft.priceDaily.toInt().toString()
                                                                    draftPrice3DayStr = draft.price3Day.toInt().toString()
                                                                    draftPriceWeeklyStr = draft.priceWeekly.toInt().toString()
                                                                    draftPrice2WeekStr = draft.price2Week.toInt().toString()
                                                                    draftPriceMonthlyStr = draft.priceMonthly.toInt().toString()
                                                                    draftFuelBarsStr = draft.fuelBars.toString()
                                                                    draftFuelCostPerBarStr = draft.fuelCostPerBar.toInt().toString()
                                                                    showTemplatesMenu = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = draftBrand,
                                                onValueChange = { draftBrand = it },
                                                label = { Text("Brand (e.g. Honda, Yamaha)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            OutlinedTextField(
                                                value = draftModel,
                                                onValueChange = { draftModel = it },
                                                label = { Text("Model Name (e.g. Click 160cc)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = draftCcStr,
                                                    onValueChange = { draftCcStr = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Engine CC") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = draftYearStr,
                                                    onValueChange = { draftYearStr = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Model Year") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )
                                            }

                                            Text(text = "Specs & Features", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val featuresList = listOf(
                                                    Triple("Charger", draftHasPhoneCharger, { v: Boolean -> draftHasPhoneCharger = v }),
                                                    Triple("Keyless", draftHasKeyless, { v: Boolean -> draftHasKeyless = v }),
                                                    Triple("ABS", draftHasAbs, { v: Boolean -> draftHasAbs = v })
                                                )
                                                featuresList.forEach { (label, value, onValueChange) ->
                                                    FilterChip(
                                                        selected = value,
                                                        onClick = { onValueChange(!value) },
                                                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                                        shape = RoundedCornerShape(16.dp),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = draftDescription,
                                                onValueChange = { draftDescription = it },
                                                label = { Text("Short Description") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                    2 -> {
                                        // STEP 2: PRICING TIERS
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(text = "Pricing Tiers (฿)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                            OutlinedTextField(
                                                value = draftPriceDailyStr,
                                                onValueChange = { draftPriceDailyStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("Daily Rate (1-2 Days)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                suffix = { Text("฿") }
                                            )

                                            OutlinedTextField(
                                                value = draftPrice3DayStr,
                                                onValueChange = { draftPrice3DayStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("3-Day Daily Rate") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                suffix = { Text("฿") }
                                            )

                                            OutlinedTextField(
                                                value = draftPriceWeeklyStr,
                                                onValueChange = { draftPriceWeeklyStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("Weekly Daily Rate") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                suffix = { Text("฿") }
                                            )

                                            OutlinedTextField(
                                                value = draftPrice2WeekStr,
                                                onValueChange = { draftPrice2WeekStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("2-Week Daily Rate") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                suffix = { Text("฿") }
                                            )

                                            OutlinedTextField(
                                                value = draftPriceMonthlyStr,
                                                onValueChange = { draftPriceMonthlyStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("Monthly Daily Rate") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                suffix = { Text("฿") }
                                            )

                                            OutlinedTextField(
                                                value = draftDepositAmountStr,
                                                onValueChange = { draftDepositAmountStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("Cash Deposit Amount") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                suffix = { Text("฿") }
                                            )

                                            OutlinedTextField(
                                                value = draftFuelBarsStr,
                                                onValueChange = { draftFuelBarsStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("Number of Fuel Bars") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            OutlinedTextField(
                                                value = draftFuelCostPerBarStr,
                                                onValueChange = { draftFuelCostPerBarStr = it.filter { c -> c.isDigit() } },
                                                label = { Text("Fuel Cost Per Bar") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                suffix = { Text("฿") }
                                            )
                                        }
                                    }
                                    3 -> {
                                        // STEP 3: REVIEW & CONFIRM
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(text = "Verify Blueprint Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Text(
                                                        text = "$draftBrand $draftModel",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        Text("CC: ${draftCcStr}cc", fontWeight = FontWeight.Medium)
                                                        Text("Year: $draftYearStr", fontWeight = FontWeight.Medium)
                                                    }

                                                    if (draftDescription.isNotEmpty()) {
                                                        Text(text = "Description: $draftDescription", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                                    }

                                                    val features = listOfNotNull(
                                                        if (draftHasPhoneCharger) "Charger" else null,
                                                        if (draftHasKeyless) "Keyless" else null,
                                                        if (draftHasAbs) "ABS" else null
                                                    )
                                                    if (features.isNotEmpty()) {
                                                        Text(text = "Features: " + features.joinToString(" • "), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    
                                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                    Text(text = "Pricing Tiers (THB/Day):", fontWeight = FontWeight.Bold)
                                                    
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Daily (1-2 Days)")
                                                            Text("${draftPriceDailyStr} ฿", fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("3-Day Rate")
                                                            Text("${draftPrice3DayStr} ฿", fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Weekly Rate")
                                                            Text("${draftPriceWeeklyStr} ฿", fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("2-Week Rate")
                                                            Text("${draftPrice2WeekStr} ฿", fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Monthly Rate")
                                                            Text("${draftPriceMonthlyStr} ฿", fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Deposit Amount")
                                                            Text("${draftDepositAmountStr} ฿", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Number of Fuel Bars")
                                                            Text(draftFuelBarsStr, fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Fuel Cost Per Bar")
                                                            Text("${draftFuelCostPerBarStr} ฿", fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Navigation controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (draftStep > 1) {
                                    OutlinedButton(
                                        onClick = { draftStep -= 1 },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Back")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            // Cancel & reset
                                            draftStep = 0
                                            draftBrand = ""
                                            draftModel = ""
                                            draftCcStr = ""
                                            draftYearStr = "2024"
                                            draftHasPhoneCharger = false
                                            draftHasKeyless = false
                                            draftHasAbs = false
                                            draftDescription = ""
                                            draftPriceDailyStr = ""
                                            draftPrice3DayStr = ""
                                            draftPriceWeeklyStr = ""
                                            draftPrice2WeekStr = ""
                                            draftPriceMonthlyStr = ""
                                            draftDepositAmountStr = ""
                                            draftFuelBarsStr = "6"
                                            draftFuelCostPerBarStr = "100"
                                            editingDraftId = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                }

                                Button(
                                    onClick = {
                                        when (draftStep) {
                                            1 -> {
                                                val ccVal = draftCcStr.toIntOrNull()
                                                val yearVal = draftYearStr.toIntOrNull()
                                                if (draftBrand.trim().isEmpty() || draftModel.trim().isEmpty()) {
                                                    Toast.makeText(context, "Please enter brand and model name", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (ccVal == null || ccVal <= 0) {
                                                    Toast.makeText(context, "Please enter CC", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (yearVal == null || yearVal < 1900 || yearVal > 2100) {
                                                    Toast.makeText(context, "Please enter model year", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                draftStep = 2
                                            }
                                            2 -> {
                                                val dailyVal = draftPriceDailyStr.toDoubleOrNull()
                                                val threeDayVal = draftPrice3DayStr.toDoubleOrNull()
                                                val weeklyVal = draftPriceWeeklyStr.toDoubleOrNull()
                                                val twoWeekVal = draftPrice2WeekStr.toDoubleOrNull()
                                                val monthlyVal = draftPriceMonthlyStr.toDoubleOrNull()
                                                val depositVal = draftDepositAmountStr.toDoubleOrNull()
                                                val fuelBarsVal = draftFuelBarsStr.toIntOrNull()
                                                val fuelCostVal = draftFuelCostPerBarStr.toDoubleOrNull()
                                                if (dailyVal == null || dailyVal <= 0 || threeDayVal == null || threeDayVal <= 0 ||
                                                    weeklyVal == null || weeklyVal <= 0 || twoWeekVal == null || twoWeekVal <= 0 ||
                                                    monthlyVal == null || monthlyVal <= 0 || depositVal == null || depositVal < 0
                                                ) {
                                                    Toast.makeText(context, "Please fill in all pricing tiers and deposit", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (fuelBarsVal == null || fuelBarsVal <= 0 || fuelCostVal == null || fuelCostVal < 0) {
                                                    Toast.makeText(context, "Please enter valid fuel settings", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                draftStep = 3
                                            }
                                            3 -> {
                                                val depositVal = draftDepositAmountStr.toDoubleOrNull() ?: 0.0
                                                val fuelBarsVal = draftFuelBarsStr.toIntOrNull() ?: 6
                                                val fuelCostVal = draftFuelCostPerBarStr.toDoubleOrNull() ?: 100.0
                                                if (editingDraftId != null) {
                                                    val updatedDraft = ScooterModelDraft(
                                                        id = editingDraftId!!,
                                                        brand = draftBrand.trim(),
                                                        model = draftModel.trim(),
                                                        cc = draftCcStr.toInt(),
                                                        year = draftYearStr.toInt(),
                                                        hasPhoneCharger = draftHasPhoneCharger,
                                                        hasKeyless = draftHasKeyless,
                                                        hasAbs = draftHasAbs,
                                                        priceDaily = draftPriceDailyStr.toDouble(),
                                                        price3Day = draftPrice3DayStr.toDouble(),
                                                        priceWeekly = draftPriceWeeklyStr.toDouble(),
                                                        price2Week = draftPrice2WeekStr.toDouble(),
                                                        priceMonthly = draftPriceMonthlyStr.toDouble(),
                                                        depositAmount = depositVal,
                                                        description = draftDescription.trim(),
                                                        fuelBars = fuelBarsVal,
                                                        fuelCostPerBar = fuelCostVal
                                                    )
                                                    RentalRepository.updateModelDraft(updatedDraft)
                                                    Toast.makeText(context, "Updated blueprint: ${updatedDraft.brand} ${updatedDraft.model}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val newDraft = ScooterModelDraft(
                                                        id = UUID.randomUUID().toString().take(6),
                                                        brand = draftBrand.trim(),
                                                        model = draftModel.trim(),
                                                        cc = draftCcStr.toInt(),
                                                        year = draftYearStr.toInt(),
                                                        hasPhoneCharger = draftHasPhoneCharger,
                                                        hasKeyless = draftHasKeyless,
                                                        hasAbs = draftHasAbs,
                                                        priceDaily = draftPriceDailyStr.toDouble(),
                                                        price3Day = draftPrice3DayStr.toDouble(),
                                                        priceWeekly = draftPriceWeeklyStr.toDouble(),
                                                        price2Week = draftPrice2WeekStr.toDouble(),
                                                        priceMonthly = draftPriceMonthlyStr.toDouble(),
                                                        depositAmount = depositVal,
                                                        description = draftDescription.trim(),
                                                        fuelBars = fuelBarsVal,
                                                        fuelCostPerBar = fuelCostVal
                                                    )
                                                    RentalRepository.addModelDraft(newDraft)
                                                    Toast.makeText(context, "Saved blueprint: ${newDraft.brand} ${newDraft.model}", Toast.LENGTH_SHORT).show()
                                                }
                                                
                                                // Reset and return
                                                draftStep = 0
                                                draftBrand = ""
                                                draftModel = ""
                                                draftCcStr = ""
                                                draftYearStr = "2024"
                                                draftHasPhoneCharger = false
                                                draftHasKeyless = false
                                                draftHasAbs = false
                                                draftDescription = ""
                                                draftPriceDailyStr = ""
                                                draftPrice3DayStr = ""
                                                draftPriceWeeklyStr = ""
                                                draftPrice2WeekStr = ""
                                                draftPriceMonthlyStr = ""
                                                draftDepositAmountStr = ""
                                                draftFuelBarsStr = "6"
                                                draftFuelCostPerBarStr = "100"
                                                editingDraftId = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (draftStep == 3) "Save Blueprint" else "Next")
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: FLEET VEHICLES MANAGER
                    if (scooterStep == 0) {
                        if (editingScooter != null) {
                            val scooter = editingScooter!!
                            var editPlate by remember(scooter) { mutableStateOf(scooter.plateNumber) }
                            var editCc by remember(scooter) { mutableStateOf(scooter.cc.toString()) }
                            var editYear by remember(scooter) { mutableStateOf(scooter.year.toString()) }
                            var editPriceDaily by remember(scooter) { mutableStateOf(scooter.priceDaily.toInt().toString()) }
                            var editPrice3Day by remember(scooter) { mutableStateOf(scooter.price3Day.toInt().toString()) }
                            var editPriceWeekly by remember(scooter) { mutableStateOf(scooter.priceWeekly.toInt().toString()) }
                            var editPrice2Week by remember(scooter) { mutableStateOf(scooter.price2Week.toInt().toString()) }
                            var editPriceMonthly by remember(scooter) { mutableStateOf(scooter.priceMonthly.toInt().toString()) }
                            var editDeposit by remember(scooter) { mutableStateOf(scooter.depositAmount.toInt().toString()) }
                            var editCharger by remember(scooter) { mutableStateOf(scooter.hasPhoneCharger) }
                            var editKeyless by remember(scooter) { mutableStateOf(scooter.hasKeyless) }
                            var editAbs by remember(scooter) { mutableStateOf(scooter.hasAbs) }
                            var editDesc by remember(scooter) { mutableStateOf(scooter.description) }
                            var editFuelBars by remember(scooter) { mutableStateOf(scooter.fuelBars.toString()) }
                            var editFuelCostPerBar by remember(scooter) { mutableStateOf(scooter.fuelCostPerBar.toInt().toString()) }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Edit Fleet Scooter",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${scooter.brand} ${scooter.model}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Card 1: Vehicle Details
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Vehicle Specs",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )
                                            
                                            OutlinedTextField(
                                                value = editPlate,
                                                onValueChange = { editPlate = it },
                                                label = { Text("Plate Number") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editCc,
                                                    onValueChange = { editCc = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Engine CC") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = editYear,
                                                    onValueChange = { editYear = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Year") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }

                                    // Card 2: Features
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Specs & Features",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val features = listOf(
                                                    Triple("Charger", editCharger, { v: Boolean -> editCharger = v }),
                                                    Triple("Keyless", editKeyless, { v: Boolean -> editKeyless = v }),
                                                    Triple("ABS", editAbs, { v: Boolean -> editAbs = v })
                                                )
                                                features.forEach { (label, valState, setVal) ->
                                                    FilterChip(
                                                        selected = valState,
                                                        onClick = { setVal(!valState) },
                                                        label = { 
                                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(16.dp),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Card 3: Pricing & Deposit
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Pricing & Deposit Setup",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editPriceDaily,
                                                    onValueChange = { editPriceDaily = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Daily (1-2d)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true,
                                                    suffix = { Text("฿") }
                                                )

                                                OutlinedTextField(
                                                    value = editPrice3Day,
                                                    onValueChange = { editPrice3Day = it.filter { c -> c.isDigit() } },
                                                    label = { Text("3-Day Rate") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true,
                                                    suffix = { Text("฿") }
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editPriceWeekly,
                                                    onValueChange = { editPriceWeekly = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Weekly Rate") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true,
                                                    suffix = { Text("฿") }
                                                )

                                                OutlinedTextField(
                                                    value = editPrice2Week,
                                                    onValueChange = { editPrice2Week = it.filter { c -> c.isDigit() } },
                                                    label = { Text("2-Week Rate") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true,
                                                    suffix = { Text("฿") }
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editPriceMonthly,
                                                    onValueChange = { editPriceMonthly = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Monthly Rate") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true,
                                                    suffix = { Text("฿") }
                                                )

                                                OutlinedTextField(
                                                    value = editDeposit,
                                                    onValueChange = { editDeposit = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Cash Deposit") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true,
                                                    suffix = { Text("฿") }
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editFuelBars,
                                                    onValueChange = { editFuelBars = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Number of Fuel Bars") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = editFuelCostPerBar,
                                                    onValueChange = { editFuelCostPerBar = it.filter { c -> c.isDigit() } },
                                                    label = { Text("Fuel Cost Per Bar") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true,
                                                    suffix = { Text("฿") }
                                                )
                                            }
                                        }
                                    }

                                    // Card 4: Description
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Description & Notes",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )
                                            
                                            OutlinedTextField(
                                                value = editDesc,
                                                onValueChange = { editDesc = it },
                                                label = { Text("General Notes") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                maxLines = 4
                                            )
                                        }
                                    }

                                    // Card 5: Scooter Photos
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Scooter Photos (at least 1 required)",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )
                                            
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                for (row in 0..1) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        for (col in 0..3) {
                                                            val index = row * 4 + col
                                                            val photo = editPhotos.getOrNull(index)
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .aspectRatio(1f)
                                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color.White)
                                                                    .clickable {
                                                                        activePhotoIndex = index
                                                                        showPhotoSourceDialog = true
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (photo != null) {
                                                                    when (photo) {
                                                                        is Uri -> {
                                                                            val bitmap = rememberUriBitmap(photo)
                                                                            if (bitmap != null) {
                                                                                Image(
                                                                                    bitmap = bitmap,
                                                                                    contentDescription = null,
                                                                                    modifier = Modifier.fillMaxSize(),
                                                                                    contentScale = ContentScale.Crop
                                                                                )
                                                                            }
                                                                        }
                                                                        is String -> {
                                                                            val uri = remember(photo) { Uri.parse(photo) }
                                                                            val bitmap = rememberUriBitmap(uri)
                                                                            if (bitmap != null) {
                                                                                Image(
                                                                                    bitmap = bitmap,
                                                                                    contentDescription = null,
                                                                                    modifier = Modifier.fillMaxSize(),
                                                                                    contentScale = ContentScale.Crop
                                                                                )
                                                                            }
                                                                        }
                                                                        is android.graphics.Bitmap -> {
                                                                            Image(
                                                                                bitmap = photo.asImageBitmap(),
                                                                                contentDescription = null,
                                                                                modifier = Modifier.fillMaxSize(),
                                                                                contentScale = ContentScale.Crop
                                                                            )
                                                                        }
                                                                    }
                                                                    
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .align(Alignment.TopEnd)
                                                                            .padding(2.dp)
                                                                            .size(18.dp)
                                                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                                            .clickable {
                                                                                editPhotos[index] = null
                                                                            },
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.Delete,
                                                                            contentDescription = "Delete Photo",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(10.dp)
                                                                        )
                                                                    }
                                                                } else {
                                                                    Column(
                                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                                        verticalArrangement = Arrangement.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.Add,
                                                                            contentDescription = "Add Photo",
                                                                            tint = Color.Gray,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                        Text(
                                                                            text = "${index + 1}",
                                                                            fontSize = 9.sp,
                                                                            color = Color.Gray
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { editingScooter = null },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                        ) {
                                            Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val ccVal = editCc.toIntOrNull()
                                                val yearVal = editYear.toIntOrNull()
                                                val dailyVal = editPriceDaily.toDoubleOrNull()
                                                val threeDayVal = editPrice3Day.toDoubleOrNull()
                                                val weeklyVal = editPriceWeekly.toDoubleOrNull()
                                                val twoWeekVal = editPrice2Week.toDoubleOrNull()
                                                val monthlyVal = editPriceMonthly.toDoubleOrNull()
                                                val depositVal = editDeposit.toDoubleOrNull()
                                                val fuelBarsVal = editFuelBars.toIntOrNull()
                                                val fuelCostVal = editFuelCostPerBar.toDoubleOrNull()

                                                if (editPlate.trim().isEmpty()) {
                                                    Toast.makeText(context, "Please enter plate number", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (ccVal == null || ccVal <= 0 || yearVal == null || yearVal < 1900 || yearVal > 2100) {
                                                    Toast.makeText(context, "Please enter valid cc and year", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (dailyVal == null || dailyVal <= 0 || threeDayVal == null || threeDayVal <= 0 ||
                                                    weeklyVal == null || weeklyVal <= 0 || twoWeekVal == null || twoWeekVal <= 0 ||
                                                    monthlyVal == null || monthlyVal <= 0 || depositVal == null || depositVal < 0
                                                ) {
                                                    Toast.makeText(context, "Please fill in all pricing tiers and deposit", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (fuelBarsVal == null || fuelBarsVal <= 0 || fuelCostVal == null || fuelCostVal < 0) {
                                                    Toast.makeText(context, "Please enter valid fuel settings", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val attachedCount = editPhotos.count { it != null }
                                                if (attachedCount < 1) {
                                                    Toast.makeText(context, "Please attach at least 1 photo of the scooter", Toast.LENGTH_LONG).show()
                                                    return@Button
                                                }

                                                val updatedScooter = scooter.copy(
                                                    plateNumber = editPlate.trim(),
                                                    cc = ccVal,
                                                    year = yearVal,
                                                    pricePerDay = dailyVal,
                                                    priceDaily = dailyVal,
                                                    price3Day = threeDayVal,
                                                    priceWeekly = weeklyVal,
                                                    price2Week = twoWeekVal,
                                                    priceMonthly = monthlyVal,
                                                    depositAmount = depositVal,
                                                    hasPhoneCharger = editCharger,
                                                    hasKeyless = editKeyless,
                                                    hasAbs = editAbs,
                                                    description = editDesc.trim(),
                                                    fuelBars = fuelBarsVal,
                                                    fuelCostPerBar = fuelCostVal,
                                                    images = editPhotos.filterNotNull()
                                                )

                                                RentalRepository.updateScooter(updatedScooter)
                                                Toast.makeText(context, "Scooter ${updatedScooter.plateNumber} updated successfully!", Toast.LENGTH_SHORT).show()
                                                editingScooter = null
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Save Changes", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { scooterStep = 1 },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Register Scooter to Fleet", fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "Active Fleet Vehicles",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = fleetSearchQuery,
                                    onValueChange = { fleetSearchQuery = it },
                                    label = { Text("Search by Plate Number") },
                                    placeholder = { Text("e.g. Phuket 12-345") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    trailingIcon = {
                                        if (fleetSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { fleetSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    }
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val filteredScooters = remember(RentalRepository.scooters.toList(), fleetSearchQuery) {
                                        RentalRepository.scooters.filter { scooter ->
                                            if (fleetSearchQuery.trim().isNotEmpty()) {
                                                scooter.plateNumber.contains(fleetSearchQuery.trim(), ignoreCase = true)
                                            } else {
                                                true
                                            }
                                        }
                                    }

                                    if (filteredScooters.isEmpty()) {
                                        Text(
                                            text = if (fleetSearchQuery.isNotEmpty()) "No scooters match this plate number." else "No scooters registered in the fleet.",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        filteredScooters.forEach { scooter ->
                                            val isRented = scooter.status == ScooterStatus.RENTED
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isRented) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(
                                                                text = scooter.plateNumber,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Card(
                                                                shape = RoundedCornerShape(8.dp),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = if (isRented) BrandBlueLight else BrandGreenLight,
                                                                    contentColor = if (isRented) BrandBlueDark else BrandGreenDark
                                                                )
                                                            ) {
                                                                Text(
                                                                    text = if (isRented) "RENTED" else "AVAILABLE",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(text = "CC: ${scooter.cc} | Year: ${scooter.year} | Daily Rate: ${scooter.priceDaily} ฿", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                    
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                                .clickable {
                                                                    editingScooter = scooter
                                                                    editPhotos.clear()
                                                                    editPhotos.addAll(scooter.images)
                                                                    while (editPhotos.size < 8) {
                                                                        editPhotos.add(null)
                                                                    }
                                                                }
                                                                .padding(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Scooter",
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                                .clickable { scooterToDelete = scooter }
                                                                .padding(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Scooter",
                                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                                modifier = Modifier.size(18.dp)
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
                    } else {
                        // Fleet Registration Wizard
                        val selectedDraft = RentalRepository.modelDrafts.find { it.id == selectedDraftId }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header & Progress Indicator
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (scooterStep) {
                                                1 -> "Select Blueprint & Plate"
                                                2 -> "Photo Inspection"
                                                else -> "Review & Submit"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Step $scooterStep of 3",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    // Progress bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (step in 1..3) {
                                            val isDone = step < scooterStep
                                            val isCurrent = step == scooterStep
                                            val color = when {
                                                isCurrent -> MaterialTheme.colorScheme.primary
                                                isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                else -> Color.LightGray.copy(alpha = 0.5f)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(color)
                                            )
                                        }
                                    }
                                }
                            }

                            // Step screens
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (scooterStep) {
                                    1 -> {
                                        // STEP 1: CHOOSE BLUEPRINT & PLATE
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            var dropdownExpanded by remember { mutableStateOf(false) }
                                            
                                            Text(text = "Choose Blueprint", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedButton(
                                                    onClick = { dropdownExpanded = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = selectedDraft?.let { "${it.brand} ${it.model} (${it.year})" } ?: "Select Blueprint...",
                                                            color = if (selectedDraft != null) Color.Black else Color.Gray
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = dropdownExpanded,
                                                    onDismissRequest = { dropdownExpanded = false },
                                                    modifier = Modifier.fillMaxWidth(0.9f)
                                                ) {
                                                    RentalRepository.modelDrafts.forEach { draft ->
                                                        DropdownMenuItem(
                                                            text = { Text("${draft.brand} ${draft.model} (${draft.year})") },
                                                            onClick = {
                                                                selectedDraftId = draft.id
                                                                dropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            if (selectedDraft != null) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(text = "Specs: ${selectedDraft.cc}cc | Year: ${selectedDraft.year}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        val features = listOfNotNull(
                                                            if (selectedDraft.hasPhoneCharger) "Charger" else null,
                                                            if (selectedDraft.hasKeyless) "Keyless" else null,
                                                            if (selectedDraft.hasAbs) "ABS" else null
                                                        )
                                                        Text(text = "Features: " + (if (features.isEmpty()) "None" else features.joinToString(", ")), fontSize = 12.sp, color = Color.DarkGray)
                                                        Text(text = "Daily Rate: ${selectedDraft.priceDaily} ฿", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = concretePlate,
                                                onValueChange = { concretePlate = it },
                                                label = { Text("Scooter Plate Number") },
                                                placeholder = { Text("e.g. Phuket 12-345") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )
                                        }
                                    }
                                    2 -> {
                                        // STEP 2: PHOTO INSPECTION (8 PHOTOS)
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            selectedDraft?.let { draft ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "${draft.brand} ${draft.model}",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 15.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = "Plate: $concretePlate",
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 13.sp,
                                                                color = Color.DarkGray
                                                            )
                                                        }
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                text = "${draft.cc}cc | ${draft.year}",
                                                                fontSize = 12.sp,
                                                                color = Color.Gray
                                                            )
                                                            Text(
                                                                text = "${draft.priceDaily} ฿ / day",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Text(text = "Scooter Photos (at least 1 required)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
                                            
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                for (row in 0..1) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        for (col in 0..3) {
                                                            val index = row * 4 + col
                                                            val photo = concretePhotos[index]
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .aspectRatio(1f)
                                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color.White)
                                                                    .clickable {
                                                                        activePhotoIndex = index
                                                                        showPhotoSourceDialog = true
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (photo != null) {
                                                                    when (photo) {
                                                                        is Uri -> {
                                                                            val bitmap = rememberUriBitmap(photo)
                                                                            if (bitmap != null) {
                                                                                Image(
                                                                                    bitmap = bitmap,
                                                                                    contentDescription = null,
                                                                                    modifier = Modifier.fillMaxSize(),
                                                                                    contentScale = ContentScale.Crop
                                                                                )
                                                                            }
                                                                        }
                                                                        is String -> {
																    val uri = remember(photo) { Uri.parse(photo) }
																    val bitmap = rememberUriBitmap(uri)
																    if (bitmap != null) {
																        Image(
																            bitmap = bitmap,
																            contentDescription = null,
																            modifier = Modifier.fillMaxSize(),
																            contentScale = ContentScale.Crop
																        )
																    }
                                                                        }

                                                                        is android.graphics.Bitmap -> {
                                                                            Image(
                                                                                bitmap = (photo as android.graphics.Bitmap).asImageBitmap(),
                                                                                contentDescription = null,
                                                                                modifier = Modifier.fillMaxSize(),
                                                                                contentScale = ContentScale.Crop
                                                                            )
                                                                        }
                                                                    }
                                                                    
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .align(Alignment.TopEnd)
                                                                            .padding(2.dp)
                                                                            .size(18.dp)
                                                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                                            .clickable {
                                                                                concretePhotos[index] = null
                                                                            },
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.Delete,
                                                                            contentDescription = "Delete Photo",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(10.dp)
                                                                        )
                                                                    }
                                                                } else {
                                                                    Column(
                                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                                        verticalArrangement = Arrangement.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.Add,
                                                                            contentDescription = "Add Photo",
                                                                            tint = Color.Gray,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                        Text(
                                                                            text = "${index + 1}",
                                                                            fontSize = 9.sp,
                                                                            color = Color.Gray
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
                                    3 -> {
                                        // STEP 3: REVIEW & REGISTER
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(text = "Verify Scooter Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    selectedDraft?.let { draft ->
                                                        Text(
                                                            text = "${draft.brand} ${draft.model}",
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(text = "Plate: $concretePlate", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        Text(text = "CC: ${draft.cc}cc | Year: ${draft.year}", fontSize = 13.sp, color = Color.Gray)
                                                        
                                                        val features = listOfNotNull(
                                                            if (draft.hasPhoneCharger) "Charger" else null,
                                                            if (draft.hasKeyless) "Keyless" else null,
                                                            if (draft.hasAbs) "ABS" else null
                                                        )
                                                        if (features.isNotEmpty()) {
                                                            Text(text = "Features: " + features.joinToString(" • "), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                    
                                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                    Text(text = "Photos Preview:", fontWeight = FontWeight.Bold)
                                                    
                                                    val uploadedPhotos = concretePhotos.filterNotNull()
                                                    if (uploadedPhotos.isNotEmpty()) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .horizontalScroll(rememberScrollState()),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            uploadedPhotos.forEach { photo ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(80.dp)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                                ) {
                                                                    when (photo) {
                                                                        is Uri -> {
                                                                            val bitmap = rememberUriBitmap(photo)
                                                                            if (bitmap != null) {
                                                                                Image(
                                                                                    bitmap = bitmap,
                                                                                    contentDescription = null,
                                                                                    modifier = Modifier.fillMaxSize(),
                                                                                    contentScale = ContentScale.Crop
                                                                                )
                                                                            }
                                                                        }
                                                                        is String -> {
																    val uri = remember(photo) { Uri.parse(photo) }
																    val bitmap = rememberUriBitmap(uri)
																    if (bitmap != null) {
																        Image(
																            bitmap = bitmap,
																            contentDescription = null,
																            modifier = Modifier.fillMaxSize(),
																            contentScale = ContentScale.Crop
																        )
																    }
                                                                        }

                                                                        is android.graphics.Bitmap -> {
                                                                            Image(
                                                                                bitmap = photo.asImageBitmap(),
                                                                                contentDescription = null,
                                                                                modifier = Modifier.fillMaxSize(),
                                                                                contentScale = ContentScale.Crop
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Text("No photos attached", color = Color.Gray, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Navigation controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (scooterStep > 1) {
                                    OutlinedButton(
                                        onClick = { scooterStep -= 1 },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Back")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            // Cancel & reset
                                            scooterStep = 0
                                            selectedDraftId = null
                                            concretePlate = ""
                                            for (i in 0..7) {
                                                concretePhotos[i] = null
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                }

                                Button(
                                    onClick = {
                                        when (scooterStep) {
                                            1 -> {
                                                if (selectedDraftId == null) {
                                                    Toast.makeText(context, "Please select a blueprint template", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (concretePlate.trim().isEmpty()) {
                                                    Toast.makeText(context, "Please enter plate number", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                scooterStep = 2
                                            }
                                            2 -> {
                                                val attachedCount = concretePhotos.count { it != null }
                                                if (attachedCount < 1) {
                                                    Toast.makeText(context, "Please attach at least 1 photo of the scooter", Toast.LENGTH_LONG).show()
                                                    return@Button
                                                }
                                                scooterStep = 3
                                            }
                                            3 -> {
                                                if (selectedDraft == null) return@Button
                                                val newId = UUID.randomUUID().toString().take(6)
                                                val newScooter = Scooter(
                                                    id = newId,
                                                    brand = selectedDraft.brand,
                                                    model = selectedDraft.model,
                                                    plateNumber = concretePlate.trim(),
                                                    pricePerDay = selectedDraft.priceDaily,
                                                    status = ScooterStatus.AVAILABLE,
                                                    description = if (selectedDraft.description.isEmpty()) "Registered using model draft blueprint." else selectedDraft.description,
                                                    cc = selectedDraft.cc,
                                                    year = selectedDraft.year,
                                                    hasPhoneCharger = selectedDraft.hasPhoneCharger,
                                                    hasKeyless = selectedDraft.hasKeyless,
                                                    hasAbs = selectedDraft.hasAbs,
                                                    priceDaily = selectedDraft.priceDaily,
                                                    price3Day = selectedDraft.price3Day,
                                                    priceWeekly = selectedDraft.priceWeekly,
                                                    price2Week = selectedDraft.price2Week,
                                                    priceMonthly = selectedDraft.priceMonthly,
                                                    depositAmount = selectedDraft.depositAmount,
                                                    images = concretePhotos.filterNotNull(),
                                                    fuelBars = selectedDraft.fuelBars,
                                                    fuelCostPerBar = selectedDraft.fuelCostPerBar
                                                )

                                                RentalRepository.addScooter(newScooter)
                                                Toast.makeText(context, "Successfully registered ${newScooter.brand} ${newScooter.model}!", Toast.LENGTH_LONG).show()
                                                
                                                // Reset and return
                                                scooterStep = 0
                                                selectedDraftId = null
                                                concretePlate = ""
                                                for (i in 0..7) {
                                                    concretePhotos[i] = null
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (scooterStep == 3) "Register Scooter" else "Next")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPhotoSourceDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoSourceDialog = false },
                title = { Text("Select Photo Source") },
                text = { Text("Select photo #${activePhotoIndex + 1} from camera or gallery:") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            val uri = createTempImageUri("fleet_")
                            tempPhotoUriStr = uri.toString()
                            photoCameraLauncher.launch(uri)
                        }
                    ) {
                        Text("Camera")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            photoGalleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("Gallery")
                    }
                }
            )
        }
    }

    // Confirmation dialog for deleting a scooter blueprint draft template
    if (draftToDelete != null) {
        val draft = draftToDelete!!
        AlertDialog(
            onDismissRequest = { draftToDelete = null },
            title = { Text("Delete Blueprint Template?") },
            text = { Text("Are you sure you want to permanently delete the blueprint for ${draft.brand} ${draft.model}?") },
            confirmButton = {
                Button(
                    onClick = {
                        RentalRepository.deleteModelDraft(draft.id) { success ->
                             if (success) {
                                 Toast.makeText(context, "Deleted draft blueprint ${draft.model}", Toast.LENGTH_SHORT).show()
                             } else {
                                 Toast.makeText(context, "Failed to delete draft blueprint ${draft.model}", Toast.LENGTH_SHORT).show()
                             }
                         }
                         draftToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { draftToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for deleting an active fleet scooter
    if (scooterToDelete != null) {
        val scooter = scooterToDelete!!
        val isRented = scooter.status == ScooterStatus.RENTED
        
        AlertDialog(
            onDismissRequest = { if (!isDeletingScooter) scooterToDelete = null },
            title = { Text("Delete Scooter from Fleet?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to permanently delete the scooter ${scooter.brand} ${scooter.model} (Plate: ${scooter.plateNumber}) from the active fleet?")
                    if (isRented) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Warning: This scooter is currently rented! Deleting it will terminate its active rental agreement and remove all associated records from the cloud.",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingScooter = true
                        RentalRepository.deleteScooter(scooter.id) { success ->
                            isDeletingScooter = false
                            if (success) {
                                Toast.makeText(context, "Deleted scooter ${scooter.plateNumber} successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete scooter. Check database connection.", Toast.LENGTH_LONG).show()
                            }
                            scooterToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isDeletingScooter
                ) {
                    if (isDeletingScooter) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { scooterToDelete = null },
                    enabled = !isDeletingScooter
                ) {
                    Text("Cancel")
                }
            }
        )
    }



}
