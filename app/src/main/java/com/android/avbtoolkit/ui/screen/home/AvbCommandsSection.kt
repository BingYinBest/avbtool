package com.android.avbtoolkit.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.avbtoolkit.AvbCommand
import com.android.avbtoolkit.AvbCommands
import com.android.avbtoolkit.HomeSegment
import com.android.avbtoolkit.R

/**
 * Frequency-ordered avbtool command list grouped by [HomeSegment], plus a
 * console entry. Rendered inside the home pager for both UI modes.
 */
@Composable
fun AvbCommandsSection(
    onOpenCommand: (String) -> Unit,
    onOpenConsole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = false, onClick = onOpenConsole)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.nav_console), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.nav_console_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HomeSegment.entries.forEach { segment ->
            val commands = AvbCommands.all.filter { it.group == segment }
            if (commands.isEmpty()) return@forEach
            Text(
                text = stringResource(segment.labelRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
            commands.forEach { command ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = false, onClick = { onOpenCommand(command.id) })
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = commandIcon(command.id),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(command.titleRes), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(command.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun commandIcon(commandId: String): ImageVector = when (commandId) {
    "add_hash_footer" -> Icons.AutoMirrored.Filled.NoteAdd
    "add_hashtree_footer" -> Icons.Filled.AccountTree
    "info_image" -> Icons.Filled.Info
    "erase_footer" -> Icons.Filled.DeleteSweep
    "resize_image" -> Icons.Filled.PhotoSizeSelectLarge
    "extract_vbmeta_image" -> Icons.Filled.Unarchive
    "print_partition_digests" -> Icons.Filled.Fingerprint
    "calculate_vbmeta_digest" -> Icons.Filled.Calculate
    "verify_image" -> Icons.Filled.Verified
    "zero_hashtree" -> Icons.Filled.LayersClear
    "calculate_kernel_cmdline" -> Icons.Filled.KeyboardCommandKey
    "extract_public_key" -> Icons.Filled.Key
    "extract_public_key_digest" -> Icons.Filled.Output
    "append_vbmeta_image" -> Icons.Filled.AddCircle
    "set_ab_metadata" -> Icons.Filled.Storage
    "make_vbmeta_image" -> Icons.Filled.DiscFull
    "make_certificate" -> Icons.Filled.VerifiedUser
    "make_cert_permanent_attributes" -> Icons.Filled.Lock
    "make_cert_metadata" -> Icons.Filled.VerifiedUser
    "make_cert_unlock_credential" -> Icons.Filled.LockOpen
    else -> Icons.Filled.Info
}
