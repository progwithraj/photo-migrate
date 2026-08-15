package com.photomigrate.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photomigrate.app.data.model.AccountRole
import com.photomigrate.app.data.model.GoogleAccount
import com.photomigrate.app.data.model.MediaItem
import com.photomigrate.app.data.model.SyncStatus
import com.photomigrate.app.data.model.TransferLog
import com.photomigrate.app.ui.theme.*

@Composable
fun MeshBackground() {
    val isDark = isSystemInDarkTheme()
    val themeColors = LocalThemeColors.current
    val baseColor = MaterialTheme.colorScheme.background
    
    val accent1 = if (isDark) themeColors.primary.copy(alpha = 0.5f) else themeColors.mesh1
    val accent2 = if (isDark) themeColors.primary.copy(alpha = 0.3f) else themeColors.mesh2
    val accent3 = if (isDark) themeColors.primary.copy(alpha = 0.2f) else themeColors.mesh3

    Box(modifier = Modifier.fillMaxSize().background(baseColor)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
            drawCircle(
                color = accent1,
                radius = size.width,
                center = Offset(size.width * 0.2f, size.height * 0.2f),
                alpha = 0.4f
            )
            drawCircle(
                color = accent2,
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.8f, size.height * 0.5f),
                alpha = 0.3f
            )
            drawCircle(
                color = accent3,
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.3f, size.height * 0.8f),
                alpha = 0.3f
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(0.5.dp, GlassCardBorder, RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun AccountCard(
    account: GoogleAccount,
    isSelectedAsSource: Boolean,
    isSelectedAsDest: Boolean,
    onSelectRole: (AccountRole) -> Unit,
    onRemoveAccount: () -> Unit
) {
    val usedGb = String.format("%.2f", account.usedStorageBytes.toDouble() / (1024 * 1024 * 1024))
    val totalGb = String.format("%.0f", account.totalStorageBytes.toDouble() / (1024 * 1024 * 1024))
    val percentage = account.usedPercentage

    val storageColor = when {
        percentage >= 0.9f -> ErrorRed
        percentage >= 0.75f -> WarningAmber
        else -> MaterialTheme.colorScheme.primary
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile Picture with ring
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .border(2.dp, if (isSelectedAsSource || isSelectedAsDest) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (account.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = account.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.email.take(1).uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName.ifEmpty { account.email.substringBefore("@") },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = account.email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRemoveAccount,
                modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage Usage
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$usedGb GB of $totalGb GB used",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = storageColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = percentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = storageColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Role Selector - iOS segment style
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp).height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleButton(
                    label = "Source",
                    isSelected = isSelectedAsSource,
                    onClick = { onSelectRole(AccountRole.SOURCE) },
                    modifier = Modifier.weight(1f)
                )
                RoleButton(
                    label = "Destination",
                    isSelected = isSelectedAsDest,
                    onClick = { onSelectRole(AccountRole.DESTINATION) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RoleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun MediaItemGridCard(
    item: MediaItem,
    onToggleSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggleSelect() }
    ) {
        AsyncImage(
            model = item.thumbnailUrl ?: item.baseUrl,
            contentDescription = item.filename,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay status badge
        if (item.isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.TopEnd
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text("VIDEO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sync Status Badge
        if (item.status != SyncStatus.IDLE) {
            val (icon, color) = when (item.status) {
                SyncStatus.COMPLETED, SyncStatus.TRASHED_FROM_SOURCE -> Icons.Default.Done to SuccessGreen
                SyncStatus.FAILED -> Icons.Default.Error to ErrorRed
                else -> Icons.Default.Sync to MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(color, CircleShape)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.status.name,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun LogItemRow(log: TransferLog) {
    val color = if (log.isError) ErrorRed else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = log.message,
            fontSize = 12.sp,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
