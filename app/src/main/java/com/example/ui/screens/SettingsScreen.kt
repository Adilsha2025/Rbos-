package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenPrimaryContainer
import com.example.util.ShareUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onToggleReminder: (Boolean) -> Unit,
    onToggleDriveBackup: (Boolean) -> Unit,
    onUpdatePinSecurity: (Boolean, String) -> Unit,
    onAddDefaultItem: (String) -> Unit,
    onRemoveDefaultItem: (Int) -> Unit,
    onRenameDefaultItem: (Int, String) -> Unit,
    onMoveDefaultItem: (Int, Int) -> Unit,
    onResetDefaultItems: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Int,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var renameIndex by remember { mutableStateOf<Int?>(null) }
    var renameCurrentText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Cloud & Local Storage Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenPrimaryContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "ડેટા સુરક્ષા",
                            tint = GreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ડેટા સંપૂર્ણ સુરક્ષિત છે",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "સ્થાનિક રૂમ ડેટાબેઝ અને ફાયરસ્ટોર ક્લાઉડ સિંક",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Section: Reminders & Backup
        item {
            Text(
                text = "રિમાઇન્ડર અને બેકઅપ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Daily reminder toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "રિમાઇન્ડર",
                                tint = GreenPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "રોજનું રિમાઇન્ડર — રાત્રે 9:00",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "જો આજની એન્ટ્રી બાકી હોય તો 21:00 વાગ્યે સૂચના આપો",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = onToggleReminder,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GreenPrimary
                            ),
                            modifier = Modifier.testTag("daily_reminder_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Google Drive Backup toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Google Drive",
                                tint = GreenPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Drive બેકઅપ",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "દરેક વખતે સાચવ્યા પછી સુરક્ષિત ઓટો-બેકઅપ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.driveBackup,
                            onCheckedChange = onToggleDriveBackup,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GreenPrimary
                            ),
                            modifier = Modifier.testTag("drive_backup_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // 4-Digit Security PIN Lock Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Security,
                                contentDescription = "સુરક્ષા PIN",
                                tint = GreenPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "૪-અંકનો સુરક્ષા PIN લૉક",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (settings.pinEnabled) "એપ ખોલતી વખતે PIN પૂછવામાં આવશે" else "એપ ખોલવા માટે 4-અંકનો PIN સેટ કરો",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (settings.pinEnabled) {
                                    Text(
                                        text = "PIN બદલો",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenPrimary,
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clickable { showSetPinDialog = true }
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = settings.pinEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    showSetPinDialog = true
                                } else {
                                    onUpdatePinSecurity(false, "")
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GreenPrimary
                            ),
                            modifier = Modifier.testTag("pin_lock_switch")
                        )
                    }
                }
            }
        }

        // Section: Default Item List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ડિફોલ્ટ વસ્તુઓની યાદી",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onResetDefaultItems) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "ફરીથી સેટ કરો",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("રીસેટ કરો", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    settings.defaultItems.forEachIndexed { index, itemName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${index + 1}. $itemName",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Move Up
                                IconButton(
                                    onClick = { if (index > 0) onMoveDefaultItem(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "ઉપર ખસેડો",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Move Down
                                IconButton(
                                    onClick = { if (index < settings.defaultItems.size - 1) onMoveDefaultItem(index, index + 1) },
                                    enabled = index < settings.defaultItems.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "નીચે ખસેડો",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Rename
                                IconButton(
                                    onClick = {
                                        renameIndex = index
                                        renameCurrentText = itemName
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "નામ બદલો",
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { onRemoveDefaultItem(index) },
                                    enabled = settings.defaultItems.size > 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "કાઢી નાખો",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (index < settings.defaultItems.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                thickness = 0.5.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showAddItemDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("add_default_item_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "+ નવી વસ્તુ ઉમેરો",
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ નવી વસ્તુ ઉમેરો",
                            style = MaterialTheme.typography.titleSmall,
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: JSON Backup & Export
        item {
            Text(
                text = "બેકઅપ અને એક્સપોર્ટ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "તમારો તમામ ડેટા JSON ફાઈલ તરીકે ડાઉનલોડ અથવા રિસ્ટોર કરો જેથી ક્યારેય ડેટા ગુમ ન થાય.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val json = onExportJson()
                                    ShareUtils.shareJsonBackup(context, json)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_json_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "JSON એક્સપોર્ટ"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JSON એક્સપોર્ટ")
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_json_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "JSON ઇમ્પોર્ટ",
                                tint = GreenPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JSON ઇમ્પોર્ટ", color = GreenPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("clear_all_data_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "બધો ડેટા સાફ કરો",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "બધો ડેટા સાફ કરો (ખાલી શરૂઆત)",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Add Default Item Dialog
    if (showAddItemDialog) {
        var newItemName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("નવી ડિફોલ્ટ વસ્તુ ઉમેરો", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("વસ્તુનું નામ") },
                    placeholder = { Text("દા.ત. બેંક ડિપોઝિટ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            onAddDefaultItem(newItemName)
                            showAddItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = newItemName.isNotBlank()
                ) {
                    Text("ઉમેરો")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("રદ કરો")
                }
            }
        )
    }

    // Rename Item Dialog
    if (renameIndex != null) {
        val idx = renameIndex!!
        AlertDialog(
            onDismissRequest = { renameIndex = null },
            title = { Text("વસ્તુનું નામ બદલો", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameCurrentText,
                    onValueChange = { renameCurrentText = it },
                    label = { Text("નવું નામ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameCurrentText.isNotBlank()) {
                            onRenameDefaultItem(idx, renameCurrentText)
                            renameIndex = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = renameCurrentText.isNotBlank()
                ) {
                    Text("સાચવો")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameIndex = null }) {
                    Text("રદ કરો")
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("JSON ડેટા ઇમ્પોર્ટ કરો", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "અહીં અગાઉ એક્સપોર્ટ કરેલ JSON ડેટા પેસ્ટ કરો:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("{ \"app\": \"રોજની ડાયરી\", ... }") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            onImportJson(importJsonInput)
                            showImportDialog = false
                            importJsonInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = importJsonInput.isNotBlank()
                ) {
                    Text("ઇમ્પોર્ટ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("રદ કરો")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("બધો ડેટા સાફ કરવો છે?", fontWeight = FontWeight.Bold) },
            text = {
                Text("આનાથી તમામ દિવસોની સાચવેલી એન્ટ્રીઓ કાયમ માટે ભૂંસાઈ જશે અને એપ્લિકેશન ખાલી થઈ જશે.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("હા, બધું સાફ કરો")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("રદ કરો")
                }
            }
        )
    }

    // Set / Change 4-Digit PIN Dialog
    if (showSetPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = {
                Text(
                    text = if (settings.pinEnabled) "સુરક્ષા PIN બદલો" else "૪-અંકનો સુરક્ષા PIN સેટ કરો",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "એપ ખોલતી વખતે આ 4-અંકનો PIN પૂછવામાં આવશે:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                        label = { Text("નવો 4-અંકનો PIN") },
                        placeholder = { Text("દા.ત. 1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmPinInput = it },
                        label = { Text("PIN ની પુષ્ટિ કરો") },
                        placeholder = { Text("ફરીથી PIN દાખલ કરો") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(
                            text = pinError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinError = "PIN બરાબર 4 અંકનો હોવો જોઈએ"
                        } else if (pinInput != confirmPinInput) {
                            pinError = "બંને PIN મેળ ખાતા નથી"
                        } else {
                            onUpdatePinSecurity(true, pinInput)
                            showSetPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = pinInput.length == 4 && confirmPinInput.length == 4
                ) {
                    Text("PIN સેટ કરો")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("રદ કરો")
                }
            }
        )
    }
}
