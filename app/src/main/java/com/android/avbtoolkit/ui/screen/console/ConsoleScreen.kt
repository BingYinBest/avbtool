package com.android.avbtoolkit.ui.screen.console

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import com.android.avbtoolkit.AvbExecutor
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
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * AVB terminal console: an EmulatorView bound to [AvbTermSession].
 * Lines typed here are run through the bundled avbtool (see
 * [AvbTermSession.parseCommand]).
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

    DisposableEffect(Unit) {
        onDispose { session.finish() }
    }

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
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    EmulatorView(ctx, session, ctx.resources.displayMetrics).apply {
                        setTextSize(12)
                        setBackKeyCharacter(0x7f)
                        isFocusable = true
                        isFocusableInTouchMode = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        // EmulatorView consumes touch events (onTouchEvent
                        // returns true), so setOnClickListener never fires.
                        // Focus + show the IME on any touch instead.
                        setOnTouchListener { v, _ ->
                            v.requestFocus()
                            val imm = v.context.getSystemService(
                                android.content.Context.INPUT_METHOD_SERVICE
                            ) as? android.view.inputmethod.InputMethodManager
                            imm?.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                            false
                        }
                    }
                },
                update = { view ->
                    if (view.termSession !== session) {
                        view.attachSession(context, session)
                    }
                    view.requestFocus()
                    val imm = view.context.getSystemService(
                        android.content.Context.INPUT_METHOD_SERVICE
                    ) as? android.view.inputmethod.InputMethodManager
                    imm?.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}