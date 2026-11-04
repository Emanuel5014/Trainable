package com.emanuel5014.trainable.ui.components.motion

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

internal actual fun Morph.toComposePath(progress: Float): Path = toPath(progress).asComposePath()
