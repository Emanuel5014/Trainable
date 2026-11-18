package com.emanuel5014.trainable.data.ai

internal actual fun parseLenientJson(text: String): Any = LenientJsonTokener(text).nextValue()
