package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DayEntry
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenPrimaryContainer
import com.example.util.GujaratiDateUtils
import com.example.util.IndianNumberFormatter
import com.example.util.ShareUtils
import java.util.Calendar
import kotlin.math.abs

@Composable
fun ReportScreen(
    allDays: List<DayEntry>,
    reportMonthYear: Pair<Int, Int>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (year, monthIndex) = reportMonthYear
    val monthTitle = GujaratiDateUtils.formatMonthYear(year, monthIndex)

    // Filter days for this month
    val monthPrefix = String.format("%04d-%02d", year, monthIndex + 1)
    val monthEntries = allDays.filter { it.date.startsWith(monthPrefix) }.sortedBy { it.date }

    // Statistics
    val savedDaysCount = monthEntries.size
    val maxEntry = monthEntries.maxByOrNull { it.total }
    val minEntry = monthEntries.minByOrNull { it.total }
    val monthlyTotal = monthEntries.sumOf { it.total }
    val dailyAverage = if (savedDaysCount > 0) monthlyTotal / savedDaysCount else 0L

    var selectedChartEntry by remember { mutableStateOf<DayEntry?>(null) }

    // COMPARISON 1: This Month vs Last Month
    val prevYear = if (monthIndex == 0) year - 1 else year
    val prevMonth = if (monthIndex == 0) 11 else monthIndex - 1
    val prevMonthPrefix = String.format("%04d-%02d", prevYear, prevMonth + 1)
    val prevMonthEntries = allDays.filter { it.date.startsWith(prevMonthPrefix) }
    val prevMonthTotal = prevMonthEntries.sumOf { it.total }
    val monthDiff = monthlyTotal - prevMonthTotal
    val prevMonthTitle = GujaratiDateUtils.formatMonthYear(prevYear, prevMonth)

    // COMPARISON 2: This Week vs Last Week (Trailing 7 days vs previous 7 days in India time)
    val cal = GujaratiDateUtils.getIndiaCalendar()
    val allDaysMap = allDays.associateBy { it.date }

    var thisWeekSum = 0L
    val todayCal = GujaratiDateUtils.getIndiaCalendar()
    for (i in 0 until 7) {
        val iso = String.format("%04d-%02d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1, todayCal.get(Calendar.DAY_OF_MONTH))
        thisWeekSum += allDaysMap[iso]?.total ?: 0L
        todayCal.add(Calendar.DAY_OF_YEAR, -1)
    }

    var lastWeekSum = 0L
    for (i in 0 until 7) {
        val iso = String.format("%04d-%02d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1, todayCal.get(Calendar.DAY_OF_MONTH))
        lastWeekSum += allDaysMap[iso]?.total ?: 0L
        todayCal.add(Calendar.DAY_OF_YEAR, -1)
    }
    val weekDiff = thisWeekSum - lastWeekSum

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Month Selector
            MonthSelectorHeader(
                monthTitle = monthTitle,
                onPrev = onPrevMonth,
                onNext = onNextMonth
            )
        }

        // Bar Chart Card
        item {
            DailyTotalsBarChartCard(
                monthEntries = monthEntries,
                year = year,
                monthIndex = monthIndex,
                selectedEntry = selectedChartEntry,
                onSelectEntry = { selectedChartEntry = it }
            )
        }

        // Section: Comparisons (This Month vs Last Month & This Week vs Last Week)
        item {
            Text(
                text = "સરખામણી અને તુલનાત્મક અહેવાલ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            // Month vs Last Month Comparison Card
            ComparisonCard(
                title = "આ મહિનો vs ગત મહિનો",
                currentLabel = monthTitle,
                currentAmount = monthlyTotal,
                previousLabel = prevMonthTitle,
                previousAmount = prevMonthTotal,
                difference = monthDiff
            )
        }

        item {
            // Week vs Last Week Comparison Card
            ComparisonCard(
                title = "આ અઠવાડિયું vs ગત અઠવાડિયું",
                currentLabel = "આ અઠવાડિયું (છેલ્લા 7 દિવસ)",
                currentAmount = thisWeekSum,
                previousLabel = "ગત અઠવાડિયું",
                previousAmount = lastWeekSum,
                difference = weekDiff
            )
        }

        // Stat Cards: સૌથી વધુ, સૌથી ઓછું, સાચવેલા દિવસો
        item {
            Text(
                text = "મહિનાના મુખ્ય આંકડા",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // સૌથી વધુ (Highest)
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "સૌથી વધુ",
                    amount = maxEntry?.total ?: 0L,
                    subtext = maxEntry?.date ?: "-",
                    icon = Icons.Default.ArrowUpward,
                    accentColor = GreenPrimary,
                    containerColor = GreenPrimaryContainer
                )

                // સૌથી ઓછું (Lowest)
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "સૌથી ઓછું",
                    amount = minEntry?.total ?: 0L,
                    subtext = minEntry?.date ?: "-",
                    icon = Icons.Default.ArrowDownward,
                    accentColor = DangerRed,
                    containerColor = Color(0xFFFEE2E2)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // સાચવેલા દિવસો
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "સાચવેલા દિવસો",
                    amount = null,
                    countDisplay = "$savedDaysCount દિવસ",
                    subtext = "કુલ એન્ટ્રીઓ",
                    icon = Icons.Default.DateRange,
                    accentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // માસિક સરેરાશ
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "માસિક સરેરાશ",
                    amount = dailyAverage,
                    subtext = "રોજિંદી સરેરાશ",
                    icon = Icons.Default.Savings,
                    accentColor = GoldAccent,
                    containerColor = Color(0xFFFEF9C3)
                )
            }
        }

        // Action Buttons: "PDF શેર કરો" & "WhatsApp પર મોકલો"
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        ShareUtils.shareMonthPdf(context, monthEntries, monthTitle)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("share_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF શેર કરો"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PDF શેર કરો",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val entryToShare = selectedChartEntry
                            ?: monthEntries.lastOrNull()
                            ?: maxEntry
                        if (entryToShare != null) {
                            ShareUtils.shareDayViaWhatsApp(context, entryToShare)
                        } else {
                            val emptyEntry = DayEntry(
                                date = GujaratiDateUtils.getTodayIsoDate(),
                                dateDisplay = monthTitle,
                                items = emptyList(),
                                total = 0L
                            )
                            ShareUtils.shareDayViaWhatsApp(context, emptyEntry)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("share_whatsapp_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "WhatsApp પર મોકલો",
                        tint = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    currentLabel: String,
    currentAmount: Long,
    previousLabel: String,
    previousAmount: Long,
    difference: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = title,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (difference > 0) GreenPrimaryContainer
                    else if (difference < 0) DangerContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (difference > 0) Icons.Default.TrendingUp
                            else if (difference < 0) Icons.Default.TrendingDown
                            else Icons.Default.TrendingFlat,
                            contentDescription = "ટ્રેન્ડ",
                            tint = if (difference > 0) GreenPrimary else if (difference < 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (difference > 0) "+ ₹${IndianNumberFormatter.formatIndian(difference)} વધારો"
                            else if (difference < 0) "- ₹${IndianNumberFormatter.formatIndian(abs(difference))} ઘટાડો"
                            else "સરખું",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (difference > 0) GreenPrimary else if (difference < 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹ ${IndianNumberFormatter.formatIndian(currentAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = previousLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹ ${IndianNumberFormatter.formatIndian(previousAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSelectorHeader(
    monthTitle: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.testTag("report_prev_month")) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "પાછલો મહિનો",
                    tint = GreenPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "માસિક હિસાબ અહેવાલ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }

            IconButton(onClick = onNext, modifier = Modifier.testTag("report_next_month")) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "આગલો મહિનો",
                    tint = GreenPrimary
                )
            }
        }
    }
}

@Composable
private fun DailyTotalsBarChartCard(
    monthEntries: List<DayEntry>,
    year: Int,
    monthIndex: Int,
    selectedEntry: DayEntry?,
    onSelectEntry: (DayEntry) -> Unit
) {
    val daysInMonth = GujaratiDateUtils.getDaysInMonth(year, monthIndex)
    val entriesMap = monthEntries.associateBy { it.date.takeLast(2).toIntOrNull() ?: 0 }
    val maxTotal = monthEntries.maxOfOrNull { it.total }?.coerceAtLeast(1L) ?: 100000L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "રોજિંદો કુલ હિસાબ ગ્રાફ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectedEntry != null) {
                    Text(
                        text = "${selectedEntry.date.takeLast(2)} તારીખ: ₹${IndianNumberFormatter.formatIndian(selectedEntry.total)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (monthEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "આ મહિના માટે કોઈ સેવ કરેલી એન્ટ્રી નથી",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(monthEntries) {
                            detectTapGestures { tapOffset ->
                                val stepX = size.width / daysInMonth.toFloat()
                                val tappedDay = (tapOffset.x / stepX).toInt() + 1
                                val entry = entriesMap[tappedDay]
                                if (entry != null) {
                                    onSelectEntry(entry)
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height - 30f
                    val stepX = width / daysInMonth.toFloat()
                    val barWidth = (stepX * 0.7f).coerceAtLeast(4f)

                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 1.5f
                    )

                    for (day in 1..daysInMonth) {
                        val entry = entriesMap[day]
                        val total = entry?.total ?: 0L
                        val barHeight = if (total > 0) (total.toFloat() / maxTotal) * (height - 20f) else 0f

                        val left = (day - 1) * stepX + (stepX - barWidth) / 2f
                        val top = height - barHeight

                        val isSelected = selectedEntry?.date?.takeLast(2)?.toIntOrNull() == day

                        if (barHeight > 0) {
                            drawRoundRect(
                                color = if (isSelected) Color(0xFF15803D)
                                else Color(0xFF22C55E).copy(alpha = 0.85f),
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        } else {
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.4f),
                                radius = 2f,
                                center = Offset(left + barWidth / 2f, height)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelDays = listOf(1, 5, 10, 15, 20, 25, daysInMonth)
                    labelDays.forEach { d ->
                        Text(
                            text = "$d",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Long?,
    countDisplay: String? = null,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (amount != null) "₹ ${IndianNumberFormatter.formatIndian(amount)}"
                else countDisplay ?: "-",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
