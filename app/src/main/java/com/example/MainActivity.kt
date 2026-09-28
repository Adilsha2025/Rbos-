package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.NavigationItem
import com.example.ui.screens.CreditLedgerScreen
import com.example.ui.screens.EntryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NoteCounterBottomSheet
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.GreenPrimaryContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DiaryViewModel
import com.example.util.GujaratiDateUtils

class MainActivity : ComponentActivity() {

    private val viewModel: DiaryViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

                if (isAppLocked) {
                    PinLockScreen(
                        onPinEntered = { enteredPin ->
                            viewModel.onUnlockWithPin(enteredPin)
                        }
                    )
                } else {
                    var currentTab by remember { mutableStateOf(NavigationItem.ENTRY) }
                    val snackbarHostState = remember { SnackbarHostState() }

                    val entryDraft by viewModel.entryDraft.collectAsStateWithLifecycle()
                    val allDays by viewModel.allDays.collectAsStateWithLifecycle()
                    val allCredits by viewModel.allCredits.collectAsStateWithLifecycle()
                    val showNoteCounter by viewModel.showNoteCounter.collectAsStateWithLifecycle()
                    val noteCounts by viewModel.noteCounts.collectAsStateWithLifecycle()
                    val selectedHistoryDate by viewModel.selectedHistoryDate.collectAsStateWithLifecycle()
                    val historyCalendarMonth by viewModel.historyCalendarMonth.collectAsStateWithLifecycle()
                    val reportMonth by viewModel.reportMonth.collectAsStateWithLifecycle()
                    val settings by viewModel.settings.collectAsStateWithLifecycle()
                    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

                    // BackHandler: Return to Entry tab if on other tabs
                    if (currentTab != NavigationItem.ENTRY) {
                        BackHandler {
                            currentTab = NavigationItem.ENTRY
                        }
                    }

                    // Show Snackbar notifications
                    LaunchedEffect(snackbarMessage) {
                        snackbarMessage?.let { msg ->
                            snackbarHostState.showSnackbar(msg)
                            viewModel.clearSnackbar()
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Book,
                                                contentDescription = "ખાતાવહી",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "રોજની ડાયરી",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                    }
                                },
                                actions = {
                                    // Lock button if PIN is enabled
                                    if (settings.pinEnabled && settings.securityPin.length == 4) {
                                        IconButton(
                                            onClick = { viewModel.lockAppManually() },
                                            modifier = Modifier.testTag("lock_app_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "લૉક કરો",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    // Jump to today button
                                    IconButton(
                                        onClick = {
                                            viewModel.initializeDay(GujaratiDateUtils.getTodayIsoDate())
                                            currentTab = NavigationItem.ENTRY
                                        },
                                        modifier = Modifier.testTag("jump_to_today_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Today,
                                            contentDescription = "આજનો દિવસ",
                                            tint = Color.White
                                        )
                                    }

                                    // Quick Note Counter action button
                                    IconButton(
                                        onClick = { viewModel.openNoteCounter() },
                                        modifier = Modifier.testTag("top_note_counter_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = "નોટ ગણતરી",
                                            tint = Color.White
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = GreenPrimary
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                            ) {
                                NavigationItem.entries.forEach { item ->
                                    val isSelected = currentTab == item
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = item },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = GreenPrimary,
                                            selectedTextColor = GreenPrimary,
                                            indicatorColor = GreenPrimaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${item.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                NavigationItem.ENTRY -> {
                                    EntryScreen(
                                        draftState = entryDraft,
                                        onAmountChange = { index, amt ->
                                            viewModel.onItemAmountChanged(index, amt)
                                        },
                                        onAddNewItem = { name, amt ->
                                            viewModel.onAddNewCustomItem(name, amt)
                                        },
                                        onRemoveItem = { index ->
                                            viewModel.onRemoveItem(index)
                                        },
                                        onNotesChange = { notes ->
                                            viewModel.onNotesChanged(notes)
                                        },
                                        onBankDepositAmountChange = { amt ->
                                            viewModel.onBankDepositAmountChanged(amt)
                                        },
                                        onBankDepositSlipUriChange = { uri ->
                                            viewModel.onBankDepositSlipUriChanged(uri)
                                        },
                                        onOpenNoteCounter = { viewModel.openNoteCounter() },
                                        onSave = { viewModel.onSaveDay() },
                                        onUnlockToEdit = { viewModel.onUnlockForEdit() }
                                    )
                                }

                                NavigationItem.CREDIT -> {
                                    CreditLedgerScreen(
                                        credits = allCredits,
                                        onAddCredit = { name, amount, phone, notes, startDate ->
                                            viewModel.onAddCredit(name, amount, phone, notes, startDate)
                                        },
                                        onTogglePaid = { id, currentPaid ->
                                            viewModel.onToggleCreditPaid(id, currentPaid)
                                        },
                                        onDeleteCredit = { id ->
                                            viewModel.onDeleteCredit(id)
                                        }
                                    )
                                }

                                NavigationItem.HISTORY -> {
                                    HistoryScreen(
                                        allDays = allDays,
                                        selectedIsoDate = selectedHistoryDate,
                                        calendarMonthYear = historyCalendarMonth,
                                        onSelectDate = { isoDate ->
                                            viewModel.onSelectHistoryDate(isoDate)
                                        },
                                        onPrevMonth = { viewModel.onHistoryMonthPrev() },
                                        onNextMonth = { viewModel.onHistoryMonthNext() },
                                        onOpenInEntry = { date ->
                                            viewModel.initializeDay(date)
                                            currentTab = NavigationItem.ENTRY
                                        }
                                    )
                                }

                                NavigationItem.REPORT -> {
                                    ReportScreen(
                                        allDays = allDays,
                                        reportMonthYear = reportMonth,
                                        onPrevMonth = { viewModel.onReportMonthPrev() },
                                        onNextMonth = { viewModel.onReportMonthNext() }
                                    )
                                }

                                NavigationItem.SETTINGS -> {
                                    SettingsScreen(
                                        settings = settings,
                                        onToggleReminder = { viewModel.onToggleReminder(it) },
                                        onToggleDriveBackup = { viewModel.onToggleDriveBackup(it) },
                                        onUpdatePinSecurity = { enabled, pin ->
                                            viewModel.onUpdatePinSecurity(enabled, pin)
                                        },
                                        onAddDefaultItem = { viewModel.onAddDefaultItem(it) },
                                        onRemoveDefaultItem = { viewModel.onRemoveDefaultItem(it) },
                                        onRenameDefaultItem = { idx, name ->
                                            viewModel.onRenameDefaultItem(idx, name)
                                        },
                                        onMoveDefaultItem = { from, to ->
                                            viewModel.onMoveDefaultItem(from, to)
                                        },
                                        onResetDefaultItems = { viewModel.onResetDefaultItems() },
                                        onExportJson = { viewModel.getJsonExport() },
                                        onImportJson = { viewModel.importJsonData(it) },
                                        onClearAllData = { viewModel.clearAllData() }
                                    )
                                }
                            }

                            // Cash Note Counter Modal Sheet (SCREEN 2)
                            if (showNoteCounter) {
                                NoteCounterBottomSheet(
                                    noteCounts = noteCounts,
                                    denominations = viewModel.denominations,
                                    onCountChanged = { denom, count ->
                                        viewModel.onNoteCountChanged(denom, count)
                                    },
                                    onClearAll = { viewModel.onClearNoteCounts() },
                                    onApplyToCash = { viewModel.onApplyNotesToCash() },
                                    onDismiss = { viewModel.closeNoteCounter() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
