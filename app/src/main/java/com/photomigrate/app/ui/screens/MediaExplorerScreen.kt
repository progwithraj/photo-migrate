package com.photomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.ui.components.MediaItemGridCard
import com.photomigrate.app.ui.components.PremiumLoader
import com.photomigrate.app.ui.theme.CredNeonPink

@Composable
fun MediaExplorerScreen(
    accounts: List<GoogleAccount>,
    onFetchMedia: suspend (GoogleAccount) -> List<MediaItem>
) {
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedItemForViewer by remember { mutableStateOf<MediaItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("All") }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explore",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color.White
                )

                // Account Avatar Switcher
                if (selectedAccount != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CredNeonPink.copy(alpha = 0.2f))
                            .clickable {
                                val nextIdx = (accounts.indexOf(selectedAccount) + 1) % accounts.size.coerceAtLeast(1)
                                if (accounts.isNotEmpty()) selectedAccount = accounts[nextIdx]
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedAccount!!.email.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = CredNeonPink,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your photos...", color = Color(0xFF636578), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF636578)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF131422),
                    unfocusedContainerColor = Color(0xFF131422),
                    focusedBorderColor = CredNeonPink,
                    unfocusedBorderColor = Color(0xFF23253B)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "Images", "Videos", "Favorites").forEach { category ->
                    val isSelected = activeCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeCategory = category },
                        label = { Text(category, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CredNeonPink,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF131422),
                            labelColor = Color(0xFF9394A5)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFF23253B),
                            selectedBorderColor = CredNeonPink
                        )
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
                val filteredList = remember(mediaItems, activeCategory, searchQuery) {
                    var list = mediaItems
                    if (activeCategory == "Images") list = list.filter { !it.isVideo }
                    if (activeCategory == "Videos") list = list.filter { it.isVideo }
                    if (searchQuery.isNotBlank()) list = list.filter { it.filename.contains(searchQuery, ignoreCase = true) }
                    list
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredList) { item ->
                        MediaItemGridCard(item = item, onToggleSelect = {
                            selectedItemForViewer = item
                        })
                    }
                }
            }
        }
    }
}
