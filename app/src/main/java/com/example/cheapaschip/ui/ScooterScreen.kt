package com.example.cheapaschip.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cheapaschip.data.RentalRepository
import com.example.cheapaschip.data.Scooter
import com.example.cheapaschip.data.ScooterStatus
import com.example.cheapaschip.ui.theme.*

@Composable
fun ScooterScreen(
    modifier: Modifier = Modifier,
    onRentClick: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var detailScooter by remember { mutableStateOf<Scooter?>(null) }

    val modelFilters = listOf("All", "Click", "PCX", "ADV", "Forza")
    val filteredScooters = RentalRepository.scooters.filter { scooter ->
        val matchesSearch = scooter.model.contains(searchQuery, ignoreCase = true) ||
                scooter.plateNumber.contains(searchQuery, ignoreCase = true) ||
                scooter.brand.contains(searchQuery, ignoreCase = true)
        val matchesFilter = selectedFilter == "All" || scooter.model.contains(selectedFilter, ignoreCase = true)
        matchesSearch && matchesFilter
    }.sortedBy { it.status }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        Text(
            text = "Scooter Fleet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by brand, model, plate...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Model Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = modelFilters.indexOf(selectedFilter),
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            modelFilters.forEach { modelFilter ->
                Tab(
                    selected = selectedFilter == modelFilter,
                    onClick = { selectedFilter = modelFilter },
                    text = {
                        Text(
                            text = modelFilter,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedFilter == modelFilter) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Scooter List
        if (filteredScooters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No scooters match your search",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredScooters) { scooter ->
                    ScooterCard(
                        scooter = scooter,
                        onDetailClick = { detailScooter = scooter },
                        onShareClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val link = "https://cheapaschip.com/scooter/${scooter.id}"
                            val clip = ClipData.newPlainText("scooter_link", link)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied: $link", Toast.LENGTH_SHORT).show()
                        },
                        onRentClick = { onRentClick(scooter.id) }
                    )
                }
            }
        }
    }

    // Details Dialog
    detailScooter?.let { scooter ->
        AlertDialog(
            onDismissRequest = { detailScooter = null },
            title = {
                val (family, cc) = splitModelAndCc(scooter.model)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${scooter.brand} $family",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (cc.isNotEmpty()) {
                        Text(
                            text = cc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (scooter.images.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            items(scooter.images) { img ->
                                Card(
                                    modifier = Modifier.size(100.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        when (img) {
                                            is Uri -> {
                                                val bitmap = rememberUriBitmap(img)
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
                                                    bitmap = (img as android.graphics.Bitmap).asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Plate Number:", fontWeight = FontWeight.Bold)
                        Text(text = scooter.plateNumber)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Model Year:", fontWeight = FontWeight.Bold)
                        Text(text = "${scooter.year}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Engine Displacement:", fontWeight = FontWeight.Bold)
                        Text(text = "${scooter.cc} cc")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Key Features:", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (scooter.hasPhoneCharger) {
                            Text(
                                text = "Phone Charger",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .background(BrandGreenLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                color = BrandGreenDark
                            )
                        }
                        if (scooter.hasKeyless) {
                            Text(
                                text = "Keyless",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .background(BrandBlueLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                color = BrandBlueDark
                            )
                        }
                        if (scooter.hasAbs) {
                            Text(
                                text = "ABS Brakes",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .background(BrandYellowLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                color = BrandYellowDark
                            )
                        }
                        if (!scooter.hasPhoneCharger && !scooter.hasKeyless && !scooter.hasAbs) {
                            Text(text = "Standard setup", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Pricing Tiers:", fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val factor = if (RentalRepository.isHighSeasonMode) (1.0 + RentalRepository.highSeasonPercentage / 100.0) else 1.0
                        val daily = (scooter.priceDaily * factor).toInt()
                        val threeDay = (scooter.price3Day * factor).toInt()
                        val weekly = (scooter.priceWeekly * factor).toInt()
                        val twoWeek = (scooter.price2Week * factor).toInt()
                        val monthly = (scooter.priceMonthly * factor).toInt()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Daily Rate:", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "$daily ฿ / day", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "3-Day Rate:", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "$threeDay ฿ (${(threeDay/3)} ฿/day)", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Weekly Rate:", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "$weekly ฿ (${(weekly/7)} ฿/day)", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "2-Week Rate:", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "$twoWeek ฿ (${(twoWeek/14)} ฿/day)", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Monthly Rate:", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "$monthly ฿ (${(monthly/30)} ฿/day)", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Description:", fontWeight = FontWeight.Bold)
                    Text(
                        text = scooter.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                if (scooter.status == ScooterStatus.AVAILABLE) {
                    Button(
                        onClick = {
                            detailScooter = null
                            onRentClick(scooter.id)
                        }
                    ) {
                        Text("Rent Now")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { detailScooter = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ScooterCard(
    scooter: Scooter,
    onDetailClick: () -> Unit,
    onShareClick: () -> Unit,
    onRentClick: () -> Unit
) {
    val isAvailable = scooter.status == ScooterStatus.AVAILABLE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Picture of motor in a white box
                val firstImage = scooter.images.firstOrNull()
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (firstImage != null) {
                            when (firstImage) {
                                is Uri -> {
                                    val bitmap = rememberUriBitmap(firstImage)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "${scooter.brand} ${scooter.model}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = com.example.cheapaschip.R.drawable.modern_scooter_placeholder),
                                            contentDescription = "${scooter.brand} ${scooter.model}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                is android.graphics.Bitmap -> {
                                    Image(
                                        bitmap = (firstImage as android.graphics.Bitmap).asImageBitmap(),
                                        contentDescription = "${scooter.brand} ${scooter.model}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Image(
                                        painter = painterResource(id = com.example.cheapaschip.R.drawable.modern_scooter_placeholder),
                                        contentDescription = "${scooter.brand} ${scooter.model}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            Image(
                                painter = painterResource(id = com.example.cheapaschip.R.drawable.modern_scooter_placeholder),
                                contentDescription = "${scooter.brand} ${scooter.model}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Right side: Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Row 1: Plate Number & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scooter.plateNumber,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isAvailable) BrandGreenLight else BrandBlueLight,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isAvailable) "Available" else "Rented",
                                color = if (isAvailable) BrandGreenDark else BrandBlueDark,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Row 2: model and cc of motor
                    val (family, cc) = splitModelAndCc(scooter.model)
                    val modelText = if (cc.isNotEmpty()) "${scooter.brand} $family ($cc)" else "${scooter.brand} $family"
                    Text(
                        text = modelText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Row 3: price & year
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayPrice = if (RentalRepository.isHighSeasonMode) {
                            scooter.pricePerDay * (1.0 + RentalRepository.highSeasonPercentage / 100.0)
                        } else {
                            scooter.pricePerDay
                        }
                        Text(
                            text = "${displayPrice.toInt()} ฿ / day",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${scooter.year}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Feature indicators row (if any features exist)
            if (scooter.hasPhoneCharger || scooter.hasKeyless || scooter.hasAbs) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (scooter.hasPhoneCharger) {
                        Text(
                            text = "Charger",
                            fontSize = 11.sp,
                            color = BrandGreenDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (scooter.hasKeyless) {
                        Text(
                            text = "Keyless",
                            fontSize = 11.sp,
                            color = BrandBlueDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (scooter.hasAbs) {
                        Text(
                            text = "ABS",
                            fontSize = 11.sp,
                            color = BrandYellowDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Button
                OutlinedIconButton(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Info, contentDescription = "View Details")
                }

                // Share Button
                OutlinedIconButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Link")
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Button
                Button(
                    onClick = onRentClick,
                    enabled = isAvailable,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Rent Now")
                }
            }
        }
    }
}

private fun splitModelAndCc(model: String): Pair<String, String> {
    val parts = model.split(" ")
    if (parts.size >= 2) {
        val family = parts.dropLast(1).joinToString(" ")
        val cc = parts.last()
        return Pair(family, cc)
    }
    return Pair(model, "")
}



