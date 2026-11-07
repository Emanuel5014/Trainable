package com.emanuel5014.trainable.data.ai

/** What a model read off a photo: the answer and, for models that show it, their reasoning. */
data class LlmScanResult(
    val output: String,
    val thinking: String
)
