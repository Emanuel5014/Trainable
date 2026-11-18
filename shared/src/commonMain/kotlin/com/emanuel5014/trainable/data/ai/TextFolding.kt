package com.emanuel5014.trainable.data.ai

/** [value] without accents and other combining marks ("Panca inclinàta" → "Panca inclinata"), for name matching. */
fun stripDiacritics(value: String): String = canonicalDecomposition(value).replace(Regex("\\p{Mn}+"), "")

/** Unicode NFD (Android: java.text.Normalizer, iOS: NSString decomposedStringWithCanonicalMapping). */
internal expect fun canonicalDecomposition(value: String): String
