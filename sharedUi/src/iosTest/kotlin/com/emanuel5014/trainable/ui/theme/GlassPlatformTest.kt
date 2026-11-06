package com.emanuel5014.trainable.ui.theme

import kotlin.test.Test
import kotlin.test.assertTrue

class GlassPlatformTest {
    /** iOS always has Skia blur, so glass uses its thin body (Android only from 12). */
    @Test
    fun glassCanBlurOnIos() {
        assertTrue(glassCanBlur)
    }
}
