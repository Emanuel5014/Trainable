package com.emanuel5014.trainable.data.ai.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Google's Gemini API through its OpenAI-compatible endpoint. It speaks the same protocol as the other
 * OpenAI-style providers, but wants a key, always thinks before answering, and prefers its default temperature.
 */
object GeminiEndpoint {

    const val HOST = "generativelanguage.googleapis.com"

    /** The address a profile gets from the Gemini preset. */
    const val BASE_URL = "https://$HOST/v1beta/openai"

    /** Whether [baseUrl] points at the Gemini API, however the profile was created. */
    fun matches(baseUrl: String): Boolean =
        BaseUrlPolicy.normalize(baseUrl).toHttpUrlOrNull()?.host.equals(HOST, ignoreCase = true)
}
