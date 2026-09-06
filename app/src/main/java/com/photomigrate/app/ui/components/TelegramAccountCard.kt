package com.photomigrate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photomigrate.app.data.model.TelegramAccount

@Composable
fun TelegramAccountCard(
    account: TelegramAccount,
    isSelectedAsDest: Boolean,
    onSelectDest: () -> Unit,
    onRemoveAccount: () -> Unit
) {
    // Telegram Blue
    val tgColor = Color(0xFF0088CC)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelectedAsDest) 2.dp else 0.dp,
                color = if (isSelectedAsDest) tgColor else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(tgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = account.botName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Telegram Vault",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Max 50MB / file",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = tgColor
                        )
                    }
                }

                IconButton(onClick = onRemoveAccount) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Account",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Telegram can only be destination
            Button(
                onClick = onSelectDest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelectedAsDest) tgColor else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelectedAsDest) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSelectedAsDest) "Selected Destination" else "Set as Destination")
            }
        }
    }
}
