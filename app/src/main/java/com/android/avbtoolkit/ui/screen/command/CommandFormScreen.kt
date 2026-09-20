package com.android.avbtoolkit.ui.screen.command

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.android.avbtoolkit.AvbArg
import com.android.avbtoolkit.AvbArgType
import com.android.avbtoolkit.AvbCommand
import com.android.avbtoolkit.AvbExecutor
import com.android.avbtoolkit.R
import com.android.avbtoolkit.ui.component.liquid.LiquidGlassBackground
import com.android.avbtoolkit.ui.component.liquid.LiquidGlassTopBar
import com.android.avbtoolkit.ui.component.liquid.liquidGlassLayer
import com.android.avbtoolkit.ui.component.liquid.rememberLiquidGlass
import com.android.avbtoolkit.ui.component.miuix.EditText
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * Form screen for a single avbtool command: renders every declared
 * argument as an editable row (SAF file picker for FILE, text field for
 * TEXT/UINT/FLAGS, switch for BOOL, dropdown for choice lists), builds
 * the argv and runs the bundled avbtool via [AvbExecutor].
 */
@Composable
fun CommandScreen(
    command: AvbCommand,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Keep fds open until the run completes.
    val fdToPathCache = remember(command.id) { mutableMapOf<String, Int>() }

    fun releaseFds() {
        fdToPathCache.values.forEach { AvbExecutor.releaseFd(it) }
        fdToPathCache.clear()
    }

    // argument key -> current value (files stored as content uri string)
    var values by remember(command.id) {
        mutableStateOf(command.args.associate { it.key to "" })
    }
    var running by remember { mutableStateOf(false) }
    var stdout by remember { mutableStateOf("") }
    var stderr by remember { mutableStateOf("") }
    var exitCode by remember { mutableStateOf<Int?>(null) }
    // Which FILE arg is waiting for a document pick.
    var pendingFileArg by remember { mutableStateOf<AvbArg?>(null) }

    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingFileArg?.let { arg ->
                values = values + (arg.key to uri.toString())
                pendingFileArg = null
            }
        }
    }

    fun pickFile(arg: AvbArg) {
        pendingFileArg = arg
        openFile.launch(arrayOf("*/*"))
    }

    // argv: only include non-empty, non-flag values; flags are added when checked.
    fun buildArgv(): List<String> {
        val argv = mutableListOf(command.id)
        val flags = mutableListOf<String>()
        for (arg in command.args) {
            val raw = values[arg.key].orEmpty()
            when (arg.type) {
                AvbArgType.BOOL -> if (raw == "true") flags.add(arg.key)
                else -> if (raw.isNotBlank()) {
                    argv.add(arg.key)
                    argv.add(
                        if (arg.type == AvbArgType.FILE) {
                            // content uri -> SAF fd pseudo-path
                            val fd = AvbExecutor.openFd(Uri.parse(raw))
                            fdToPathCache[arg.key] = fd
                            "/saf/fd/$fd"
                        } else {
                            raw
                        }
                    )
                }
            }
        }
        return argv + flags
    }


    fun run() {
        AvbExecutor.ensureStarted(context)
        running = true
        exitCode = null
        stdout = ""
        stderr = ""
        scope.launch {
            try {
                val result = AvbExecutor.run(buildArgv())
                stdout = result.stdout
                stderr = result.stderr
                exitCode = result.exitCode
            } finally {
                releaseFds()
                running = false
            }
        }
    }

    val liquidGlass = rememberLiquidGlass()
    BoxWithGlass(command, onBack, liquidGlass) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(12.dp)) {
                        MiuixText(
                            text = stringResource(command.descriptionRes),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                }
            }

            command.args.chunked(1).forEach { chunk ->
                val arg = chunk.first()
                item(key = arg.key) {
                    when (arg.type) {
                        AvbArgType.BOOL -> SwitchPreference(
                            title = arg.key,
                            checked = values[arg.key] == "true",
                            onCheckedChange = { on ->
                                values = values + (arg.key to on.toString())
                            },
                        )

                        AvbArgType.FILE -> ArgFileRow(
                            value = values[arg.key].orEmpty(),
                            onPick = { pickFile(arg) },
                        )

                        else -> EditText(
                            title = arg.key,
                            value = values[arg.key].orEmpty(),
                            onValueChange = { v -> values = values + (arg.key to v) },
                            textHint = arg.choices?.joinToString("/").orEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = ::run,
                    enabled = !running,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    MiuixText(stringResource(
                        if (running) R.string.command_running else R.string.command_run
                    ))
                }
                MiuixText(
                    text = "avbtool ${command.id}",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (stdout.isNotBlank() || stderr.isNotBlank()) {
                item {
                    ResultCard(
                        exitCode = exitCode,
                        stdout = stdout,
                        stderr = stderr,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxWithGlass(
    command: AvbCommand,
    onBack: () -> Unit,
    liquidGlass: LayerBackdrop,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(
            Modifier.liquidGlassLayer(liquidGlass).fillMaxSize()
        ) {
            LiquidGlassBackground()
        }
        MiuixScaffold(
            topBar = {
                LiquidGlassTopBar(
                    backdrop = liquidGlass,
                    title = stringResource(command.titleRes),
                    onBack = onBack,
                )
            },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(Modifier.padding(innerPadding)) {
                content()
            }
        }
    }
}

@Composable
private fun ArgFileRow(
    value: String,
    onPick: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                MiuixText(
                    text = if (value.isBlank()) {
                        stringResource(R.string.command_choose_file)
                    } else {
                        value.substringAfterLast('/')
                    },
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                )
                MiuixText(
                    text = if (value.isBlank()) {
                        "—"
                    } else {
                        value
                    },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(exitCode: Int?, stdout: String, stderr: String) {
    val ok = exitCode == 0
    Card {
        Column(Modifier.padding(12.dp)) {
            MiuixText(
                text = stringResource(
                    when {
                        exitCode == null -> R.string.command_result_running
                        ok -> R.string.command_result_success
                        else -> R.string.command_result_failed
                    }
                ),
                style = MiuixTheme.textStyles.body1,
                color = if (ok) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.error,
            )
            SelectionContainer {
                Column(Modifier.padding(top = 8.dp)) {
                    if (stdout.isNotBlank()) {
                        MiuixText(stdout, style = MiuixTheme.textStyles.body2)
                    }
                    if (stderr.isNotBlank()) {
                        MiuixText(
                            stderr,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}