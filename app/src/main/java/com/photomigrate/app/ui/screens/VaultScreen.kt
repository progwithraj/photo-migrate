package com.photomigrate.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.db.VaultItemEntity
import com.photomigrate.app.util.VaultManager
import java.io.File

@Composable
fun VaultScreen(
    items: List<VaultItemEntity>,
    vaultManager: VaultManager,
    onDelete: (VaultItemEntity) -> Unit
) {
    var selectedItem by remember { mutableStateOf<VaultItemEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Incognito Vault",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Your private, biometric-protected local storage.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your vault is empty", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items) { item ->
                    VaultItemCard(item, vaultManager) {
                        selectedItem = item
                    }
                }
            }
        }
    }

    if (selectedItem != null) {
        VaultItemViewer(
            item = selectedItem!!,
            vaultManager = vaultManager,
            onClose = { selectedItem = null },
            onDelete = {
                onDelete(selectedItem!!)
                selectedItem = null
            }
        )
    }
}

@Composable
fun VaultItemCard(
    item: VaultItemEntity,
    vaultManager: VaultManager,
    onClick: () -> Unit
) {
    val bitmap = remember(item.id) {
        try {
            val stream = vaultManager.decrypt(File(item.localEncryptedPath))
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.BrokenImage, null, modifier = Modifier.align(Alignment.Center))
        }

        if (item.mimeType.startsWith("video/")) {
            Icon(
                Icons.Default.PlayCircle,
                null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(32.dp)
            )
        }
    }
}

@Composable
fun VaultItemViewer(
    item: VaultItemEntity,
    vaultManager: VaultManager,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val bitmap = remember(item.id) {
            try {
                val stream = vaultManager.decrypt(File(item.localEncryptedPath))
                BitmapFactory.decodeStream(stream)
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}
