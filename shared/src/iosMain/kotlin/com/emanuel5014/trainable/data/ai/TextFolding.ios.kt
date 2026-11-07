package com.emanuel5014.trainable.data.ai

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun canonicalDecomposition(value: String): String = (value as NSString).decomposedStringWithCanonicalMapping
