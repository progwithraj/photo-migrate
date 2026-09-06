package com.photomigrate.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.fragment.app.FragmentActivity
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.TransferJob
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.data.repository.TransferRepository
import com.photomigrate.app.service.TransferWorker
import com.photomigrate.app.ui.components.MeshBackground
import com.photomigrate.app.ui.screens.*
import com.photomigrate.app.ui.theme.PhotoMigrateTheme
import com.photomigrate.app.ui.theme.ThemeManager
import com.photomigrate.app.util.BiometricAuthenticator
import kotlinx.coroutines.*

class MainActivity : FragmentActivity() {

    private lateinit var oauthManager: OAuthManager
    private lateinit var repository: TransferRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oauthManager = OAuthManager(this)
        repository = TransferRepository.getInstance(this)

        handleOAuthRedirect(intent)

        setContent {
            PhotoMigrateTheme(appTheme = ThemeManager.currentTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MeshBackground()
                    
                    val navController = rememberNavController()
                    var accounts by remember { mutableStateOf(oauthManager.getSavedAccounts()) }
                    var selectedSourceId by remember { mutableStateOf<String?>(accounts.firstOrNull()?.id) }
                    var selectedDestId by remember { mutableStateOf<String?>(accounts.getOrNull(1)?.id) }

                    val sourceMediaList by repository.sourceMediaList.collectAsState()
                    val isLoadingMedia by repository.isLoadingMedia.collectAsState()
                    val currentJob by repository.currentJob.collectAsState()

                    var incompleteJob by remember { mutableStateOf<TransferJob?>(null) }
                    
                    LaunchedEffect(Unit) {
                        val job = repository.getIncompleteJob()
                        Log.d("PhotoMigrate", "Checking for incomplete job... Found: ${job?.id} (Status: ${job?.status})")
                        incompleteJob = job
                    }

                    if (incompleteJob != null) {
                        AlertDialog(
                            onDismissRequest = { },
                            icon = { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary) },
                            title = { Text("Resume Unfinished Job?", fontWeight = FontWeight.ExtraBold) },
                            text = { 
                                Column {
                                    Text("An incomplete transfer from ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US).format(java.util.Date(incompleteJob!!.startTime))} was detected.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Progress: ${incompleteJob!!.completedItems} / ${incompleteJob!!.totalItems} items.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val jobToResume = incompleteJob!!
                                        incompleteJob = null
                                        lifecycleScope.launch {
                                            val resumed = repository.resumeJob(jobToResume)
                                            val sourceAcc = accounts.find { it.id == resumed.sourceAccountId }
                                            val destAcc = accounts.find { it.id == resumed.destinationAccountId }
                                            
                                            if (sourceAcc != null && destAcc != null) {
                                                val workData = Data.Builder()
                                                    .putString(TransferWorker.KEY_JOB_ID, resumed.id)
                                                    .putString(TransferWorker.KEY_SOURCE_ACCOUNT_ID, sourceAcc.id)
                                                    .putString(TransferWorker.KEY_DEST_ACCOUNT_ID, destAcc.id)
                                                    .putString(TransferWorker.KEY_MODE, resumed.mode.name)
                                                    .putBoolean("is_compressed", resumed.isCompressionEnabled)
                                                    .putString("org_mode", resumed.orgMode.name)
                                                    .build()

                                                val workRequest = OneTimeWorkRequestBuilder<TransferWorker>()
                                                    .setInputData(workData)
                                                    .build()

                                                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                                                    "photo_transfer_work",
                                                    androidx.work.ExistingWorkPolicy.REPLACE,
                                                    workRequest
                                                )
                                                navController.navigate("transfer")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Finish Transfer")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    val id = incompleteJob!!.id
                                    incompleteJob = null
                                    lifecycleScope.launch {
                                        repository.cancelJob(id)
                                    }
                                }) {
                                    Text("Discard & Start New", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }

                    var currentTab by remember { mutableIntStateOf(0) } // 0: Migrate, 1: Explore, 2: Vault

                    var showTelegramSetup by remember { mutableStateOf(false) }
                    var showTelegramProSetup by remember { mutableStateOf(false) }

                    if (showTelegramProSetup) {
                        var accountName by remember { mutableStateOf("") }
                        var botToken by remember { mutableStateOf("") }
                        var customChatId by remember { mutableStateOf("") }
                        var proError by remember { mutableStateOf<String?>(null) }
                        var isProVerifying by remember { mutableStateOf(false) }

                        AlertDialog(
                            onDismissRequest = { showTelegramProSetup = false },
                            title = { Text("Add Telegram Pro (MTProto 2GB)", fontWeight = FontWeight.ExtraBold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Unlocks direct MTProto chunked uploads for files up to 2 GB (4K Videos, RAWs).", fontSize = 13.sp)
                                    
                                    OutlinedTextField(
                                        value = accountName,
                                        onValueChange = { accountName = it; proError = null },
                                        label = { Text("Vault Name (e.g. My 2GB Backup)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = botToken,
                                        onValueChange = { botToken = it; proError = null },
                                        label = { Text("Bot Token") },
                                        placeholder = { Text("e.g. 123456:AAF...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = customChatId,
                                        onValueChange = { customChatId = it; proError = null },
                                        label = { Text("Channel / Chat ID (Optional)") },
                                        placeholder = { Text("Leave blank for auto-detect, or e.g. -100123456") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (proError != null) {
                                        Text(proError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = accountName.isNotBlank() && botToken.isNotBlank() && !isProVerifying,
                                    onClick = {
                                        isProVerifying = true
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val tgService = com.photomigrate.app.data.api.TelegramService()
                                            val cleanToken = botToken.trim()
                                            val botName = tgService.verifyBot(cleanToken)
                                            
                                            if (botName == null) {
                                                withContext(Dispatchers.Main) {
                                                    isProVerifying = false
                                                    proError = "Invalid Bot Token."
                                                }
                                                return@launch
                                            }

                                            val finalChatId = if (customChatId.isNotBlank()) {
                                                customChatId.trim()
                                            } else {
                                                tgService.getLatestChatId(cleanToken)
                                            }

                                            withContext(Dispatchers.Main) {
                                                isProVerifying = false
                                                if (finalChatId != null) {
                                                    oauthManager.saveTelegramProAccount(
                                                        com.photomigrate.app.data.model.TelegramProAccount(
                                                            id = "tg_pro_${System.currentTimeMillis()}",
                                                            botToken = cleanToken,
                                                            chatId = finalChatId,
                                                            name = accountName.trim()
                                                        )
                                                    )
                                                    Toast.makeText(this@MainActivity, "Telegram Pro Connected (2GB Limit Unlocked)!", Toast.LENGTH_SHORT).show()
                                                    showTelegramProSetup = false
                                                    recreate()
                                                } else {
                                                    proError = "Could not find Chat ID. Send a message to @$botName in Telegram first, or enter your Chat/Channel ID manually above!"
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    if (isProVerifying) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                    } else {
                                        Text("Connect MTProto Pro")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTelegramProSetup = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showTelegramSetup) {
                        var botToken by remember { mutableStateOf("") }
                        var isVerifying by remember { mutableStateOf(false) }
                        var botName by remember { mutableStateOf<String?>(null) }
                        var verifyError by remember { mutableStateOf<String?>(null) }

                        AlertDialog(
                            onDismissRequest = { showTelegramSetup = false },
                            title = { Text("Add Telegram Vault", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (botName == null) {
                                        Text("1. Talk to @BotFather on Telegram and create a new bot.", fontSize = 14.sp)
                                        Text("2. Paste your HTTP API Token below.", fontSize = 14.sp)
                                        OutlinedTextField(
                                            value = botToken,
                                            onValueChange = { botToken = it; verifyError = null },
                                            label = { Text("Bot Token") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (verifyError != null) Text(verifyError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    } else {
                                        Text("Bot Verified: @$botName", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Open Telegram and search for @$botName", fontSize = 14.sp)
                                        Text("2. Press Start or send any message to the bot.", fontSize = 14.sp)
                                        Text("3. Come back here and click 'Verify Chat'.", fontSize = 14.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = botToken.isNotBlank() && !isVerifying,
                                    onClick = {
                                        isVerifying = true
                                        val tgService = com.photomigrate.app.data.api.TelegramService()
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            if (botName == null) {
                                                val name = tgService.verifyBot(botToken.trim())
                                                withContext(Dispatchers.Main) {
                                                    isVerifying = false
                                                    if (name != null) {
                                                        botName = name
                                                    } else {
                                                        verifyError = "Invalid Bot Token"
                                                    }
                                                }
                                            } else {
                                                // Find Chat ID
                                                val chatId = tgService.getLatestChatId(botToken.trim())
                                                withContext(Dispatchers.Main) {
                                                    isVerifying = false
                                                    if (chatId != null) {
                                                        oauthManager.saveTelegramAccount(
                                                            com.photomigrate.app.data.model.TelegramAccount(
                                                                id = "tg_$chatId",
                                                                botToken = botToken.trim(),
                                                                chatId = chatId,
                                                                botName = botName!!
                                                            )
                                                        )
                                                        Toast.makeText(this@MainActivity, "Telegram Vault Added!", Toast.LENGTH_SHORT).show()
                                                        showTelegramSetup = false
                                                        recreate() // Quick refresh of the UI state
                                                    } else {
                                                        verifyError = "Could not find any recent messages. Make sure you sent a message to @$botName!"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    if (isVerifying) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                    } else {
                                        Text(if (botName == null) "Verify Bot" else "Verify Chat")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTelegramSetup = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                            if (currentRoute == "home") {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    modifier = Modifier.height(80.dp)
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = { Icon(Icons.Default.SyncAlt, null) },
                                        label = { Text("Migrate") }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = { Icon(Icons.Default.Explore, null) },
                                        label = { Text("Explore") }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 2,
                                        onClick = {
                                            if (BiometricAuthenticator.isAvailable(this@MainActivity)) {
                                                BiometricAuthenticator.authenticate(
                                                    activity = this@MainActivity,
                                                    onSuccess = { currentTab = 2 },
                                                    onError = { msg ->
                                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                currentTab = 2
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Security, null) },
                                        label = { Text("Vault") }
                                    )
                                }
                            }
                        }
                    ) { mainPadding ->
                        NavHost(
                            navController = navController, 
                            startDestination = "home", 
                            modifier = Modifier.padding(bottom = mainPadding.calculateBottomPadding())
                        ) {
                            composable("home") {
                                when (currentTab) {
                                    0 -> AccountScreen(
                                        accounts = accounts,
                                        telegramAccounts = oauthManager.getTelegramAccounts(),
                                        telegramProAccounts = oauthManager.getTelegramProAccounts(),
                                        selectedSourceId = selectedSourceId,
                                        selectedDestId = selectedDestId,
                                        onSelectSourceAccount = { id ->
                                            if (id == selectedDestId) {
                                                selectedDestId = selectedSourceId
                                            }
                                            selectedSourceId = id
                                        },
                                        onSelectDestAccount = { id ->
                                            if (id == selectedSourceId) {
                                                selectedSourceId = selectedDestId
                                            }
                                            selectedDestId = id
                                        },
                                        onAddAccountClick = {
                                            val authUrl = oauthManager.generateAuthUrl()
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                            startActivity(browserIntent)
                                        },
                                        onAddTelegramClick = {
                                            showTelegramSetup = true
                                        },
                                        onAddTelegramProClick = {
                                            showTelegramProSetup = true
                                        },
                                        onOpenSetupGuide = { navController.navigate("settings") },
                                        onOpenHistory = { navController.navigate("history") },
                                        onRemoveAccount = { email ->
                                            oauthManager.removeAccount(email)
                                            accounts = oauthManager.getSavedAccounts()
                                            if (selectedSourceId == accounts.find { it.email == email }?.id) selectedSourceId = null
                                            if (selectedDestId == accounts.find { it.email == email }?.id) selectedDestId = null
                                        },
                                        onRemoveTelegramAccount = { id ->
                                            oauthManager.removeTelegramAccount(id)
                                            if (selectedDestId == id) selectedDestId = null
                                            recreate()
                                        },
                                        onRemoveTelegramProAccount = { id ->
                                            oauthManager.removeTelegramProAccount(id)
                                            if (selectedDestId == id) selectedDestId = null
                                            recreate()
                                        },
                                        onRefreshAll = {
                                            lifecycleScope.launch {
                                                val currentAccs = oauthManager.getSavedAccounts()
                                                coroutineScope {
                                                    currentAccs.map { account ->
                                                        async { oauthManager.refreshStorageQuota(account) }
                                                    }.awaitAll()
                                                }
                                                accounts = oauthManager.getSavedAccounts()
                                                Toast.makeText(this@MainActivity, "Storage usage updated!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onProceedToPicker = {
                                            val sourceAccount = accounts.find { it.id == selectedSourceId }
                                            val destAccount = accounts.find { it.id == selectedDestId }
                                            val telegramAccount = oauthManager.getTelegramAccounts().find { it.id == selectedDestId }
                                            val telegramProAccount = oauthManager.getTelegramProAccounts().find { it.id == selectedDestId }
                                            
                                            if (sourceAccount != null && (destAccount != null || telegramAccount != null || telegramProAccount != null)) {
                                                lifecycleScope.launch {
                                                    val job = repository.getIncompleteJob()
                                                    if (job != null) {
                                                        incompleteJob = job
                                                    } else {
                                                        // Load media in background, don't block navigation
                                                        lifecycleScope.launch {
                                                            repository.loadSourceMedia(sourceAccount, destAccount) // destAccount can be null here for Telegram
                                                        }
                                                        navController.navigate("picker")
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    1 -> MediaExplorerScreen(
                                        accounts = accounts,
                                        onFetchMedia = { account ->
                                            repository.loadMediaForExplorer(account, limit = 500)
                                        }
                                    )
                                    else -> {
                                        val vaultItems by produceState<List<com.photomigrate.app.data.db.VaultItemEntity>>(initialValue = emptyList()) {
                                            value = repository.getVaultItems()
                                        }
                                        VaultScreen(
                                            items = vaultItems,
                                            vaultManager = repository.getVaultManager(),
                                            onDelete = { item ->
                                                lifecycleScope.launch {
                                                    repository.deleteFromVault(item)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            composable("picker") {
                                MediaPickerScreen(
                                    mediaItems = sourceMediaList,
                                    isLoading = isLoadingMedia,
                                    optimizationPreference = oauthManager.getOptimizationPreference(),
                                    defaultSortBy = oauthManager.getDefaultSortOrder(),
                                    aiOrgEnabled = oauthManager.isAiOrgEnabled(),
                                    onBackClick = { navController.popBackStack() },
                                    onMoveToVault = { items ->
                                        val sourceAccount = accounts.find { it.id == selectedSourceId }
                                        if (sourceAccount != null) {
                                            lifecycleScope.launch {
                                                Toast.makeText(this@MainActivity, "Moving ${items.size} items to Vault...", Toast.LENGTH_SHORT).show()
                                                var count = 0
                                                items.forEach { item ->
                                                    if (repository.moveToVault(sourceAccount, item)) count++
                                                }
                                                Toast.makeText(this@MainActivity, "Successfully moved $count items to Vault", Toast.LENGTH_LONG).show()
                                                navController.popBackStack()
                                            }
                                        }
                                    },
                                    onStartTransfer = { mode, isCompressed, orgMode, selectedItems ->
                                        val sourceAcc = accounts.find { it.id == selectedSourceId }
                                        val destAcc = accounts.find { it.id == selectedDestId }
                                        val telegramAcc = oauthManager.getTelegramAccounts().find { it.id == selectedDestId }
                                        val telegramProAcc = oauthManager.getTelegramProAccounts().find { it.id == selectedDestId }

                                        if (sourceAcc != null && (destAcc != null || telegramAcc != null || telegramProAcc != null)) {
                                            lifecycleScope.launch {
                                                val destId = destAcc?.id ?: telegramAcc?.id ?: telegramProAcc!!.id
                                                val destType = when {
                                                    telegramProAcc != null -> com.photomigrate.app.data.model.DestinationType.TELEGRAM_MTPROTO
                                                    telegramAcc != null -> com.photomigrate.app.data.model.DestinationType.TELEGRAM_BOT
                                                    else -> com.photomigrate.app.data.model.DestinationType.GOOGLE
                                                }
                                                val job = repository.createAndStartJob(sourceAcc, destId, destType, mode, isCompressed, orgMode, selectedItems)

                                                val workData = Data.Builder()
                                                    .putString(TransferWorker.KEY_JOB_ID, job.id)
                                                    .putString(TransferWorker.KEY_SOURCE_ACCOUNT_ID, sourceAcc.id)
                                                    .putString(TransferWorker.KEY_DEST_ACCOUNT_ID, destId)
                                                    .putString(TransferWorker.KEY_MODE, mode.name)
                                                    .putBoolean("is_compressed", isCompressed)
                                                    .putString("org_mode", orgMode.name)
                                                    .build()

                                                val workRequest = OneTimeWorkRequestBuilder<TransferWorker>()
                                                    .setInputData(workData)
                                                    .build()

                                                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                                                    "photo_transfer_work",
                                                    androidx.work.ExistingWorkPolicy.REPLACE,
                                                    workRequest
                                                )

                                                navController.navigate("transfer")
                                            }
                                        }
                                    }
                                )
                            }

                            composable("transfer") {
                                TransferScreen(
                                    job = currentJob,
                                    onPauseClick = { repository.pauseJob() },
                                    onResumeClick = { repository.resumeJob() },
                                    onDoneClick = {
                                        lifecycleScope.launch {
                                            val currentAccs = oauthManager.getSavedAccounts()
                                            selectedSourceId?.let { id ->
                                                currentAccs.find { it.id == id }?.let { oauthManager.refreshStorageQuota(it) }
                                            }
                                            selectedDestId?.let { id ->
                                                currentAccs.find { it.id == id }?.let { oauthManager.refreshStorageQuota(it) }
                                            }
                                            accounts = oauthManager.getSavedAccounts()
                                        }
                                        navController.popBackStack("home", inclusive = false)
                                    }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    currentClientId = oauthManager.getClientId(),
                                    currentClientSecret = oauthManager.getClientSecret(),
                                    onSaveCredentials = { clientId, clientSecret ->
                                        oauthManager.saveClientCredentials(clientId, clientSecret)
                                        Toast.makeText(this@MainActivity, "Credentials Saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("history") {
                                val history by produceState<List<TransferJob>>(initialValue = emptyList()) {
                                    value = repository.getHistory()
                                }
                                val totalBytes by produceState<Long>(initialValue = 0L) {
                                    value = repository.getTotalTransferredBytes()
                                }
                                val pendingCleanups by produceState<List<com.photomigrate.app.data.db.PendingCleanup>>(initialValue = emptyList()) {
                                    value = repository.getPendingCleanups()
                                }
                                
                                HistoryScreen(
                                    history = history,
                                    totalBytes = totalBytes,
                                    hasPendingCleanups = pendingCleanups.isNotEmpty(),
                                    onBackClick = { navController.popBackStack() },
                                    onOpenCleanup = { navController.navigate("cleanup") },
                                    onClearHistory = {
                                        lifecycleScope.launch {
                                            repository.clearHistory()
                                            navController.popBackStack()
                                            Toast.makeText(this@MainActivity, "History Cleared", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onFetchLogs = { jobId ->
                                        repository.getJobLogs(jobId)
                                    }
                                )
                            }

                            composable("cleanup") {
                                val items by produceState<List<com.photomigrate.app.data.db.PendingCleanup>>(initialValue = emptyList()) {
                                    value = repository.getPendingCleanups()
                                }
                                CleanupScreen(
                                    items = items,
                                    onBackClick = { navController.popBackStack() },
                                    onDeleteCloud = { item ->
                                        lifecycleScope.launch {
                                            Toast.makeText(this@MainActivity, "Retrying trash for ${item.filename}...", Toast.LENGTH_SHORT).show()
                                            val account = oauthManager.getSavedAccounts().find { it.id == item.accountId }
                                            if (account != null) {
                                                val api = com.photomigrate.app.data.api.GooglePhotosService(this@MainActivity)
                                                val success = api.deleteFromSourceAccount(account, item.mediaId, item.filename)
                                                if (success) {
                                                    repository.markCleanupDone(item.mediaId, item.accountId)
                                                    Toast.makeText(this@MainActivity, "Cleaned!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Retry failed again.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    onDismiss = { item ->
                                        lifecycleScope.launch {
                                            repository.markCleanupDone(item.mediaId, item.accountId)
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        Log.d("PhotoMigrate", "Received URI: $uri")
        if (uri.scheme == OAuthManager.REDIRECT_URI_SCHEME) {
            val code = uri.getQueryParameter("code")
            val verifier = oauthManager.getPendingVerifier()
            Log.d("PhotoMigrate", "Code found, exchanging... verifier: $verifier")

            if (!code.isNullOrEmpty()) {
                intent.data = null
                oauthManager.exchangeCodeForToken(code, verifier) { account, error ->
                    runOnUiThread {
                        if (account != null) {
                            Log.d("PhotoMigrate", "Success! Account: ${account.email}")
                            Toast.makeText(this, "Connected ${account.email}!", Toast.LENGTH_LONG).show()
                            recreate() 
                        } else {
                            Log.e("PhotoMigrate", "Auth Error: $error")
                            Toast.makeText(this, error ?: "Authentication failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}
