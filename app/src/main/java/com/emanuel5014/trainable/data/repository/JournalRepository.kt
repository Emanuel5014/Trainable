package com.emanuel5014.trainable.data.repository

import androidx.room.withTransaction
import com.emanuel5014.trainable.data.local.GymDatabase
import com.emanuel5014.trainable.data.local.dao.JournalDao
import com.emanuel5014.trainable.data.local.entity.SessionTagEntity
import com.emanuel5014.trainable.data.local.entity.SessionTagRefEntity
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.domain.journal.TagName
import com.emanuel5014.trainable.domain.journal.TagPalette
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Runs several database calls as one: all of them happen or none does. Behind an interface so tests need no database. */
interface TransactionRunner {
    suspend fun <R> run(block: suspend () -> R): R
}

class RoomTransactionRunner @Inject constructor(private val database: GymDatabase) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = database.withTransaction { block() }
}

/** The tag a name stands for, and whether asking for it made a new one. */
data class TagResult(val tag: SessionTagEntity, val created: Boolean)

sealed interface RenameResult {
    data object Renamed : RenameResult

    /** The new name is another tag's: the caller can offer to merge into [existing]. */
    data class Conflict(val existing: SessionTagEntity) : RenameResult

    /** Nothing is left of the name once it is cleaned. */
    data object Invalid : RenameResult
}

/** The workout journal: the rating, note and tags of a workout, and the tags themselves. */
@Singleton
class JournalRepository @Inject constructor(
    private val dao: JournalDao,
    private val transactions: TransactionRunner
) {
    /** Every tag with its use, the most recently used first. */
    val tags: Flow<List<TagWithUsage>> = dao.observeTagsWithUsage()

    /** The tag with this name (case and accents do not count), made on the spot when there is none; null for a blank name. */
    suspend fun findOrCreateTag(rawName: String): TagResult? {
        val name = TagName.clean(rawName) ?: return null
        val key = TagName.key(name)
        return transactions.run {
            val existing = dao.findTagByKey(key)
            if (existing != null) {
                TagResult(existing, created = false)
            } else {
                val tag = SessionTagEntity(name = name, nameKey = key, colorIndex = TagPalette.nextSlot(dao.allColorIndexes()))
                TagResult(tag.copy(id = dao.insertTag(tag).toInt()), created = true)
            }
        }
    }

    /** Replaces the rating, note and tags of the workout with [journal] (cleaned first). */
    suspend fun saveJournal(sessionId: Int, journal: SessionJournal) {
        val cleaned = journal.cleaned()
        transactions.run {
            dao.setJournalFields(sessionId, cleaned.rating, cleaned.note)
            dao.clearRefs(sessionId)
            if (cleaned.tagIds.isNotEmpty()) {
                dao.insertRefs(cleaned.tagIds.map { SessionTagRefEntity(sessionId = sessionId, tagId = it) })
            }
        }
    }

    suspend fun renameTag(id: Int, rawName: String): RenameResult {
        val name = TagName.clean(rawName) ?: return RenameResult.Invalid
        val key = TagName.key(name)
        return transactions.run {
            val existing = dao.findTagByKey(key)
            if (existing != null && existing.id != id) {
                RenameResult.Conflict(existing)
            } else {
                dao.renameTag(id, name, key)
                RenameResult.Renamed
            }
        }
    }

    suspend fun setTagColor(id: Int, colorIndex: Int) {
        dao.setTagColor(id, colorIndex.coerceIn(0, TagPalette.SIZE - 1))
    }

    /** A colour picked freely instead of a palette slot; any transparency in [argb] is dropped. */
    suspend fun setTagCustomColor(id: Int, argb: Int) {
        dao.setTagCustomColor(id, argb or OPAQUE)
    }

    private companion object {
        const val OPAQUE = 0xFF shl 24
    }

    /** Puts [intoId] on every workout that has [fromId] (once, even when it has both) and deletes [fromId]. */
    suspend fun mergeTags(fromId: Int, intoId: Int) {
        if (fromId == intoId) return
        transactions.run {
            dao.insertRefs(dao.sessionIdsWithTag(fromId).map { SessionTagRefEntity(sessionId = it, tagId = intoId) })
            dao.deleteTag(fromId)
        }
    }

    suspend fun deleteTag(id: Int) {
        dao.deleteTag(id)
    }

    /** Deletes the tags that no workout has any more; the ones still in use stay. */
    suspend fun deleteTagsIfUnused(ids: List<Int>) {
        transactions.run {
            ids.forEach { if (dao.sessionIdsWithTag(it).isEmpty()) dao.deleteTag(it) }
        }
    }
}
