package com.android.avbtoolkit.ui.screen.command

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.avbtoolkit.R
import com.android.avbtoolkit.ui.LocalUiMode
import com.android.avbtoolkit.ui.UiMode
import com.android.avbtoolkit.ui.component.miuix.EditText
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * Dialog for a FILE argument. The value can be typed by hand, picked via
 * the SAF file picker (input args), or produced by picking a directory
 * and a custom file name (output args, `documents` / CREATE intent).
 * Renders Miuix or Material dialogs to match the active UI mode.
 */
@Composable
fun ArgPathDialog(
    title: String,
    isOutput: Boolean,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ArgPathDialogMiuix(title, isOutput, initialValue, onConfirm, onDismiss)
        UiMode.Material -> ArgPathDialogMaterial(title, isOutput, initialValue, onConfirm, onDismiss)
    }
}

/** Best-effort human readable name for the current value (uri or path). */
fun fileDisplayName(initialValue: String): String {
    val tail = initialValue.substringAfterLast('/').ifBlank { initialValue }
    return try {
        java.net.URLDecoder.decode(tail, "UTF-8")
    } catch (e: Exception) {
        tail
    }
}

/** Query DISPLAY_NAME for a content uri; fall back to the decoded tail. */
fun resolveDisplayName(context: android.content.Context, value: String): String {
    val uri = try {
        Uri.parse(value)
    } catch (e: Exception) {
        return fileDisplayName(value)
    }
    if (uri.scheme != "content") return fileDisplayName(value)
    return try {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { c ->
            if (c.moveToFirst()) c.getString(0)
            else null
        } ?: fileDisplayName(value)
    } catch (e: Exception) {
        fileDisplayName(value)
    }
}

@Composable
private fun ArgPathDialogMiuix(
    title: String,
    isOutput: Boolean,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var text by remember(initialValue) { mutableStateOf(initialValue) }

    // SAF single-file picker (input args and direct replace of output).
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) text = uri.toString()
    }
    // Directory tree picker (output args): combine with the typed file name.
    val pickDir = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val name = text.substringAfterLast('/').ifBlank { "output.img" }
            try {
                val created = android.provider.DocumentsContract.createDocument(
                    context.contentResolver, treeUri,
                    "application/octet-stream", name,
                )
                if (created != null) text = created.toString()
            } catch (e: Exception) {
                text = treeUri.toString() + "/" + name
            }
        }
    }

    WindowDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            EditText(
                title = "",
                value = text,
                onValueChange = { text = it },
                textHint = stringResource(
                    if (isOutput) R.string.file_dialog_hint_filename
                    else R.string.file_dialog_hint_path
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isOutput) {
                    MiuixTextButton(text = stringResource(R.string.file_dialog_pick_dir), onClick = {
                        text = fileDisplayName(text)
                        pickDir.launch(null)
                    })
                } else {
                    MiuixTextButton(text = stringResource(R.string.file_dialog_pick_file), onClick = {
                        pickFile.launch(arrayOf("*/*"))
                    })
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(text) },
                ) {
                    MiuixTextButtonText()
                }
            }
        }
    }
}

@Composable
private fun MiuixTextButtonText() {
    MiuixText(stringResource(android.R.string.ok))
}

@Composable
private fun ArgPathDialogMaterial(
    title: String,
    isOutput: Boolean,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var text by remember(initialValue) { mutableStateOf(initialValue) }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) text = uri.toString()
    }
    val pickDir = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val name = text.substringAfterLast('/').ifBlank { "output.img" }
            try {
                val created = android.provider.DocumentsContract.createDocument(
                    context.contentResolver, treeUri,
                    "application/octet-stream", name,
                )
                if (created != null) text = created.toString()
            } catch (e: Exception) {
                text = treeUri.toString() + "/" + name
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = {
                        Text(
                            stringResource(
                                if (isOutput) R.string.file_dialog_hint_filename
                                else R.string.file_dialog_hint_path
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(text) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isOutput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { pickDir.launch(null) }) {
                            Text(stringResource(R.string.file_dialog_pick_dir))
                        }
                        TextButton(onClick = { text = fileDisplayName(text) }) {
                            Text(stringResource(R.string.file_dialog_pick_file))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { pickFile.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.file_dialog_pick_file))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}