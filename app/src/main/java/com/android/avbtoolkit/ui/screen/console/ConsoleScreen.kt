package com.android.avbtoolkit.ui.screen.console

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.avbtoolkit.AvbExecutor
import com.android.avbtoolkit.ui.LocalUiMode
import com.android.avbtoolkit.ui.UiMode
import com.android.avbtoolkit.ui.component.miuix.EditText
import com.android.avbtoolkit.ui.theme.LocalEnableBlur
import com.android.avbtoolkit.ui.util.BlurredBar
import com.android.avbtoolkit.ui.util.rememberBlurBackdrop
import com.android.avbtoolkit.R
import jackpal.androidterm.emulatorview.EmulatorView
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Common quick commands shown above the input row in both themes. */
private val QuickCommands = listOf("version", "info_image", "verify_image", "check_mldsa_support")

@Composable
private fun TerminalPanel(session: AvbTermSession) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            EmulatorView(ctx, session, ctx.resources.displayMetrics).apply {
                setTextSize(12)
                setBackKeyCharacter(0x7f)
                isFocusable = false
                setBackgroundColor(0xFF101014.toInt())
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view ->
            if (view.termSession !== session) {
                view.attachSession(context, session)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * AVB terminal console: an EmulatorView bound to [AvbTermSession], plus a
 * command input box. Typing happens in the box at the bottom; tapping it
 * is what summons the IME (the terminal area itself never grabs focus).
 * Renders Miuix or Material chrome to match the active UI mode.
 */
@Composable
fun ConsoleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AvbExecutor.ensureStarted(context)

    val session = remember {
        AvbTermSession(scope).apply {
            setTitle("avbtool console")
        }
    }
    var input by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { session.finish() }
    }

    fun submit() {
        val text = input.trim()
        if (text.isNotEmpty()) {
            session.submitInput(text)
            input = ""
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> ConsoleMiuix(
            session = session,
            input = input,
            onInputChange = { input = it },
            onSubmit = ::submit,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
        UiMode.Material -> ConsoleMaterial(
            session = session,
            input = input,
            onInputChange = { input = it },
            onSubmit = ::submit,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
    }
}

@Composable
private fun ConsoleMiuix(
    session: AvbTermSession,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
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
                    title = stringResource(R.string.nav_console),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    actions = {
                        if (onOpenSettings != null) {
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = MiuixTheme.colorScheme.onBackground,
                                )
                            }
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp),
        ) {
            // Quick command chips (Termux-like convenience row).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
            ) {
                QuickCommands.forEach { cmd ->
                    MiuixTextButton(
                        text = cmd,
                        onClick = { session.submitInput(cmd) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }

            // Terminal panel: rounded dark surface with monospace output.
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 6.dp),
            ) {
                top.yukonga.miuix.kmp.basic.Card(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                        TerminalPanel(session)
                    }
                }
            }

            // Command input row.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditText(
                    title = "",
                    value = input,
                    onValueChange = onInputChange,
                    textHint = stringResource(R.string.terminal_input_hint),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onSubmit,
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.terminal_send),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleMaterial(
    session: AvbTermSession,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MaterialScaffold(
        topBar = {
            MaterialTopAppBar(
                title = { Text(stringResource(R.string.nav_console)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    if (onOpenSettings != null) {
                        androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings),
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
            ) {
                QuickCommands.forEach { cmd ->
                    AssistChip(
                        onClick = { session.submitInput(cmd) },
                        label = { Text(cmd) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101014),
            ) {
                Box(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                    TerminalPanel(session)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text(stringResource(R.string.terminal_input_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onSubmit,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.terminal_send))
                }
            }
        }
    }
}