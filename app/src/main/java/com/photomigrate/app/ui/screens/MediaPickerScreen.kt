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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.auth.OAuthManager
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.OrganizationMode
import com.photomigrate.app.data.model.SortBy
import com.photomigrate.app.data.model.TransferMode
import com.photomigrate.app.ui.components.GlassCard
import com.photomigrate.app.ui.components.MediaItemGridCard
import com.photomigrate.app.ui.components.PremiumLoader
import com.photomigrate.app.ui.theme.CredNeonPink
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    mediaItems: List<MediaItem>,
    isLoading: Boolean,
    optimizationPreference: String = OAuthManager.OPT_ASK,
    defaultSortBy: SortBy = SortBy.NEWEST,
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
    
    var sortBy by remember { mutableStateOf(defaultSortBy) }
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
        var isStorageSaverSelected by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showQualityDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0D0E19),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2E42))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Transfer Options",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Choose how you want to organize and save your photos.",
                            fontSize = 12.sp,
                            color = Color(0xFF9394A5)
                        )
                    }
                    IconButton(onClick = { showQualityDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Section: Organization
                if (aiOrgEnabled) {
                    Text(
                        text = "Organization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OptionCard(
                            title = "Original Library",
                            subtitle = "Keep the same folder structure as your source.",
                            icon = Icons.Default.Folder,
                            isSelected = selectedOrgMode == OrganizationMode.NONE,
                            onClick = { selectedOrgMode = OrganizationMode.NONE }
                        )
                        OptionCard(
                            title = "Group by Date",
                            subtitle = "Organize by date (e.g. Aug 2026).",
                            icon = Icons.Default.CalendarToday,
                            isSelected = selectedOrgMode == OrganizationMode.BY_DATE,
                            onClick = { selectedOrgMode = OrganizationMode.BY_DATE }
                        )
                        OptionCard(
                            title = "Smart AI Grouping",
                            subtitle = "Group by people, places, events and more.",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = selectedOrgMode == OrganizationMode.BY_CONTENT,
                            onClick = { selectedOrgMode = OrganizationMode.BY_CONTENT }
                        )
                    }
                }

                // Section: Quality
                Text(
                    text = "Quality",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OptionCard(
                        title = "Original Quality",
                        subtitle = "Keep full resolution (larger size).",
                        icon = Icons.Default.RadioButtonChecked,
                        isSelected = !isStorageSaverSelected,
                        onClick = { isStorageSaverSelected = false }
                    )
                    OptionCard(
                        title = "Storage Saver",
                        subtitle = "Compress to save space.",
                        icon = Icons.Default.Speed,
                        isSelected = isStorageSaverSelected,
                        onClick = { isStorageSaverSelected = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                com.photomigrate.app.ui.components.CredButton(
                    text = "Continue",
                    onClick = {
                        showQualityDialog = false
                        val itemsToTransfer = mediaItems.filter { it.id in selectedIds }
                        onStartTransfer(selectedMode, isStorageSaverSelected, selectedOrgMode, itemsToTransfer)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
        ) {
            if (isLoading && filteredMediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumLoader()
                }
            } else if (filteredMediaItems.isEmpty()) {
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
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 16.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
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
fun OptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (isSelected) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = activeColor,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )
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
