package com.emanuel5014.trainable.data.ai

import java.text.Normalizer

internal actual fun canonicalDecomposition(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
