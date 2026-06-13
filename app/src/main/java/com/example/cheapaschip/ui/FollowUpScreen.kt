package com.example.cheapaschip.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cheapaschip.data.RentalContract
import com.example.cheapaschip.data.RentalRepository
import com.example.cheapaschip.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FollowUpScreen(
    modifier: Modifier = Modifier,
    onNewRentalClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Sort active rentals by due date (closest due date first)
    val activeContracts = RentalRepository.contracts
        .filter { !it.isCompleted }
        .sortedBy { RentalRepository.getDueDate(it).time }

    // Agreement preview dialog state
    var previewContract by remember { mutableStateOf<RentalContract?>(null) }
    var contractToReturn by remember { mutableStateOf<RentalContract?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        Text(
            text = "Active Rentals Follow-Up",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (activeContracts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No active rentals at the moment",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Button(onClick = onNewRentalClick) {
                        Text("Go to Scooters Inventory")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeContracts) { contract ->
                    val scooter = RentalRepository.scooters.find { it.id == contract.scooterId }
                    ActiveRentalCard(
                        contract = contract,
                        scooterModel = scooter?.let { "${it.brand} ${it.model}" } ?: "Unknown Scooter",
                        plateNumber = scooter?.plateNumber ?: "N/A",
                        onCallClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${contract.customerPhone}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReturnClick = {
                            contractToReturn = contract
                        },
                        onViewAgreementClick = {
                            previewContract = contract
                        }
                    )
                }
            }
        }
    }

    // Agreement Preview Dialog
    if (previewContract != null) {
        val contract = previewContract!!
        val scooter = RentalRepository.scooters.find { it.id == contract.scooterId }
        if (scooter != null) {
            Dialog(
                onDismissRequest = { previewContract = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.9f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Agreement Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            TextButton(onClick = { previewContract = null }) {
                                Text("Close")
                            }
                        }
                        HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            ContractReceiptPreview(
                                contract = contract,
                                scooter = scooter,
                                signaturePath = RentalRepository.signaturePaths[contract.id] ?: Path(),
                                signatureTrigger = 0
                            )
                        }
                    }
                }
            }
        }
    }

    if (contractToReturn != null) {
        val contract = contractToReturn!!
        val scooter = RentalRepository.scooters.find { it.id == contract.scooterId }
        val blueprint = scooter?.let { bike ->
            RentalRepository.modelDrafts.find { it.brand == bike.brand && it.model == bike.model }
        }
        val pricePerBar = blueprint?.fuelCostPerBar ?: scooter?.fuelCostPerBar ?: RentalRepository.globalFuelCostPerBar
        val maxBars = blueprint?.fuelBars ?: scooter?.fuelBars ?: 6
        
        val startingBars = remember(contract.fuelLevel, maxBars) {
            val ratio = contract.fuelLevel.toDouble() / 100.0
            kotlin.math.round(ratio * maxBars.toDouble()).toInt().coerceIn(0, maxBars)
        }
        
        var returnedFuelIndex by remember(startingBars) { mutableStateOf(startingBars) }
        var returnedFuelLevel by remember(startingBars, maxBars) {
            mutableStateOf(
                if (startingBars == 0) "Empty (0/$maxBars)" else if (startingBars == maxBars) "Full ($maxBars/$maxBars)" else "$startingBars/$maxBars"
            )
        }
        
        val missingBars = maxOf(0, startingBars - returnedFuelIndex)
        val fuelCharge = missingBars * pricePerBar

        Dialog(
            onDismissRequest = { contractToReturn = null }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Scooter Return Inspection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = scooter?.let { "${it.brand} ${it.model} (${scooter.plateNumber})" } ?: "Unknown Scooter",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Starting Fuel Level:")
                        Text(
                            text = if (startingBars == 0) "Empty (0/$maxBars)" else if (startingBars == maxBars) "Full ($maxBars/$maxBars)" else "$startingBars/$maxBars",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Returned Fuel Level", fontWeight = FontWeight.Bold)
                            Text(text = returnedFuelLevel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        
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
                                for (i in 1..maxBars) {
                                    val isFilled = returnedFuelIndex >= i
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isFilled) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.2f))
                                            .clickable {
                                                val newIndex = if (returnedFuelIndex == 1 && i == 1) 0 else i
                                                returnedFuelIndex = newIndex
                                                returnedFuelLevel = if (newIndex == 0) "Empty (0/$maxBars)" else if (newIndex == maxBars) "Full ($maxBars/$maxBars)" else "$newIndex/$maxBars"
                                            }
                                    )
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Charge Rate:")
                            Text(text = "${pricePerBar.toInt()} ฿ / bar", fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Missing Fuel:")
                            Text(text = "$missingBars bar(s)", fontWeight = FontWeight.SemiBold, color = if (missingBars > 0) Color.Red else Color.Gray)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Fuel Charge:", fontWeight = FontWeight.Bold)
                            Text(
                                text = "${fuelCharge.toInt()} ฿", 
                                fontWeight = FontWeight.Bold, 
                                color = if (fuelCharge > 0.0) Color.Red else MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { contractToReturn = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val finalChargeStr = if (fuelCharge > 0.0) " | Fuel Charge: ${fuelCharge.toInt()} ฿" else ""
                                RentalRepository.returnScooter(contract.id, context)
                                Toast.makeText(context, "Scooter marked as returned!$finalChargeStr", Toast.LENGTH_LONG).show()
                                contractToReturn = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Return")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveRentalCard(
    contract: RentalContract,
    scooterModel: String,
    plateNumber: String,
    onCallClick: () -> Unit,
    onReturnClick: () -> Unit,
    onViewAgreementClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contract.customerName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Relative time status badge
                        val now = System.currentTimeMillis()
                        val dueDate = RentalRepository.getDueDate(contract)
                        val diffMs = dueDate.time - now
                        
                        val (badgeText, badgeBg, badgeTextTint) = when {
                            diffMs < 0 -> {
                                val absDiff = Math.abs(diffMs)
                                val h = absDiff / (3600 * 1000)
                                val m = (absDiff % (3600 * 1000)) / (60 * 1000)
                                val overdueStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                Triple("Overdue by $overdueStr", Color(0xFFFEE2E2), Color(0xFF991B1B))
                            }
                            diffMs < 3600 * 1000 -> {
                                val m = diffMs / (60 * 1000)
                                Triple("Due in ${m}m", BrandYellowLight, BrandYellowDark)
                            }
                            else -> {
                                val d = diffMs / (24 * 3600 * 1000)
                                val h = (diffMs % (24 * 3600 * 1000)) / (3600 * 1000)
                                val dueStr = if (d > 0) "${d}d ${h}h" else "${h}h"
                                Triple("Due in $dueStr", BrandBlueLight, BrandBlueDark)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(badgeBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextTint,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Phone: ${contract.customerPhone}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    if (contract.customerWhatsApp.isNotEmpty()) {
                        Text(
                            text = "WhatsApp: ${contract.customerWhatsApp}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                // Call Button
                IconButton(
                    onClick = onCallClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call Customer")
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            // Scooter Info & Rental Terms
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Rented Scooter:", fontWeight = FontWeight.Bold)
                    Text(text = "$scooterModel ($plateNumber)")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Rental Start Date:", fontWeight = FontWeight.Bold)
                    Text(text = contract.startDate)
                }
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dueDate = RentalRepository.getDueDate(contract)
                val dueDateStr = sdf.format(dueDate)
                val now = System.currentTimeMillis()
                val isOverdue = dueDate.time < now
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Return Due Date:", fontWeight = FontWeight.Bold)
                    Text(
                        text = dueDateStr,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverdue) Color(0xFF991B1B) else Color.Unspecified
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Duration:", fontWeight = FontWeight.Bold)
                    Text(text = "${contract.days} days")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Price:", fontWeight = FontWeight.Bold)
                    Text(text = "${contract.totalPrice} ฿", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Deposit Amount:", fontWeight = FontWeight.Bold)
                    Text(text = if (contract.depositAmount > 0.0) "${contract.depositAmount} ฿" else "Original Passport Held")
                }
            }

            if (contract.notes.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Notes: ${contract.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Agreement Button
                OutlinedButton(
                    onClick = onViewAgreementClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "View Agreement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Return Action Button
                Button(
                    onClick = onReturnClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mark Returned",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
