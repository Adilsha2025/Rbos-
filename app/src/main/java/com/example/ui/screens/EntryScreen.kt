package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DiaryItem
import com.example.model.EditLog
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerOnContainer
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenPrimaryContainer
import com.example.ui.viewmodel.EntryDraftState
import com.example.util.GujaratiVoiceParser
import com.example.util.IndianNumberFormatter
import java.util.Locale
import kotlin.math.abs

private sealed class VoiceTarget {
    data class Item(val index: Int) : VoiceTarget()
    object BankDeposit : VoiceTarget()
    object Notes : VoiceTarget()
}

@Composable
fun EntryScreen(
    draftState: EntryDraftState,
    onAmountChange: (index: Int, amount: Long) -> Unit,
    onAddNewItem: (name: String, amount: Long) -> Unit,
    onRemoveItem: (index: Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onBankDepositAmountChange: (Long) -> Unit,
    onBankDepositSlipUriChange: (String?) -> Unit,
    onOpenNoteCounter: () -> Unit,
    onSave: () -> Unit,
    onUnlockToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddItemDialog by remember { mutableStateOf(false) }
    var activeVoiceTarget by remember { mutableStateOf<VoiceTarget?>(null) }
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }

    val isFormEditable = !draftState.isLocked || draftState.isEditingLocked

    // Speech Recognizer Launcher for Gujarati voice input
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                when (val target = activeVoiceTarget) {
                    is VoiceTarget.Item -> {
                        val parsed = GujaratiVoiceParser.parseSpokenAmount(spoken)
                        onAmountChange(target.index, parsed)
                        Toast.makeText(context, "બોલેલી રકમ: ₹$parsed ($spoken)", Toast.LENGTH_SHORT).show()
                    }
                    VoiceTarget.BankDeposit -> {
                        val parsed = GujaratiVoiceParser.parseSpokenAmount(spoken)
                        onBankDepositAmountChange(parsed)
                        Toast.makeText(context, "બેંક જમા: ₹$parsed ($spoken)", Toast.LENGTH_SHORT).show()
                    }
                    VoiceTarget.Notes -> {
                        val existing = draftState.notes
                        val updated = if (existing.isBlank()) spoken else "$existing $spoken"
                        onNotesChange(updated)
                    }
                    null -> {}
                }
            }
        }
    }

    fun startVoiceInput(target: VoiceTarget) {
        activeVoiceTarget = target
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "gu-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "ગુજરાતીમાં રકમ અથવા વિગત બોલો...")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "વોઇસ ઇનપુટ ઉપલબ્ધ નથી", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo Picker for Bank Deposit Slip
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onBankDepositSlipUriChange(uri.toString())
        }
    }

    // Difference calculation vs last saved day
    val previousTotal = draftState.previousSavedDayTotal
    val difference = if (previousTotal != null) draftState.total - previousTotal else null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Date Header Card (Auto-filled in Gujarati: "સોમવાર, 28 સપ્ટેમ્બર 2026")
            DateHeaderCard(
                dateDisplay = draftState.dateDisplay,
                isLocked = draftState.isLocked,
                isEditingLocked = draftState.isEditingLocked,
                onUnlock = onUnlockToEdit
            )
        }

        // RED ALERT BANNER if today's total is more than 10,000 lower than yesterday
        if (draftState.isDropAlertActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drop_alert_banner"),
                    colors = CardDefaults.cardColors(containerColor = DangerContainer),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DangerRed))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DangerRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "ચેતવણી",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ચેતવણી: મોટો ઘટાડો નોંધાયો!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DangerOnContainer
                            )
                            Text(
                                text = "આજનો હિસાબ ગઈકાલ કરતાં ₹ ${IndianNumberFormatter.formatIndian(draftState.dropDifference)} ઓછો છે (₹10,000 થી વધુ ઘટાડો). કૃપા કરીને રકમ ચકાસી લો.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DangerOnContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // List of amount items
        itemsIndexed(draftState.items) { index, item ->
            ItemAmountCard(
                index = index,
                item = item,
                editable = isFormEditable,
                onAmountChange = { newAmt -> onAmountChange(index, newAmt) },
                onOpenNoteCounter = onOpenNoteCounter,
                onVoiceInput = { startVoiceInput(VoiceTarget.Item(index)) },
                onRemove = { onRemoveItem(index) }
            )
        }

        // Add custom item button
        item {
            if (isFormEditable) {
                OutlinedButton(
                    onClick = { showAddItemDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_custom_item_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+ નવી વસ્તુ ઉમેરો",
                        tint = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ નવી વસ્તુ ઉમેરો",
                        style = MaterialTheme.typography.titleMedium,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // SEPARATE LINE / CARD FOR "ID માં + રોકડ" (Float Total)
        item {
            FloatTotalLineCard(floatTotal = draftState.floatTotal)
        }

        // BANK DEPOSIT ("બેંક જમા") CARD WITH AMOUNT & OPTIONAL PHOTO
        item {
            BankDepositCard(
                depositAmount = draftState.bankDepositAmount,
                slipUri = draftState.bankDepositSlipUri,
                editable = isFormEditable,
                onAmountChange = onBankDepositAmountChange,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemovePhoto = { onBankDepositSlipUriChange(null) },
                onVoiceInput = { startVoiceInput(VoiceTarget.BankDeposit) },
                onViewFullImage = { uri -> showFullScreenImage = uri }
            )
        }

        // DAY'S NOTES ("દિવસની નોંધ") TEXT BOX
        item {
            DayNotesCard(
                notes = draftState.notes,
                editable = isFormEditable,
                onNotesChange = onNotesChange,
                onVoiceInput = { startVoiceInput(VoiceTarget.Notes) }
            )
        }

        // Live Total Card and Difference
        item {
            Spacer(modifier = Modifier.height(4.dp))
            TotalSummaryCard(
                totalAmount = draftState.total,
                difference = difference
            )
        }

        // Save Button / Locked info
        item {
            if (isFormEditable) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_day_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "સાચવો",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "સાચવો",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onUnlockToEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("unlock_edit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "ફેરફાર કરો",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "આ હિસાબમાં ફેરફાર કરો",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Change Log / Edit History if any
        if (draftState.edits.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                EditHistoryCard(edits = draftState.edits)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add custom item dialog
    if (showAddItemDialog) {
        AddCustomItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { name, amt ->
                onAddNewItem(name, amt)
                showAddItemDialog = false
            }
        )
    }

    // Full screen image viewer for deposit slip
    if (showFullScreenImage != null) {
        AlertDialog(
            onDismissRequest = { showFullScreenImage = null },
            confirmButton = {
                TextButton(onClick = { showFullScreenImage = null }) {
                    Text("બંધ કરો")
                }
            },
            title = { Text("બેંક ડિપોઝિટ સ્લિપ", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = showFullScreenImage,
                        contentDescription = "બેંક સ્લિપ",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        )
    }
}

@Composable
private fun FloatTotalLineCard(floatTotal: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("float_total_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "ID માં + રોકડ",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ID માં + રોકડ (Float / તરલ રકમ)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "આજના તરલ ભંડોળનો સરવાળો",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "₹ ${IndianNumberFormatter.formatIndian(floatTotal)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = GreenPrimary
            )
        }
    }
}

@Composable
private fun BankDepositCard(
    depositAmount: Long,
    slipUri: String?,
    editable: Boolean,
    onAmountChange: (Long) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onVoiceInput: () -> Unit,
    onViewFullImage: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_deposit_card"),
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GoldAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "બેંક જમા",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (editable) {
                    IconButton(
                        onClick = onVoiceInput,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "બોલીને રકમ લખો",
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (editable) {
                OutlinedTextField(
                    value = if (depositAmount == 0L) "" else depositAmount.toString(),
                    onValueChange = { input ->
                        val parsed = IndianNumberFormatter.parseAmount(input)
                        onAmountChange(parsed)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("બેંકમાં જમા કરેલ રકમ") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = GreenPrimary) },
                    placeholder = { Text("0") },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("જમા રકમ:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹ ${IndianNumberFormatter.formatIndian(depositAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Photo Slip Section
            if (slipUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewFullImage(slipUri) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = slipUri,
                                contentDescription = "સ્લિપ ફોટો",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ડિપોઝિટ સ્લિપ ફોટો જોડાયેલ છે",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "જોવા માટે ટેપ કરો",
                                style = MaterialTheme.typography.bodySmall,
                                color = GreenPrimary
                            )
                        }
                    }

                    if (editable) {
                        IconButton(onClick = onRemovePhoto) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "સ્લિપ કાઢી નાખો",
                                tint = DangerRed
                            )
                        }
                    }
                }
            } else if (editable) {
                OutlinedButton(
                    onClick = onPickPhoto,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "સ્લિપનો ફોટો ઉમેરો",
                        tint = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "સ્લિપનો ફોટો ઉમેરો (વૈકલ્પિક)",
                        color = GreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DayNotesCard(
    notes: String,
    editable: Boolean,
    onNotesChange: (String) -> Unit,
    onVoiceInput: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_notes_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "દિવસની નોંધ",
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "દિવસની નોંધ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (editable) {
                    IconButton(
                        onClick = onVoiceInput,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "નોંધ બોલીને લખો",
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (editable) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("આજના દિવસની કોઈપણ વિશેષ નોંધ અથવા હિસાબની વિગત અહીં લખો...") },
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 4
                )
            } else {
                if (notes.isNotBlank()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                } else {
                    Text(
                        text = "કોઈ નોંધ લખેલ નથી",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DateHeaderCard(
    dateDisplay: String,
    isLocked: Boolean,
    isEditingLocked: Boolean,
    onUnlock: () -> Unit
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "દૈનિક ખાતાવહી (ભારતીય સમય)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }

            if (isLocked && !isEditingLocked) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GreenPrimaryContainer,
                    modifier = Modifier.clickable { onUnlock() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "લોક થયેલું",
                            tint = GreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "લોક થયેલું",
                            style = MaterialTheme.typography.labelMedium,
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isEditingLocked) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "ફેરફાર મોડ",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ફેરફાર મોડ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemAmountCard(
    index: Int,
    item: DiaryItem,
    editable: Boolean,
    onAmountChange: (Long) -> Unit,
    onOpenNoteCounter: () -> Unit,
    onVoiceInput: () -> Unit,
    onRemove: () -> Unit
) {
    val isCashItem = item.name.trim() == "રોકડ"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Gujarati Voice Input Button
                    if (editable) {
                        IconButton(
                            onClick = onVoiceInput,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("voice_input_btn_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "બોલીને લખો",
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // If "રોકડ", show "નોટ ગણો" button next to it!
                    if (isCashItem && editable) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { onOpenNoteCounter() }
                                .testTag("note_counter_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "નોટ ગણો",
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "નોટ ગણો",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                        }
                    }

                    // Delete custom added item (index >= 6)
                    if (editable && index >= 6) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "કાઢી નાખો",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount Field
            if (editable) {
                OutlinedTextField(
                    value = if (item.amount == 0L) "" else item.amount.toString(),
                    onValueChange = { input ->
                        val parsed = IndianNumberFormatter.parseAmount(input)
                        onAmountChange(parsed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input_$index"),
                    prefix = {
                        Text(
                            text = "₹ ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    },
                    suffix = {
                        if (item.amount > 0) {
                            Text(
                                text = IndianNumberFormatter.formatIndian(item.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "રકમ:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹ ${IndianNumberFormatter.formatIndian(item.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalSummaryCard(
    totalAmount: Long,
    difference: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GreenPrimaryContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "આજનો કુલ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹ ${IndianNumberFormatter.formatIndian(totalAmount)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = GreenPrimary,
                modifier = Modifier.testTag("today_total_text")
            )

            // Difference vs last saved day
            if (difference != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (difference > 0) Color.White.copy(alpha = 0.9f)
                    else if (difference < 0) Color(0xFFFEE2E2)
                    else Color.White.copy(alpha = 0.7f),
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (difference > 0) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "વધારે",
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ગઈકાલ કરતા ▲ ₹${IndianNumberFormatter.formatIndian(abs(difference))} વધારે",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        } else if (difference < 0) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "ઓછા",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ગઈકાલ કરતા ▼ ₹${IndianNumberFormatter.formatIndian(abs(difference))} ઓછા",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.TrendingFlat,
                                contentDescription = "સરખું",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ગઈકાલ જેટલું જ સમાન",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditHistoryCard(edits: List<EditLog>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "ફેરફાર ઇતિહાસ",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ફેરફાર ઇતિહાસ (${edits.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (expanded) "છુપાવો" else "જુઓ",
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    edits.forEach { edit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = edit.field,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "સમય: ${edit.time}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "₹${IndianNumberFormatter.formatIndian(edit.oldValue)} ➔ ₹${IndianNumberFormatter.formatIndian(edit.newValue)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Long) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var itemAmountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "+ નવી વસ્તુ ઉમેરો",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("વસ્તુ / ખાતાનું નામ") },
                    placeholder = { Text("દા.ત. અન્ય ડિપોઝિટ") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_item_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = itemAmountText,
                    onValueChange = { input ->
                        itemAmountText = input.filter { it.isDigit() }
                    },
                    label = { Text("રકમ (₹)") },
                    placeholder = { Text("0") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_item_amount_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isNotBlank()) {
                        val parsed = IndianNumberFormatter.parseAmount(itemAmountText)
                        onAdd(itemName.trim(), parsed)
                    }
                },
                enabled = itemName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_add_item_button")
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
