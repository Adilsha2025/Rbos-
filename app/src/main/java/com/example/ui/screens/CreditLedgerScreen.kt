package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CreditEntry
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenPrimaryContainer
import com.example.util.GujaratiDateUtils
import com.example.util.IndianNumberFormatter
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun CreditLedgerScreen(
    credits: List<CreditEntry>,
    onAddCredit: (name: String, amount: Long, phone: String, notes: String, startDate: String) -> Unit,
    onTogglePaid: (id: String, currentPaid: Boolean) -> Unit,
    onDeleteCredit: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showOnlyPending by remember { mutableStateOf(true) }

    val filteredCredits = if (showOnlyPending) {
        credits.filter { !it.isPaid }
    } else {
        credits
    }

    val totalPendingAmount = credits.filter { !it.isPaid }.sumOf { it.amount }
    val pendingCount = credits.count { !it.isPaid }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Total Pending Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenPrimaryContainer),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "ઉધાર યાદી (લેવાના બાકી નાણાં)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹ ${IndianNumberFormatter.formatIndian(totalPendingAmount)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "કુલ $pendingCount ગ્રાહકો પાસેથી રકમ લેવાની બાકી છે",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Add Person & Filter Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showOnlyPending,
                        onClick = { showOnlyPending = true },
                        label = { Text("બાકી ($pendingCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = !showOnlyPending,
                        onClick = { showOnlyPending = false },
                        label = { Text("તમામ (${credits.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_credit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+ નવું ઉધાર",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ઉમેરો", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of Credits
        if (filteredCredits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showOnlyPending) "કોઈ ઉધાર બાકી નથી!" else "યાદી ખાલી છે",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredCredits, key = { it.id }) { credit ->
                CreditPersonCard(
                    credit = credit,
                    onTogglePaid = { onTogglePaid(credit.id, credit.isPaid) },
                    onDelete = { onDeleteCredit(credit.id) },
                    onWhatsAppReminder = {
                        val daysPending = calculateDaysPending(credit.startDate)
                        sendWhatsAppReminder(context, credit, daysPending)
                    },
                    onCall = {
                        if (credit.phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${credit.phone}"))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showAddDialog) {
        AddCreditDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, amount, phone, notes, startDate ->
                onAddCredit(name, amount, phone, notes, startDate)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CreditPersonCard(
    credit: CreditEntry,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit,
    onWhatsAppReminder: () -> Unit,
    onCall: () -> Unit
) {
    val daysPending = calculateDaysPending(credit.startDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (credit.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (credit.isPaid) Color.LightGray.copy(alpha = 0.5f)
                                else GreenPrimaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "વ્યક્તિ",
                            tint = if (credit.isPaid) Color.Gray else GreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = credit.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (credit.phone.isNotBlank()) {
                            Text(
                                text = "ફોન: ${credit.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹ ${IndianNumberFormatter.formatIndian(credit.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (credit.isPaid) Color.Gray else DangerRed
                    )

                    if (!credit.isPaid) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (daysPending > 30) Color(0xFFFEE2E2) else Color(0xFFFEF9C3)
                        ) {
                            Text(
                                text = "$daysPending દિવસથી બાકી",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (daysPending > 30) DangerRed else GoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GreenPrimaryContainer
                        ) {
                            Text(
                                text = "ચૂકવાઈ ગયું",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (credit.notes.isNotBlank()) {
                Text(
                    text = "નોંધ: ${credit.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "આપ્યા તારીખ: ${GujaratiDateUtils.formatGujaratiDate(credit.startDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Paid / Unpaid Toggle
                Row(
                    modifier = Modifier.clickable { onTogglePaid() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (credit.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "ચૂકવણી સ્થિતિ",
                        tint = if (credit.isPaid) GreenPrimary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (credit.isPaid) "જમા થયેલ" else "જમા તરીકે નોંધો",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (credit.isPaid) GreenPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (credit.phone.isNotBlank()) {
                        IconButton(onClick = onCall, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "કૉલ કરો",
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!credit.isPaid) {
                        IconButton(onClick = onWhatsAppReminder, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "WhatsApp યાદ અપાવો",
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "કાઢી નાખો",
                            tint = DangerRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun calculateDaysPending(startDateIso: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = GujaratiDateUtils.INDIA_TIME_ZONE
        }
        val start = sdf.parse(startDateIso) ?: return 0L
        val today = Date()
        val diff = today.time - start.time
        TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0L)
    } catch (_: Exception) {
        0L
    }
}

private fun sendWhatsAppReminder(context: Context, credit: CreditEntry, daysPending: Long) {
    val message = "નમસ્તે ${credit.personName},\n\nરોજની ડાયરી ખાતાવહી મુજબ આપના ₹ ${IndianNumberFormatter.formatIndian(credit.amount)} લેવાના બાકી છે (છેલ્લા $daysPending દિવસથી).\n\nકૃપા કરીને રકમ જલ્દી જમા કરાવી આપવા વિનંતી.\nઆભાર!"
    try {
        val encoded = URLEncoder.encode(message, "UTF-8")
        val phoneSuffix = if (credit.phone.isNotBlank()) credit.phone.replace("+", "").replace(" ", "").trim() else ""
        val url = if (phoneSuffix.isNotBlank()) "https://wa.me/91$phoneSuffix?text=$encoded" else "https://wa.me/?text=$encoded"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "WhatsApp પર મોકલો"))
    }
}

@Composable
private fun AddCreditDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Long, phone: String, notes: String, startDate: String) -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(GujaratiDateUtils.getTodayIsoDate()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "+ નવું ઉધાર ખાતું ઉમેરો",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("ગ્રાહક / વ્યક્તિનું નામ") },
                    placeholder = { Text("દા.ત. રમેશભાઈ પટેલ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                    label = { Text("બાકી રકમ (₹)") },
                    placeholder = { Text("0") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("મોબાઇલ નંબર (વૈકલ્પિક)") },
                    placeholder = { Text("9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("વિગત / નોંધ (વૈકલ્પિક)") },
                    placeholder = { Text("દા.ત. કરિયાણા સામાનનું ઉધાર") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (personName.isNotBlank() && amountText.isNotBlank()) {
                        val parsed = IndianNumberFormatter.parseAmount(amountText)
                        onAdd(personName.trim(), parsed, phone.trim(), notes.trim(), startDate)
                    }
                },
                enabled = personName.isNotBlank() && amountText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("ઉમેરો", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("રદ કરો")
            }
        }
    )
}
