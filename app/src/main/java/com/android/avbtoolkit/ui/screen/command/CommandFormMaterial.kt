package com.android.avbtoolkit.ui.screen.command

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.avbtoolkit.AvbArg
import com.android.avbtoolkit.AvbArgType
import com.android.avbtoolkit.AvbCommand
import com.android.avbtoolkit.AvbExecutor
import com.android.avbtoolkit.R
import kotlinx.coroutines.launch

/**
 * Material 3 form for a single avbtool command. Mirrors the Miuix form's
 * grouping (input args / dropdowns / switches) with Material components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommandFormMaterial(
    command: AvbCommand,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fdToPathCache = remember(command.id) { mutableMapOf<String, Int>() }

    var values by remember(command.id) {
        mutableStateOf(command.args.associate { it.key to "" })
    }
    var running by remember { mutableStateOf(false) }
    var stdout by remember { mutableStateOf("") }
    var stderr by remember { mutableStateOf("") }
    var exitCode by remember { mutableStateOf<Int?>(null) }
    var editingArg by remember { mutableStateOf<AvbArg?>(null) }

    fun releaseFds() {
        fdToPathCache.values.forEach { AvbExecutor.releaseFd(it) }
        fdToPathCache.clear()
    }

    val outKeys = setOf("--output", "--output_vbmeta_image", "--vbmeta_image", "--pkmd", "--output_pubkey", "--misc_image")

    fun run() {
        AvbExecutor.ensureStarted(context)
        running = true
        exitCode = null
        stdout = ""
        stderr = ""
        scope.launch {
            try {
                val argv = mutableListOf(command.id)
                val flags = mutableListOf<String>()
                for (arg in command.args) {
                    val raw = values[arg.key].orEmpty()
                    when (arg.type) {
                        AvbArgType.BOOL -> if (raw == "true") flags.add(arg.key)
                        else -> if (raw.isNotBlank()) {
                            argv.add(arg.key)
                            if (arg.type == AvbArgType.FILE) {
                                if (raw.startsWith("content://")) {
                                    val fd = if (arg.key in outKeys) {
                                        AvbExecutor.openFdWrite(Uri.parse(raw))
                                    } else {
                                        AvbExecutor.openFdRead(Uri.parse(raw))
                                    }
                                    fdToPathCache[arg.key] = fd
                                    argv.add("/saf/fd/$fd")
                                } else {
                                    argv.add(raw)
                                }
                            } else {
                                argv.add(raw)
                            }
                        }
                    }
                }
                val result = AvbExecutor.run(argv + flags)
                stdout = result.stdout
                stderr = result.stderr
                exitCode = result.exitCode
            } finally {
                releaseFds()
                running = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(command.titleRes)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                ) {
                    Text(
                        text = stringResource(command.descriptionRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            val inputArgs = command.args.filter {
                it.type != AvbArgType.BOOL && it.type != AvbArgType.ALGO && it.type != AvbArgType.HASH
            }
            if (inputArgs.isNotEmpty()) {
                item { MaterialGroupTitle(R.string.command_section_image_configs) }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                    ) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            inputArgs.forEach { arg ->
                                when (arg.type) {
                                    AvbArgType.FILE -> MaterialFileField(
                                        arg = arg,
                                        value = values[arg.key].orEmpty(),
                                        onEdit = { editingArg = arg },
                                    )
                                    else -> OutlinedTextField(
                                        value = values[arg.key].orEmpty(),
                                        onValueChange = { v -> values = values + (arg.key to v) },
                                        label = { Text(arg.key) },
                                        placeholder = {
                                            arg.choices?.firstOrNull()?.let { Text(it) }
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val dropdownArgs = command.args.filter {
                it.type == AvbArgType.ALGO || it.type == AvbArgType.HASH
            }
            if (dropdownArgs.isNotEmpty()) {
                item { MaterialGroupTitle(R.string.command_section_key_configs) }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                    ) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            dropdownArgs.forEach { arg ->
                                val choices = arg.choices ?: emptyList()
                                MaterialDropdownField(
                                    arg = arg,
                                    choices = choices,
                                    value = values[arg.key].orEmpty(),
                                    onValue = { v -> values = values + (arg.key to v) },
                                )
                            }
                        }
                    }
                }
            }

            val switchArgs = command.args.filter { it.type == AvbArgType.BOOL }
            if (switchArgs.isNotEmpty()) {
                item { MaterialGroupTitle(R.string.command_section_options) }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            switchArgs.forEach { arg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = arg.key,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Switch(
                                        checked = values[arg.key] == "true",
                                        onCheckedChange = { on ->
                                            values = values + (arg.key to on.toString())
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = ::run,
                    enabled = !running,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                ) {
                    Text(stringResource(if (running) R.string.command_running else R.string.command_run))
                }
            }

            if (stdout.isNotBlank() || stderr.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(
                                    when {
                                        exitCode == null -> R.string.command_result_running
                                        exitCode == 0 -> R.string.command_result_success
                                        else -> R.string.command_result_failed
                                    }
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (exitCode == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                            )
                            SelectionContainer {
                                Column(Modifier.padding(top = 8.dp)) {
                                    if (stdout.isNotBlank()) {
                                        Text(stdout, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (stderr.isNotBlank()) {
                                        Text(
                                            stderr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FILE 参数编辑弹窗（手填路径 / 选文件 / 选目录+文件名）
        editingArg?.let { arg ->
            ArgPathDialog(
                title = arg.key,
                isOutput = arg.key in outKeys,
                initialValue = values[arg.key].orEmpty(),
                onConfirm = { v ->
                    values = values + (arg.key to v)
                    editingArg = null
                },
                onDismiss = { editingArg = null },
            )
        }
    }
}

@Composable
private fun MaterialGroupTitle(@androidx.annotation.StringRes res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialDropdownField(
    arg: AvbArg,
    choices: List<String>,
    value: String,
    onValue: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = value.ifBlank { choices.firstOrNull().orEmpty() },
            onValueChange = {},
            readOnly = true,
            label = { Text(arg.key) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice) },
                    onClick = {
                        onValue(choice)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialFileField(
    arg: AvbArg,
    value: String,
    onEdit: () -> Unit,
) {
    OutlinedTextField(
        value = if (value.isBlank()) "" else resolveDisplayName(LocalContext.current, value),
        onValueChange = {},
        readOnly = true,
        label = { Text(arg.key) },
        placeholder = { Text(stringResource(R.string.command_choose_file)) },
        trailingIcon = {
            androidx.compose.material3.TextButton(onClick = onEdit) {
                Text(stringResource(R.string.command_choose_file))
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}