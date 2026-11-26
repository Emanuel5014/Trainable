package com.emanuel5014.trainable.ui.screens.workout

/**
 * The superset links of a quick workout's exercises when they are moved, removed or linked in "Organise exercises".
 * Exercises of a superset sit next to each other: every function returns one superset id (or null) per exercise, in
 * the order given, so that each id names a run of two or more neighbours and nothing else.
 */
object ExerciseOrganizer {

    /**
     * After a move or a removal: the first run of each superset keeps its id, a later run of the same id (the group
     * was split) gets [newId], and a run left with a single exercise is no superset any more.
     */
    fun keepSupersetsTogether(ids: List<String?>, newId: () -> String): List<String?> {
        val result = ids.toMutableList()
        val seen = mutableSetOf<String>()
        var start = 0
        while (start < ids.size) {
            val id = ids[start]
            var end = start
            while (end + 1 < ids.size && id != null && ids[end + 1] == id) end++
            if (id != null) {
                val single = end == start
                val replacement = when {
                    single -> null
                    id in seen -> newId()
                    else -> id
                }
                for (i in start..end) result[i] = replacement
                seen += id
            }
            start = end + 1
        }
        return result
    }

    /**
     * Links the exercise at [index] with the next one ([linked] true) or cuts the link between them. Linking joins the
     * two whole groups; cutting splits the group in two, each half a superset only if it keeps two exercises.
     */
    fun linkWithNext(ids: List<String?>, index: Int, linked: Boolean, newId: () -> String): List<String?> {
        if (index !in 0 until ids.size - 1) return ids
        val result = ids.toMutableList()
        if (linked) {
            val id = ids[index] ?: ids[index + 1] ?: newId()
            // The group after takes the id of the one before (or the other way round)
            val after = ids[index + 1]
            var i = index + 1
            while (i < ids.size && (i == index + 1 || (after != null && ids[i] == after))) {
                result[i] = id
                i++
            }
            val before = ids[index]
            var j = index
            while (j >= 0 && (j == index || (before != null && ids[j] == before))) {
                result[j] = id
                j--
            }
            return result
        }
        val id = ids[index] ?: return ids
        if (ids[index + 1] != id) return ids
        // The second half gets an id of its own
        val second = newId()
        var i = index + 1
        while (i < ids.size && ids[i] == id) {
            result[i] = second
            i++
        }
        return keepSupersetsTogether(result, newId)
    }
}
