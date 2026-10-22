package com.emanuel5014.trainable.data.repository

import android.content.Context
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.ExerciseMergeDao
import com.emanuel5014.trainable.data.local.dao.MergeOutcome
import com.emanuel5014.trainable.data.local.dao.MergePreview
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseMergeState
import com.emanuel5014.trainable.domain.exercise.ExerciseMerges
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Merging an exercise into another and undoing it: the database work is in [ExerciseMergeDao], this adds what lives outside it. */
@Singleton
class ExerciseMergeRepository @Inject constructor(
    private val mergeDao: ExerciseMergeDao,
    private val exerciseDao: ExerciseDao,
    @ApplicationContext private val context: Context
) {
    /** What merging [loserId] into another exercise would move. */
    suspend fun preview(loserId: Int): MergePreview = mergeDao.preview(loserId)

    suspend fun merge(winnerId: Int, loserId: Int): MergeOutcome {
        val outcome = mergeDao.merge(winnerId, loserId, System.currentTimeMillis())
        if (outcome is MergeOutcome.Merged) keepAnalyticsSelection(loserId = loserId, winnerId = winnerId)
        return outcome
    }

    /** @return false when the merge is unknown or a later merge builds on it (that one is undone first). */
    suspend fun undo(mergeId: Int): Boolean = mergeDao.undo(mergeId)

    fun observeMerges(): Flow<List<ExerciseMergeEntity>> = mergeDao.observeAllMerges()

    /** The merges [exerciseId] took part in, as they change. */
    fun observeState(exerciseId: Int): Flow<ExerciseMergeState> =
        combine(mergeDao.observeAllMerges(), exerciseDao.getAllExercisesIncludingDeleted()) { merges, exercises ->
            ExerciseMerges.stateFor(exerciseId, merges, exercises)
        }

    /**
     * Analytics remembers which exercises the user picked. When the merged-away one was among them the winner joins; the
     * loser stays in the set, so undoing the merge needs nothing here.
     */
    private suspend fun keepAnalyticsSelection(loserId: Int, winnerId: Int) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("selected_exercise_ids", null) ?: return@withContext
        val selected = saved.mapNotNull { it.toIntOrNull() }.toSet()
        val updated = ExerciseMerges.selectionAfterMerge(selected, loserId, winnerId)
        if (updated != selected) {
            prefs.edit().putStringSet("selected_exercise_ids", updated.map { it.toString() }.toSet()).apply()
        }
    }
}
