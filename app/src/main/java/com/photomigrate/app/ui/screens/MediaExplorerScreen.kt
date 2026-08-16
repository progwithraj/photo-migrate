package com.photomigrate.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.ui.components.MediaItemGridCard
import com.photomigrate.app.ui.components.PremiumLoader

@Composable
fun MediaExplorerScreen(
    accounts: List<GoogleAccount>,
    onFetchMedia: suspend (GoogleAccount) -> List<MediaItem>
) {
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedItemForViewer by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(selectedAccount) {
        selectedAccount?.let { account ->
            isLoading = true
            mediaItems = onFetchMedia(account)
            isLoading = false
        }
    }

    if (selectedItemForViewer != null) {
        MediaViewerScreen(
            item = selectedItemForViewer!!,
            accessToken = selectedAccount?.accessToken ?: "",
            onClose = { selectedItemForViewer = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Explore Your Gallery",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "View all your photos and videos from Google Photos.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Account Selector (Horizontal)
            ScrollableTabRow(
                selectedTabIndex = accounts.indexOf(selectedAccount).coerceAtLeast(0),
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {}
            ) {
                accounts.forEach { account ->
                    Tab(
                        selected = selectedAccount == account,
                        onClick = { selectedAccount = account },
                        text = {
                            Text(
                                account.email.substringBefore("@"),
                                fontWeight = if (selectedAccount == account) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumLoader()
                }
            } else if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No photos found in this account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(mediaItems) { item ->
                        MediaItemGridCard(item = item, onToggleSelect = {
                            selectedItemForViewer = item
                        })
                    }
                }
            }
        }
    }
}
