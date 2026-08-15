package com.photomigrate.app.ui.screens

import androidx.compose.foundation.background
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
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.components.MediaItemGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    mediaItems: List<MediaItem>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onStartTransfer: (TransferMode, List<MediaItem>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedMode by remember { mutableStateOf(TransferMode.MOVE) }
    var batchSize by remember { mutableStateOf(100) }

    val totalSelectedCount = selectedIds.size
    val allSelected = totalSelectedCount > 0 && totalSelectedCount == mediaItems.size

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
                            if (allSelected) "None" else "All",
                            fontWeight = FontWeight.Bold
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
                                    label = { Text("$size") },
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
                        onStartTransfer(selectedMode, itemsToTransfer) 
                    },
                    enabled = totalSelectedCount > 0,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
