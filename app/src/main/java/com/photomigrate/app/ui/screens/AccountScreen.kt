package com.photomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.model.AccountRole
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.TelegramAccount
import com.photomigrate.app.data.model.TelegramProAccount
import com.photomigrate.app.ui.components.AccountCard
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.components.TelegramAccountCard
import com.photomigrate.app.ui.components.TelegramProAccountCard
import com.photomigrate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    accounts: List<GoogleAccount>,
    telegramAccounts: List<TelegramAccount> = emptyList(),
    telegramProAccounts: List<TelegramProAccount> = emptyList(),
    selectedSourceId: String?,
    selectedDestId: String?,
    onSelectSourceAccount: (String) -> Unit,
    onSelectDestAccount: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAddTelegramClick: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onOpenHistory: () -> Unit,
    onRemoveAccount: (String) -> Unit,
    onRemoveTelegramAccount: (String) -> Unit,
    onRemoveTelegramProAccount: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onProceedToPicker: () -> Unit
) {
    val isEnabled = selectedSourceId != null && selectedDestId != null && selectedSourceId != selectedDestId

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PhotoMigrate",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onOpenSetupGuide) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onProceedToPicker,
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isEnabled) 8.dp else 0.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text("Select Photos", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 90.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Banner
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Migrate Securely",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "DIRECT & FREE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Move your precious memories between accounts without losing quality. Completely free and direct.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Accounts",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRefreshAll) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                        }
                        
                        TextButton(onClick = onAddAccountClick) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google", fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = onAddTelegramClick) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Telegram", fontWeight = FontWeight.Bold, color = Color(0xFF0088CC))
                        }
                    }
                }
            }

            if (accounts.isEmpty() && telegramAccounts.isEmpty() && telegramProAccounts.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Accounts Yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Connect your Google accounts to start.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAddAccountClick,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Connect Now")
                            }
                        }
                    }
                }
            } else {
                items(accounts) { account ->
                    AccountCard(
                        account = account,
                        isSelectedAsSource = selectedSourceId == account.id,
                        isSelectedAsDest = selectedDestId == account.id,
                        onSelectRole = { role ->
                            if (role == AccountRole.SOURCE) {
                                onSelectSourceAccount(account.id)
                            } else {
                                onSelectDestAccount(account.id)
                            }
                        },
                        onRemoveAccount = { onRemoveAccount(account.email) }
                    )
                }

                items(telegramAccounts) { tgAccount ->
                    TelegramAccountCard(
                        account = tgAccount,
                        isSelectedAsDest = selectedDestId == tgAccount.id,
                        onSelectDest = { onSelectDestAccount(tgAccount.id) },
                        onRemoveAccount = { onRemoveTelegramAccount(tgAccount.id) }
                    )
                }

                items(telegramProAccounts) { proAccount ->
                    TelegramProAccountCard(
                        account = proAccount,
                        isSelectedAsDest = selectedDestId == proAccount.id,
                        onSelectDest = { onSelectDestAccount(proAccount.id) },
                        onRemoveAccount = { onRemoveTelegramProAccount(proAccount.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
