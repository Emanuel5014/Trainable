package com.emanuel5014.trainable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emanuel5014.trainable.data.local.entity.SessionTagEntity
import com.emanuel5014.trainable.data.local.entity.SessionTagRefEntity
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import kotlinx.coroutines.flow.Flow

/**
 * Single-statement building blocks for the workout journal; `JournalRepository` composes them in transactions.
 * The rating and note of a workout are written with a targeted UPDATE, never by saving the whole session row, so a
 * stale copy of the session elsewhere cannot overwrite them.
 */
@Dao
interface JournalDao {
    @Query("SELECT * FROM session_tags WHERE name_key = :key LIMIT 1")
    suspend fun findTagByKey(key: String): SessionTagEntity?

    @Query("SELECT * FROM session_tags WHERE id = :id")
    suspend fun getTag(id: Int): SessionTagEntity?

    /** The palette slots in use; a tag with a free colour does not take one. */
    @Query("SELECT color_index FROM session_tags WHERE custom_color IS NULL")
    suspend fun allColorIndexes(): List<Int>

    @Insert
    suspend fun insertTag(tag: SessionTagEntity): Long

    @Query("UPDATE session_tags SET name = :name, name_key = :key WHERE id = :id")
    suspend fun renameTag(id: Int, name: String, key: String)

    /** A palette colour; it replaces any free colour the tag had. */
    @Query("UPDATE session_tags SET color_index = :colorIndex, custom_color = NULL WHERE id = :id")
    suspend fun setTagColor(id: Int, colorIndex: Int)

    @Query("UPDATE session_tags SET custom_color = :color WHERE id = :id")
    suspend fun setTagCustomColor(id: Int, color: Int)

    @Query("DELETE FROM session_tags WHERE id = :id")
    suspend fun deleteTag(id: Int)

    @Query("SELECT session_id FROM session_tag_refs WHERE tag_id = :tagId")
    suspend fun sessionIdsWithTag(tagId: Int): List<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRefs(refs: List<SessionTagRefEntity>)

    @Query("DELETE FROM session_tag_refs WHERE session_id = :sessionId")
    suspend fun clearRefs(sessionId: Int)

    @Query("UPDATE workout_sessions SET rating = :rating, journal_note = :note WHERE id = :sessionId")
    suspend fun setJournalFields(sessionId: Int, rating: Int?, note: String?)

    /** Every tag with its use, the most recently used first (then the most used, then by name). */
    @Query(
        """
        SELECT t.id AS id, t.name AS name, t.name_key AS nameKey, t.color_index AS colorIndex,
               t.custom_color AS customColor, COUNT(r.session_id) AS uses, MAX(s.timestamp) AS lastUsed
        FROM session_tags t
        LEFT JOIN session_tag_refs r ON r.tag_id = t.id
        LEFT JOIN workout_sessions s ON s.id = r.session_id
        GROUP BY t.id
        ORDER BY lastUsed DESC, uses DESC, t.name COLLATE NOCASE ASC
        """
    )
    fun observeTagsWithUsage(): Flow<List<TagWithUsage>>
}
