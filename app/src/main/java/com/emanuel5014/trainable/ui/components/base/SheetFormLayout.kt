package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.ResponsiveSize

/**
 * Layout for a bottom sheet that holds a form: the fields scroll, the action buttons stay pinned at the
 * bottom of the sheet.
 *
 * `ModalBottomSheet` already lifts its content above the keyboard, but it can only shrink it: when the
 * keyboard is open there is little room left, and buttons sitting at the end of the scrolling content end
 * up hidden behind the keyboard. Putting them in a [SheetFormFooter] keeps them reachable.
 *
 * ```
 * ModalBottomSheet(...) {
 *     SheetFormLayout {
 *         SheetFormBody { header; fields }
 *         SheetFormFooter { Row { cancel; save } }
 *     }
 * }
 * ```
 */
@Composable
fun SheetFormLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    CompositionLocalProvider(LocalSheetFormScroll provides scrollState) {
        Column(modifier = modifier.fillMaxWidth(), content = content)
    }
}

/** The scrolling part of a [SheetFormLayout]; takes whatever height the pinned footer leaves. */
@Composable
fun ColumnScope.SheetFormBody(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ResponsiveSize.cardPadding,
    spacing: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = LocalSheetFormScroll.current ?: rememberScrollState()
    Column(
        modifier = modifier
            .weight(1f, fill = false)
            .verticalScroll(scrollState)
            .padding(horizontal = horizontalPadding)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/**
 * The buttons of a [SheetFormLayout]. A hairline appears above them while the form still has content
 * hidden below, so it is clear they don't belong to the scrolling part.
 */
@Composable
fun SheetFormFooter(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ResponsiveSize.cardPadding,
    content: @Composable () -> Unit
) {
    val scrollState = LocalSheetFormScroll.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (scrollState?.canScrollForward == true) {
            HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            content()
        }
    }
}

/** True while the on-screen keyboard is showing; lets a sheet drop secondary content to make room. */
@OptIn(ExperimentalLayoutApi::class)
val isKeyboardVisible: Boolean
    @Composable get() = WindowInsets.isImeVisible

private val LocalSheetFormScroll = compositionLocalOf<ScrollState?> { null }
