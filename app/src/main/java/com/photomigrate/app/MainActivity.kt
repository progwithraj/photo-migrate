package com.photomigrate.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

                    var currentTab by remember { mutableIntStateOf(0) } // 0: Migrate, 1: Explore, 2: Vault

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
                        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(mainPadding)) {
                            composable("home") {
                                when (currentTab) {
                                    0 -> AccountScreen(
                                        accounts = accounts,
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
                                        onOpenSetupGuide = { navController.navigate("settings") },
                                        onOpenHistory = { navController.navigate("history") },
                                        onRemoveAccount = { email ->
                                            oauthManager.removeAccount(email)
                                            accounts = oauthManager.getSavedAccounts()
                                            if (selectedSourceId == accounts.find { it.email == email }?.id) selectedSourceId = null
                                            if (selectedDestId == accounts.find { it.email == email }?.id) selectedDestId = null
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
                                            if (sourceAccount != null) {
                                                lifecycleScope.launch {
                                                    val result = repository.loadSourceMedia(sourceAccount, destAccount)
                                                    if (result.isEmpty()) {
                                                        Toast.makeText(this@MainActivity, "No photos found or connection error.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                navController.navigate("picker")
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

                                        if (sourceAcc != null && destAcc != null) {
                                            lifecycleScope.launch {
                                                val job = repository.createAndStartJob(sourceAcc, destAcc, mode, isCompressed, orgMode, selectedItems)

                                                val workData = Data.Builder()
                                                    .putString(TransferWorker.KEY_JOB_ID, job.id)
                                                    .putString(TransferWorker.KEY_SOURCE_ACCOUNT_ID, sourceAcc.id)
                                                    .putString(TransferWorker.KEY_DEST_ACCOUNT_ID, destAcc.id)
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
                                
                                HistoryScreen(
                                    history = history,
                                    totalBytes = totalBytes,
                                    onBackClick = { navController.popBackStack() },
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
