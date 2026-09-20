package com.android.avbtoolkit.ui.screen.console

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import com.android.avbtoolkit.AvbExecutor
import com.android.avbtoolkit.R
import com.android.avbtoolkit.ui.component.liquid.LiquidGlassBackground
import com.android.avbtoolkit.ui.component.liquid.LiquidGlassTopBar
import com.android.avbtoolkit.ui.component.liquid.liquidGlassLayer
import com.android.avbtoolkit.ui.component.liquid.rememberLiquidGlass
import jackpal.androidterm.emulatorview.EmulatorView
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    val liquidGlass = rememberLiquidGlass()
    Box(modifier.fillMaxSize()) {
        Box(Modifier.liquidGlassLayer(liquidGlass).fillMaxSize()) {
            LiquidGlassBackground()
        }
        MiuixScaffold(
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        LiquidGlassTopBar(
                            backdrop = liquidGlass,
                            title = stringResource(R.string.nav_console),
                            onBack = onBack,
                        )
                    }
                    if (onOpenSettings != null) {
                        top.yukonga.miuix.kmp.basic.IconButton(
                            onClick = onOpenSettings,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.padding(end = 8.dp),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            },
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
        }
    }
}