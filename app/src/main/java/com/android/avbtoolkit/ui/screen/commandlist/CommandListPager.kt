package com.android.avbtoolkit.ui.screen.commandlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.avbtoolkit.AvbCatalog
import com.android.avbtoolkit.AvbCategory
import com.android.avbtoolkit.AvbCommand
import com.android.avbtoolkit.R
import com.android.avbtoolkit.ui.LocalUiMode
import com.android.avbtoolkit.ui.UiMode
import com.android.avbtoolkit.ui.navigation3.Navigator
import com.android.avbtoolkit.ui.navigation3.Route
import com.android.avbtoolkit.ui.theme.LocalEnableBlur
import com.android.avbtoolkit.ui.util.BlurredBar
import com.android.avbtoolkit.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * One of the three command list tabs (Image / vbmeta / Others).
 * Miuix mode renders Miuix cards, Material mode renders Material
 * cards. Rows open the command form screen.
 */
@Composable
fun CommandListPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    category: AvbCategory,
    isCurrentPage: Boolean,
) {
    if (!isCurrentPage) return
    val commands = remember(category) { AvbCatalog.forCategory(category) }
    val onOpenCommand: (String) -> Unit = { id -> navigator.push(Route.Command(id)) }

    when (LocalUiMode.current) {
        UiMode.Miuix -> CommandListPagerMiuix(category, commands, onOpenCommand, bottomInnerPadding)
        UiMode.Material -> CommandListPagerMaterial(category, commands, onOpenCommand, bottomInnerPadding)
    }
}

@Composable
private fun CommandListPagerMiuix(
    category: AvbCategory,
    commands: List<AvbCommand>,
    onOpenCommand: (String) -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    color = barColor,
                    title = stringResource(category.labelRes),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .padding(bottom = bottomInnerPadding),
                contentPadding = innerPadding,
            ) {
                items(commands, key = { it.id }) { command ->
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        CommandCardMiuix(command, onOpenCommand)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandCardMiuix(command: AvbCommand, onOpenCommand: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCommand(command.id) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = commandIcon(command.id),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                MiuixText(
                    stringResource(command.titleRes),
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MiuixText(
                    stringResource(command.descriptionRes),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CommandListPagerMaterial(
    category: AvbCategory,
    commands: List<AvbCommand>,
    onOpenCommand: (String) -> Unit,
    bottomInnerPadding: Dp,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(category.labelRes)) }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(bottom = bottomInnerPadding),
            contentPadding = innerPadding,
        ) {
            items(commands, key = { it.id }) { command ->
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { onOpenCommand(command.id) },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = commandIcon(command.id),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                        ) {
                            Text(
                                stringResource(command.titleRes),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                stringResource(command.descriptionRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
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
    "generate_test_image" -> Icons.Filled.PhotoSizeSelectLarge
    "update_partition_descriptor" -> Icons.Filled.Edit
    "resign_image" -> Icons.Filled.Refresh
    else -> Icons.Filled.Info
}