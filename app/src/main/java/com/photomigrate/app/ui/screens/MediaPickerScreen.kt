package com.photomigrate.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.components.MediaItemGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    mediaItems: List<MediaItem>,
    isLoading: Boolean,
    optimizationPreference: String = OAuthManager.OPT_ASK,
    aiOrgEnabled: Boolean = false,
    onBackClick: () -> Unit,
    onStartTransfer: (TransferMode, Boolean, com.photomigrate.app.data.model.OrganizationMode, List<MediaItem>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedMode by remember { mutableStateOf(TransferMode.MOVE) }
    var batchSize by remember { mutableStateOf(100) }
    
    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedOrgMode by remember { mutableStateOf(com.photomigrate.app.data.model.OrganizationMode.NONE) }

    val totalSelectedCount = selectedIds.size
    val allSelected = totalSelectedCount > 0 && totalSelectedCount == mediaItems.size

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Transfer Options", fontWeight = FontWeight.Bold) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select how you want to move your photos:")
                    
                    if (aiOrgEnabled) {
                        Text("AI Organization", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedOrgMode = com.photomigrate.app.data.model.OrganizationMode.NONE }) {
                            RadioButton(selected = selectedOrgMode == com.photomigrate.app.data.model.OrganizationMode.NONE, onClick = { selectedOrgMode = com.photomigrate.app.data.model.OrganizationMode.NONE })
                            Text("Original Library (No Sorting)", fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedOrgMode = com.photomigrate.app.data.model.OrganizationMode.BY_DATE }) {
                            RadioButton(selected = selectedOrgMode == com.photomigrate.app.data.model.OrganizationMode.BY_DATE, onClick = { selectedOrgMode = com.photomigrate.app.data.model.OrganizationMode.BY_DATE })
                            Text("Group by Date (e.g. Aug 2026)", fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedOrgMode = com.photomigrate.app.data.model.OrganizationMode.BY_CONTENT }) {
                            RadioButton(selected = selectedOrgMode == com.photomigrate.app.data.model.OrganizationMode.BY_CONTENT, onClick = { selectedOrgMode = com.photomigrate.app.data.model.OrganizationMode.BY_CONTENT })
                            Text("Smart AI Grouping (Nature, Pets...)", fontSize = 14.sp)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    
                    Text("Quality", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Choose whether to compress images to save storage.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQualityDialog = false
                        val itemsToTransfer = mediaItems.filter { it.id in selectedIds }
                        onStartTransfer(selectedMode, true, selectedOrgMode, itemsToTransfer)
                    }
                ) {
                    Text("Storage Saver")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQualityDialog = false
                        val itemsToTransfer = mediaItems.filter { it.id in selectedIds }
                        onStartTransfer(selectedMode, false, selectedOrgMode, itemsToTransfer)
                    }
                ) {
                    Text("Original Quality")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Select Items",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                        if (isLoading) {
                            Text(
                                "Scanning... (${mediaItems.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        selectedIds = if (allSelected) emptySet() else mediaItems.map { it.id }.toSet()
                    }) {
                        Text(
                            text = if (allSelected) "None" else "All",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Batch Size and Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Batch Size", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(100, 500, 1000).forEach { size ->
                                FilterChip(
                                    selected = batchSize == size,
                                    onClick = { batchSize = size },
                                    label = { Text(size.toString()) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedMode == TransferMode.MOVE,
                                onClick = { selectedMode = TransferMode.MOVE },
                                label = { Text("Move") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = selectedMode == TransferMode.COPY,
                                onClick = { selectedMode = TransferMode.COPY },
                                label = { Text("Copy") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { 
                        val itemsToTransfer = mediaItems.filter { it.id in selectedIds }
                        // Show popup if:
                        // 1. User needs to pick quality (ASK)
                        // 2. User HAS enabled AI features in settings (they need to pick organization)
                        if (optimizationPreference == OAuthManager.OPT_ASK || aiOrgEnabled) {
                            showQualityDialog = true
                        } else {
                            // Fast transfer using settings defaults
                            val isCompressed = optimizationPreference == OAuthManager.OPT_YES
                            onStartTransfer(selectedMode, isCompressed, selectedOrgMode, itemsToTransfer)
                        }
                    },
                    enabled = totalSelectedCount > 0,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Start Migration ($totalSelectedCount)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        selectedIds = mediaItems.take(batchSize).map { it.id }.toSet()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Select Next $batchSize", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (mediaItems.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Photos Found", fontWeight = FontWeight.Bold)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaItems, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        MediaItemGridCard(
                            item = item.copy(isSelected = isSelected),
                            onToggleSelect = {
                                selectedIds = if (isSelected) {
                                    selectedIds - item.id
                                } else {
                                    selectedIds + item.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
