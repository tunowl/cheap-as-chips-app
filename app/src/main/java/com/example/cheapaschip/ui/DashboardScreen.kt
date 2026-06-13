package com.example.cheapaschip.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cheapaschip.data.RentalRepository

import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.foundation.clickable

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToScooters: () -> Unit
) {
    val stats = RentalRepository.getDashboardStats()
    val scrollState = rememberScrollState()
    var chartViewMode by remember { mutableStateOf("Weekly") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title / Welcome
        Column {
            Text(
                text = "CheapAsChip Rentals",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Shop Dashboard & Performance Analytics",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Weekly Revenue",
                value = "฿${String.format("%.2f", stats.weeklyRevenue)}",
                color = MaterialTheme.colorScheme.secondary, // Green
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Monthly Revenue",
                value = "฿${String.format("%.2f", stats.monthlyRevenue)}",
                color = MaterialTheme.colorScheme.primary, // Blue
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Revenue",
                value = "฿${String.format("%.2f", stats.totalRevenue)}",
                color = MaterialTheme.colorScheme.tertiary, // Yellow
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Rented Scooters",
                value = "${stats.rentedScooters}",
                color = MaterialTheme.colorScheme.primary, // Blue
                modifier = Modifier.weight(1f)
            )
        }

        // Chart Selector & Trend Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    Text(
                        text = if (chartViewMode == "Weekly") "Weekly Revenue Trend" else "Monthly Revenue Trend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Segmented view selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Weekly", "Monthly").forEach { mode ->
                            val isSelected = chartViewMode == mode
                            Button(
                                onClick = { chartViewMode = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(text = mode, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Text(
                    text = if (chartViewMode == "Weekly") "Estimated sales performance over the past week" else "Sales performance grouped by week over the past month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Bar Chart using Compose Canvas
                val chartData = if (chartViewMode == "Weekly") stats.dailyEarnings else stats.weeklyEarnings
                val maxEarning = chartData.values.maxOrNull() ?: 1.0
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barSpacing = 16.dp.toPx()
                    val totalBars = chartData.size
                    val totalSpacing = barSpacing * (totalBars - 1)
                    val barWidth = (canvasWidth - totalSpacing) / totalBars

                    var xOffset = 0f
                    chartData.forEach { (label, earning) ->
                        val barHeight = ((earning / maxEarning).toFloat()) * (canvasHeight - 30.dp.toPx())
                        val yOffset = canvasHeight - barHeight - 20.dp.toPx()

                        // Draw Bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            ),
                            topLeft = Offset(xOffset, yOffset),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        xOffset += barWidth + barSpacing
                    }
                }

                // Chart Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    chartData.keys.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Business Reports & Summaries Card
        var activeReportType by remember { mutableStateOf<String?>(null) } // "Weekly", "Monthly", or null
        
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
                    text = "Business Reports & Summaries",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Generate and export summaries of booking logs, earnings, and vehicle metrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { activeReportType = "Weekly" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Weekly Report", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { activeReportType = "Monthly" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Monthly Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // Show Dialog when activeReportType is not null
        if (activeReportType != null) {
            val isWeekly = activeReportType == "Weekly"
            val daysLimit = if (isWeekly) 7 else 30
            val reportStats = RentalRepository.getPeriodReportStats(daysLimit)
            
            ReportDetailDialog(
                isWeekly = isWeekly,
                stats = reportStats,
                onDismiss = { activeReportType = null }
            )
        }

        // Model Rent Report Card (Enhanced)
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
                    text = "Model Rental Performance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Detailed revenue and rental metrics per scooter model",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                val maxRentals = stats.modelRentStats.maxOfOrNull { it.rentalCount } ?: 1
                stats.modelRentStats.forEach { modelStat ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = modelStat.modelName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${modelStat.rentalCount} rentals",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Days: ${modelStat.totalDays} days",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "Revenue: ฿${String.format("%.2f", modelStat.totalRevenue)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Progress Bar representation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                        ) {
                            val ratio = modelStat.rentalCount.toFloat() / maxRentals
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Rented Scooters List
        val rentedScootersList = RentalRepository.scooters.filter { it.status == com.example.cheapaschip.data.ScooterStatus.RENTED }

        Text(
            text = "Currently Rented Scooters",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (rentedScootersList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No scooters are currently rented out.", color = Color.Gray)
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rentedScootersList.forEach { scooter ->
                    val contract = RentalRepository.contracts.find { it.scooterId == scooter.id && !it.isCompleted }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${scooter.brand} ${scooter.model}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Plate: ${scooter.plateNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Renter: ${contract?.customerName ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Phone: ${contract?.customerPhone ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Action Button
        Button(
            onClick = onNavigateToScooters,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Go to Scooters Inventory",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
fun ReportDetailDialog(
    isWeekly: Boolean,
    stats: com.example.cheapaschip.data.PeriodReportStats,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isGeneratingPdf by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    isGeneratingPdf = true
                    generateReportPdf(context, isWeekly, stats) {
                        isGeneratingPdf = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isGeneratingPdf) "Generating..." else "Download PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close")
            }
        },
        title = {
            Text(
                text = if (isWeekly) "Weekly Business Report" else "Monthly Business Report",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isWeekly) "Performance summary over the past 7 days" else "Performance summary over the past 30 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Key metrics grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Revenue:", fontWeight = FontWeight.SemiBold)
                            Text("฿${String.format("%.2f", stats.totalRevenue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rentals Processed:", fontWeight = FontWeight.SemiBold)
                            Text("${stats.rentalCount} contracts")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Avg Duration:", fontWeight = FontWeight.SemiBold)
                            Text("${String.format("%.1f", stats.averageDays)} days")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active / Completed:", fontWeight = FontWeight.SemiBold)
                            Text("${stats.activeRentals} / ${stats.completedRentals}")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cash Deposits Held:", fontWeight = FontWeight.SemiBold)
                            Text("฿${String.format("%.2f", stats.cashDepositsHeld)}")
                        }
                    }
                }

                // Model revenue breakdown section
                Text(
                    text = "Revenue by Scooter Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (stats.modelStats.isEmpty()) {
                    Text("No rental activity per model in this period.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stats.modelStats.forEach { modelStat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(modelStat.modelName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${modelStat.rentalCount} rentals | ${modelStat.totalDays} days", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text(
                                    "฿${String.format("%.2f", modelStat.totalRevenue)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }

                // Booking list logs
                Text(
                    text = "Period Bookings Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (stats.contracts.isEmpty()) {
                    Text("No contracts created in this period.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        stats.contracts.forEach { contract ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(contract.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = if (contract.isCompleted) "Completed" else "Active",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (contract.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    val bike = RentalRepository.scooters.find { it.id == contract.scooterId }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = bike?.let { "${it.brand} ${it.model} (${bike.plateNumber})" } ?: "Scooter",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "฿${String.format("%.2f", contract.totalPrice)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Text(
                                        text = "Started: ${contract.startDate} | Days: ${contract.days}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

fun generateReportPdf(
    context: android.content.Context,
    isWeekly: Boolean,
    stats: com.example.cheapaschip.data.PeriodReportStats,
    onComplete: () -> Unit
) {
    Toast.makeText(context, "Generating Report PDF...", Toast.LENGTH_SHORT).show()
    
    kotlin.concurrent.thread {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 1785
            val pageHeight = 2526
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paintText = Paint().apply {
                isAntiAlias = true
                textSize = 36f
                color = AndroidColor.BLACK
            }
            
            val paintTitle = Paint().apply {
                isAntiAlias = true
                textSize = 54f
                isFakeBoldText = true
                color = AndroidColor.rgb(37, 99, 235) // primary brand blue/accent
            }
            
            val paintSub = Paint().apply {
                isAntiAlias = true
                textSize = 32f
                color = AndroidColor.GRAY
            }
            
            val paintBorder = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = AndroidColor.LTGRAY
            }
            
            val paintFill = Paint().apply {
                style = Paint.Style.FILL
                color = AndroidColor.rgb(248, 250, 252) // slate background fill
            }

            var y = 100f
            
            // Header
            canvas.drawText("CheapAsChip Scooter Rentals", 100f, y, paintTitle)
            y += 60f
            val reportTitle = if (isWeekly) "Weekly Business Performance Report" else "Monthly Business Performance Report"
            canvas.drawText(reportTitle, 100f, y, paintTitle.apply { textSize = 46f })
            y += 50f
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            canvas.drawText("Generated on: ${sdf.format(Date())}", 100f, y, paintSub)
            y += 80f
            
            // Draw Metrics Summary Box
            canvas.drawRoundRect(100f, y, (pageWidth - 100).toFloat(), y + 360f, 16f, 16f, paintFill)
            canvas.drawRoundRect(100f, y, (pageWidth - 100).toFloat(), y + 360f, 16f, 16f, paintBorder)
            
            val textPaintBold = Paint(paintText).apply { isFakeBoldText = true }
            var my = y + 60f
            canvas.drawText("Key Business Metrics", 140f, my, textPaintBold.apply { textSize = 38f })
            my += 60f
            
            canvas.drawText("Total Period Revenue:", 140f, my, paintText)
            canvas.drawText("฿${String.format("%.2f", stats.totalRevenue)}", (pageWidth - 400).toFloat(), my, textPaintBold.apply { color = AndroidColor.rgb(16, 185, 129) }) // green
            my += 55f
            
            canvas.drawText("Rentals Booked Count:", 140f, my, paintText)
            canvas.drawText("${stats.rentalCount} contracts", (pageWidth - 400).toFloat(), my, paintText)
            my += 55f
            
            canvas.drawText("Average Rental Duration:", 140f, my, paintText)
            canvas.drawText("${String.format("%.1f", stats.averageDays)} days", (pageWidth - 400).toFloat(), my, paintText)
            my += 55f
            
            canvas.drawText("Cash Deposits Held:", 140f, my, paintText)
            canvas.drawText("฿${String.format("%.2f", stats.cashDepositsHeld)}", (pageWidth - 400).toFloat(), my, paintText)
            
            y += 440f
            
            // Model breakdown section
            canvas.drawText("Revenue by Scooter Model", 100f, y, textPaintBold.apply { textSize = 42f; color = AndroidColor.rgb(37, 99, 235) })
            y += 50f
            canvas.drawLine(100f, y, (pageWidth - 100).toFloat(), y, paintBorder)
            y += 50f
            
            if (stats.modelStats.isEmpty()) {
                canvas.drawText("No rental activity recorded in this period.", 100f, y, paintSub)
                y += 50f
            } else {
                stats.modelStats.forEach { modelStat ->
                    canvas.drawText(modelStat.modelName, 100f, y, textPaintBold.apply { textSize = 34f; color = AndroidColor.BLACK })
                    canvas.drawText("${modelStat.rentalCount} rentals | ${modelStat.totalDays} days", 100f, y + 40f, paintSub.apply { textSize = 28f })
                    
                    canvas.drawText(
                        "฿${String.format("%.2f", modelStat.totalRevenue)}",
                        (pageWidth - 400).toFloat(),
                        y + 25f,
                        textPaintBold.apply { textSize = 34f; color = AndroidColor.rgb(16, 185, 129) }
                    )
                    y += 100f
                    canvas.drawLine(100f, y, (pageWidth - 100).toFloat(), y, paintBorder.apply { strokeWidth = 1f })
                    y += 50f
                }
            }
            
            y += 30f
            
            // Bookings Log Section
            canvas.drawText("Period Bookings Log", 100f, y, textPaintBold.apply { textSize = 42f; color = AndroidColor.rgb(37, 99, 235) })
            y += 50f
            canvas.drawLine(100f, y, (pageWidth - 100).toFloat(), y, paintBorder.apply { strokeWidth = 2f })
            y += 50f
            
            if (stats.contracts.isEmpty()) {
                canvas.drawText("No bookings processed in this period.", 100f, y, paintSub)
            } else {
                stats.contracts.take(10).forEach { contract ->
                    canvas.drawText(contract.customerName, 100f, y, textPaintBold.apply { textSize = 32f; color = AndroidColor.BLACK })
                    val statusText = if (contract.isCompleted) "Completed" else "Active"
                    canvas.drawText("Status: $statusText", 100f, y + 40f, paintSub.apply { textSize = 26f })
                    
                    canvas.drawText(
                        "฿${String.format("%.2f", contract.totalPrice)}",
                        (pageWidth - 400).toFloat(),
                        y + 20f,
                        textPaintBold.apply { textSize = 32f; color = AndroidColor.BLACK }
                    )
                    y += 80f
                    canvas.drawLine(100f, y, (pageWidth - 100).toFloat(), y, paintBorder.apply { strokeWidth = 1f })
                    y += 40f
                }
                
                if (stats.contracts.size > 10) {
                    canvas.drawText("... and ${stats.contracts.size - 10} more contracts.", 100f, y, paintSub.apply { textSize = 30f })
                }
            }
            
            pdfDocument.finishPage(page)
            
            val filename = "CheapAsChips_Report_${if (isWeekly) "Weekly" else "Monthly"}_${System.currentTimeMillis()}.pdf"
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            var file = File(downloadsDir, filename)
            
            try {
                val fos = FileOutputStream(file)
                pdfDocument.writeTo(fos)
                fos.close()
                val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                mainHandler.post {
                    Toast.makeText(context, "Report saved to Downloads folder", Toast.LENGTH_LONG).show()
                    openPdfFile(context, file)
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("generateReportPdf", "Failed to write to public downloads", e)
                // Fallback to app directory
                val fallbackDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                file = File(fallbackDir, filename)
                try {
                    val fos = FileOutputStream(file)
                    pdfDocument.writeTo(fos)
                    fos.close()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post {
                        Toast.makeText(context, "Saved in private directory", Toast.LENGTH_LONG).show()
                        openPdfFile(context, file)
                        onComplete()
                    }
                } catch (e2: Exception) {
                    Log.e("generateReportPdf", "All writes failed", e2)
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post {
                        Toast.makeText(context, "Failed to save Report PDF", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                }
            } finally {
                pdfDocument.close()
            }
        } catch (e: Exception) {
            Log.e("generateReportPdf", "Error building PDF", e)
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                Toast.makeText(context, "Error compiling Report PDF", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        }
    }
}

