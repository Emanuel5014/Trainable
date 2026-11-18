package com.emanuel5014.trainable.util

/**
 * The map in key order, as `toSortedMap` gave it on the JVM (which has no common version). Like a TreeMap, keys the
 * [comparator] calls equal end up as one entry: the first of those keys with the last of their values.
 */
fun <K, V> Map<K, V>.sortedByKeys(comparator: Comparator<in K>): Map<K, V> {
    val out = LinkedHashMap<K, V>()
    var lastKey: K? = null
    var hasLast = false
    for ((key, value) in entries.sortedWith { a, b -> comparator.compare(a.key, b.key) }) {
        val previous = lastKey
        if (hasLast && comparator.compare(previous as K, key) == 0) {
            out[previous] = value
        } else {
            out[key] = value
            lastKey = key
            hasLast = true
        }
    }
    return out
}

fun <K : Comparable<K>, V> Map<K, V>.sortedByKeys(): Map<K, V> = sortedByKeys(naturalOrder())
