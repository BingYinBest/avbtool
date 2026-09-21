package com.android.avbtoolkit.ui.screen.command

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.avbtoolkit.AvbArg
import com.android.avbtoolkit.AvbArgType
import com.android.avbtoolkit.AvbCommand
import com.android.avbtoolkit.AvbExecutor
import com.android.avbtoolkit.R
import com.android.avbtoolkit.ui.component.miuix.effect.BgEffectBackground
import com.android.avbtoolkit.ui.LocalUiMode
import com.android.avbtoolkit.ui.UiMode
import com.android.avbtoolkit.ui.component.miuix.EditText
import com.android.avbtoolkit.ui.theme.LocalEnableBlur
import com.android.avbtoolkit.ui.util.BlurredBar
import com.android.avbtoolkit.ui.util.rememberBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Form for a single avbtool command. Arguments are grouped into cards:
 * file pickers, text fields, dropdowns and switches. Builds argv and
 * runs via [AvbExecutor]. Renders Miuix or Material components to match
 * the active UI mode.
 */
@Composable
fun CommandScreen(
    command: AvbCommand,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> CommandFormMiuix(command, onBack, modifier)
        UiMode.Material -> CommandFormMaterial(command, onBack, modifier)
    }
}

@Composable
private fun CommandFormMiuix(
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

    // 输出类文件参数：需要 rw 打开
    val outKeys = setOf("--output", "--output_vbmeta_image", "--vbmeta_image", "--pkmd", "--output_pubkey", "--misc_image")

    // 输入类参数：值可能是 SAF content:// uri，也可能是用户手填的路径
    // （如 /data/local/tmp/boot.img）。手填路径时直接传给 avbtool，由
    // Python 侧读写；content:// 则通过 SAF fd 伪路径访问。
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

    CommandScaffold(command, onBack, modifier) {
        val scrollBehavior = MiuixScrollBehavior()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    MiuixText(
                        text = stringResource(command.descriptionRes),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // 输入类参数（文件/文本/数值）分组
            val inputArgs = command.args.filter {
                it.type != AvbArgType.BOOL &&
                    it.type != AvbArgType.ALGO &&
                    it.type != AvbArgType.HASH
            }
            if (inputArgs.isNotEmpty()) {
                item {
                    GroupTitle(R.string.command_section_image_configs)
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            inputArgs.forEach { arg ->
                                when (arg.type) {
                                    AvbArgType.FILE -> FilePreference(
                                        arg = arg,
                                        value = values[arg.key].orEmpty(),
                                        onEdit = { editingArg = arg },
                                    )
                                    else -> EditText(
                                        title = arg.key,
                                        value = values[arg.key].orEmpty(),
                                        onValueChange = { v ->
                                            values = values + (arg.key to v)
                                        },
                                        textHint = arg.choices?.joinToString("/").orEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 下拉类参数
            val dropdownArgs = command.args.filter {
                it.type == AvbArgType.ALGO || it.type == AvbArgType.HASH
            }
            if (dropdownArgs.isNotEmpty()) {
                item {
                    GroupTitle(R.string.command_section_key_configs)
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            dropdownArgs.forEach { arg ->
                                val choices = arg.choices ?: emptyList()
                                OverlayDropdownPreference(
                                    title = arg.key,
                                    summary = values[arg.key].orEmpty().ifBlank { choices.firstOrNull().orEmpty() },
                                    items = choices,
                                    selectedIndex = choices.indexOf(values[arg.key]).coerceAtLeast(0),
                                    onSelectedIndexChange = { idx ->
                                        values = values + (arg.key to choices[idx])
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 开关类参数
            val switchArgs = command.args.filter { it.type == AvbArgType.BOOL }
            if (switchArgs.isNotEmpty()) {
                item {
                    GroupTitle(R.string.command_section_options)
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            switchArgs.forEach { arg ->
                                SwitchPreference(
                                    title = arg.key,
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

            item {
                Button(
                    onClick = ::run,
                    enabled = !running,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    MiuixText(
                        stringResource(
                            if (running) R.string.command_running else R.string.command_run
                        )
                    )
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
                    ResultCard(exitCode, stdout, stderr)
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
private fun GroupTitle(@androidx.annotation.StringRes res: Int) {
    MiuixText(
        text = stringResource(res),
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun FilePreference(
    arg: AvbArg,
    value: String,
    onEdit: () -> Unit,
) {
    ArrowPreference(
        title = arg.key,
        summary = if (value.isBlank()) {
            stringResource(R.string.command_choose_file)
        } else {
            resolveDisplayName(LocalContext.current, value)
        },
        startAction = {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
            )
        },
        onClick = onEdit,
    )
}

@Composable
private fun ResultCard(exitCode: Int?, stdout: String, stderr: String) {
    val ok = exitCode == 0
    Card(Modifier.fillMaxWidth()) {
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

@Composable
private fun CommandScaffold(
    command: AvbCommand,
    onBack: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
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
                    title = stringResource(command.titleRes),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout),
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            content()
        }
    }
}