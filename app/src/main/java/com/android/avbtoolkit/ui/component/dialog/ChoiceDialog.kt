package com.android.avbtoolkit.ui.component.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.avbtoolkit.ui.LocalUiMode
import com.android.avbtoolkit.ui.UiMode
import androidx.compose.foundation.layout.Column
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Single-select choice list. Renders a Miuix [OverlayDialog] in Miuix mode
 * and a Material3 [AlertDialog] in Material mode, so command forms keep a
 * consistent look in each UI mode.
 */
@Composable
fun ChoiceListDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> {
            OverlayDialog(
                show = true,
                title = title,
                onDismissRequest = onDismiss,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (option == selected) {
                                MiuixIcon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                )
                            }
                            MiuixText(
                                text = option,
                                fontSize = MiuixTheme.textStyles.body1.fontSize,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        UiMode.Material -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(option) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(selected = option == selected, onClick = null)
                                Text(text = option, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}
