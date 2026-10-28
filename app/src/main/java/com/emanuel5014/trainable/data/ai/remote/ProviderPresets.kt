package com.emanuel5014.trainable.data.ai.remote

/**
 * A starting point in the provider editor: it fills the protocol and the address. [hint] is shown for the
 * servers that live on the user's own PC, where only the PC's address is missing.
 */
data class ProviderPreset(
    val name: String,
    val protocol: AiProtocol,
    val baseUrl: String,
    val hint: String? = null
)

object ProviderPresets {
    val all: List<ProviderPreset> = listOf(
        ProviderPreset("OpenAI", AiProtocol.OPENAI_COMPAT, "https://api.openai.com/v1"),
        ProviderPreset("OpenRouter", AiProtocol.OPENAI_COMPAT, "https://openrouter.ai/api/v1"),
        ProviderPreset("Groq", AiProtocol.OPENAI_COMPAT, "https://api.groq.com/openai/v1"),
        ProviderPreset("Claude", AiProtocol.ANTHROPIC, "https://api.anthropic.com"),
        ProviderPreset("Ollama", AiProtocol.OPENAI_COMPAT, "http://", hint = "http://192.168.1.20:11434/v1"),
        ProviderPreset("LM Studio", AiProtocol.OPENAI_COMPAT, "http://", hint = "http://192.168.1.20:1234/v1")
    )
}
