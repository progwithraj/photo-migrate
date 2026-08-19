package com.photomigrate.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.OrganizationMode
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.components.MediaItemGridCard
import com.photomigrate.app.ui.components.PremiumLoader
import kotlinx.coroutines.launch

enum class SortBy {
    NEWEST, OLDEST, SIZE_DESC, SIZE_ASC, NAME_AZ, NAME_ZA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    mediaItems: List<MediaItem>,
    isLoading: Boolean,
    optimizationPreference: String = OAuthManager.OPT_ASK,
    aiOrgEnabled: Boolean = false,
    onBackClick: () -> Unit,
    onMoveToVault: (List<MediaItem>) -> Unit,
    onStartTransfer: (TransferMode, Boolean, OrganizationMode, List<MediaItem>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedMode by remember { mutableStateOf(TransferMode.MOVE) }
    var batchSize by remember { mutableIntStateOf(100) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Images", "Videos")

    var showQualityDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    var sortBy by remember { mutableStateOf(SortBy.NEWEST) }
    var minSizeMB by remember { mutableStateOf("") }
    var maxSizeMB by remember { mutableStateOf("") }

    var selectedOrgMode by remember { mutableStateOf(OrganizationMode.NONE) }

    val gridState = rememberLazyGridState()

    val filteredMediaItems = remember(mediaItems, selectedTabIndex, sortBy, minSizeMB, maxSizeMB) {
        val typeFiltered = if (selectedTabIndex == 0) {
            mediaItems.filter { it.mimeType.startsWith("image/") }
        } else {
            mediaItems.filter { it.mimeType.startsWith("video/") }
        }

        val minSize = minSizeMB.toDoubleOrNull()?.let { it * 1024 * 1024 } ?: 0.0
        val maxSize = maxSizeMB.toDoubleOrNull()?.let { it * 1024 * 1024 } ?: Double.MAX_VALUE

        val sizeFiltered = typeFiltered.filter { it.sizeBytes >= minSize && it.sizeBytes <= maxSize }

        when (sortBy) {
            SortBy.NEWEST -> sizeFiltered.sortedByDescending { it.creationTime }
            SortBy.OLDEST -> sizeFiltered.sortedBy { it.creationTime }
            SortBy.SIZE_DESC -> sizeFiltered.sortedByDescending { it.sizeBytes }
            SortBy.SIZE_ASC -> sizeFiltered.sortedBy { it.sizeBytes }
            SortBy.NAME_AZ -> sizeFiltered.sortedBy { it.filename }
            SortBy.NAME_ZA -> sizeFiltered.sortedByDescending { it.filename }
        }
    }

    val totalSelectedCount = selectedIds.size
    val currentTabSelectedCount = filteredMediaItems.count { it.id in selectedIds }
    val allInTabSelected = filteredMediaItems.isNotEmpty() && currentTabSelectedCount == filteredMediaItems.size

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Transfer Options", fontWeight = FontWeight.Bold) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select how you want to move your photos:")
                    
                    if (aiOrgEnabled) {
                        Text("AI Organization", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedOrgMode = OrganizationMode.NONE }) {
                            RadioButton(selected = selectedOrgMode == OrganizationMode.NONE, onClick = { selectedOrgMode = OrganizationMode.NONE })
                            Text("Original Library (No Sorting)", fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedOrgMode = OrganizationMode.BY_DATE }) {
                            RadioButton(selected = selectedOrgMode == OrganizationMode.BY_DATE, onClick = { selectedOrgMode = OrganizationMode.BY_DATE })
                            Text("Group by Date (e.g. Aug 2026)", fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedOrgMode = OrganizationMode.BY_CONTENT }) {
                            RadioButton(selected = selectedOrgMode == OrganizationMode.BY_CONTENT, onClick = { selectedOrgMode = OrganizationMode.BY_CONTENT })
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
            Column {
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
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onBackground
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
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort & Filter",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = {
                            val currentTabIds = filteredMediaItems.map { it.id }.toSet()
                            selectedIds = if (allInTabSelected) {
                                selectedIds - currentTabIds
                            } else {
                                selectedIds + currentTabIds
                            }
                        }) {
                            Text(
                                text = if (allInTabSelected) "None" else "All",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            }
                        )
                    }
                }
            }
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
                        Text("Batch Size", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { 
                            val itemsToTransfer = mediaItems.filter { it.id in selectedIds }
                            if (optimizationPreference == OAuthManager.OPT_ASK || aiOrgEnabled) {
                                showQualityDialog = true
                            } else {
                                val isCompressed = optimizationPreference == OAuthManager.OPT_YES
                                onStartTransfer(selectedMode, isCompressed, selectedOrgMode, itemsToTransfer)
                            }
                        },
                        enabled = totalSelectedCount > 0,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            "Migrate ($totalSelectedCount)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { 
                            val items = mediaItems.filter { it.id in selectedIds }
                            onMoveToVault(items)
                        },
                        enabled = totalSelectedCount > 0,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vault", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        val currentTabIds = filteredMediaItems.take(batchSize).map { it.id }.toSet()
                        selectedIds = selectedIds + currentTabIds
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Select Next $batchSize", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && filteredMediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumLoader()
                }
            } else if (filteredMediaItems.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No ${tabTitles[selectedTabIndex]} Found", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Scroll) {
                                        val scrollAmount = event.changes.first().scrollDelta.y
                                        gridState.dispatchRawDelta(scrollAmount * 150f)
                                    }
                                }
                            }
                        },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMediaItems, key = { it.id }) { item ->
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

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Sort & Filter", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                Text("Sort By", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Column {
                    SortOptionRow("Newest First", SortBy.NEWEST, sortBy) { sortBy = it }
                    SortOptionRow("Oldest First", SortBy.OLDEST, sortBy) { sortBy = it }
                    SortOptionRow("Size: Largest First", SortBy.SIZE_DESC, sortBy) { sortBy = it }
                    SortOptionRow("Size: Smallest First", SortBy.SIZE_ASC, sortBy) { sortBy = it }
                    SortOptionRow("Name: A to Z", SortBy.NAME_AZ, sortBy) { sortBy = it }
                    SortOptionRow("Name: Z to A", SortBy.NAME_ZA, sortBy) { sortBy = it }
                }

                HorizontalDivider()

                Text("Size Filter (MB)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minSizeMB,
                        onValueChange = { minSizeMB = it },
                        label = { Text("Min MB") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxSizeMB,
                        onValueChange = { maxSizeMB = it },
                        label = { Text("Max MB") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun SortOptionRow(label: String, option: SortBy, selected: SortBy, onSelect: (SortBy) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(option) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected == option, onClick = { onSelect(option) })
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 15.sp)
    }
}
