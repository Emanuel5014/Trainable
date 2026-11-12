package com.emanuel5014.trainable.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.CardioExerciseMatcher
import com.emanuel5014.trainable.domain.exercise.CardioPresets
import com.emanuel5014.trainable.domain.exercise.ExercisePresets

/**
 * Gives every cardio log the id of its exercise, decided by [CardioExerciseMatcher] from the name written on the log.
 * [migrate] is the 42 -> 43 step (one transaction, run by Room); [repair] runs at every start and links the rows still
 * without one. Neither changes or removes an existing exercise or log: they add exercises and fill `exercise_id` only.
 */
object CardioLinking {

    private const val CARDIO = "Cardio"

    /** Every column of cardio_logs at 42, in order: copied as they are into the rebuilt table. */
    private const val CARDIO_COLUMNS = "`id`, `session_id`, `categoria`, `distanza`, `durata_secondi`, `timestamp`, " +
        "`ordine_esercizio`, `durata_target_secondi`, `is_completed`, `numero_serie`, `note`, `rpe`"

    /** The table Room expects for CardioLogEntity at 43 (copied from its generated schema). */
    private const val CREATE_CARDIO_LOGS_NEW = "CREATE TABLE `cardio_logs_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`session_id` INTEGER NOT NULL, `categoria` TEXT NOT NULL, `distanza` REAL NOT NULL, `durata_secondi` INTEGER NOT NULL, " +
        "`timestamp` INTEGER NOT NULL, `ordine_esercizio` INTEGER NOT NULL, `durata_target_secondi` INTEGER, " +
        "`is_completed` INTEGER NOT NULL, `numero_serie` INTEGER NOT NULL, `note` TEXT, `rpe` REAL, `exercise_id` INTEGER, " +
        "FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
        "FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"

    private const val CREATE_SKIPPED_PRESETS = "CREATE TABLE IF NOT EXISTS `skipped_presets` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))"

    /** The 42 -> 43 migration. The order matters: the presets go in before the plan, or their logs would make customs. */
    fun migrate(connection: SQLiteConnection) {
        rebuildCardioLogs(connection)
        connection.execSQL(CREATE_SKIPPED_PRESETS)

        // An id of the new presets held by another exercise (no published build ever made one): that preset is left out
        // for good and the row is not touched. A row that is the preset itself (a build of this work run at 42) is ours:
        // it stays as it is and takes its logs like any slot.
        val slotPresets = ExerciseData.initialExercises.filter { it.id in CardioPresets.SLOT_IDS }.associateBy { it.id }
        val before = readExercises(connection)
        val taken = before.mapTo(hashSetOf()) { it.id }
        before.filter { it.id in CardioPresets.SLOT_IDS && !isShippedPreset(it, slotPresets[it.id]) }.map { it.id }.sorted().forEach { id ->
            connection.prepare("INSERT OR IGNORE INTO skipped_presets (id) VALUES (?)").use { it.bindLong(1, id.toLong()); it.step() }
        }
        val skipped = readSkipped(connection)

        // The presets, unless a name of theirs is already taken (then the exercise with that name takes their place)
        ExercisePresets.toInsert(slotPresets.values.toList(), readExercises(connection), skipped)
            .filter { it.id !in taken }
            .forEach { insertExercise(connection, it, orIgnore = true) }

        // Only now, with the presets in, every log is decided
        val exercises = readExercises(connection)
        val rows = readUnlinked(connection)
        if (rows.isEmpty()) return
        val plan = CardioExerciseMatcher.plan(rows, exercises, skipped, nextCustomId(exercises))
        // Plain insert: the ids are above every id of the table, a clash is impossible and must not be hidden
        plan.newExercises.forEach { insertExercise(connection, it, orIgnore = false) }
        val links = trustedLinks(plan, readExercises(connection), skipped)
        connection.prepare("UPDATE cardio_logs SET exercise_id = ? WHERE id = ?").use { statement ->
            links.forEach { (logId, exerciseId) ->
                statement.bindLong(1, exerciseId.toLong())
                statement.bindLong(2, logId.toLong())
                statement.step()
                statement.reset()
            }
        }
    }

    /**
     * Links the cardio logs that have no exercise: rows written by an older path, or left without one by the
     * `ON DELETE SET NULL` of an exercise deleted for real. Normally there is none. A failure leaves the rows as they are,
     * the next start tries again.
     */
    suspend fun repair(workoutDao: WorkoutDao) = workoutDao.repairCardioLinks()

    /**
     * The body of [WorkoutDao.repairCardioLinks], inside its one write transaction: nothing can change the exercises or
     * the logs between the plan and the links, and a link is only written on a row that still has none.
     */
    internal suspend fun repairInTransaction(dao: WorkoutDao) {
        val rows = dao.getUnlinkedCardioRows()
        if (rows.isEmpty()) return
        val exercises = dao.getExercisesForCardioLinking()
        val skipped = dao.getSkippedPresetIdsForCardioLinking().toSet()
        val plan = CardioExerciseMatcher.plan(rows, exercises, skipped, nextCustomId(exercises))
        // The ids are above every id of the table, so the insert cannot clash; if anything were there first, the check
        // below leaves those logs alone instead of linking them to someone else's exercise
        if (plan.newExercises.isNotEmpty()) dao.insertExercisesForCardioLinking(plan.newExercises)
        trustedLinks(plan, dao.getExercisesForCardioLinking(), skipped)
            .forEach { (logId, exerciseId) -> dao.linkCardioRow(logId, exerciseId) }
    }

    /** [row] is the shipped [preset] itself: same id and stored name, cardio. */
    private fun isShippedPreset(row: ExerciseEntity, preset: ExerciseEntity?): Boolean =
        preset != null && row.id == preset.id && row.nome == preset.nome && row.categoria.equals(CARDIO, ignoreCase = true)

    private fun nextCustomId(exercises: List<ExerciseEntity>) =
        maxOf(ExerciseEntity.FIRST_CUSTOM_ID, (exercises.maxOfOrNull { it.id } ?: 0) + 1)

    /**
     * The links of [plan] whose exercise is in the table as a cardio one, not skipped, and, for a new one, with the name
     * it was created with. The matcher only plans such links; this keeps a mistake from ever pointing a log elsewhere
     * (such a log stays unlinked and keeps its name).
     */
    private fun trustedLinks(plan: CardioExerciseMatcher.LinkPlan, exercises: List<ExerciseEntity>, skipped: Set<Int>): Map<Int, Int> {
        val byId = exercises.associateBy { it.id }
        val created = plan.newExercises.associateBy { it.id }
        return plan.links.filter { (_, exerciseId) ->
            val exercise = byId[exerciseId] ?: return@filter false
            exercise.categoria.equals(CARDIO, ignoreCase = true) && exerciseId !in skipped &&
                (created[exerciseId]?.let { it.nome == exercise.nome && it.categoria == exercise.categoria } ?: true)
        }
    }

    /**
     * SQLite cannot add a foreign key to a table, so cardio_logs is rebuilt with `exercise_id`: every row and column
     * copied as it is, the AUTOINCREMENT counter kept. No table refers to cardio_logs, so dropping it removes nothing
     * elsewhere. Room runs migrations with the foreign keys off (Android and iOS) and they cannot be switched inside the
     * transaction; should they ever be on, their check is put off to the commit, so an old log whose workout is gone
     * is copied like the others instead of failing the update.
     */
    private fun rebuildCardioLogs(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA defer_foreign_keys = ON")
        val sequence = connection.prepare("SELECT seq FROM sqlite_sequence WHERE name = 'cardio_logs'").use {
            if (it.step()) it.getLong(0) else null
        }
        val count = count(connection, "cardio_logs")

        connection.execSQL("DROP TABLE IF EXISTS `cardio_logs_new`")
        connection.execSQL(CREATE_CARDIO_LOGS_NEW)
        connection.execSQL("INSERT INTO `cardio_logs_new` ($CARDIO_COLUMNS, `exercise_id`) SELECT $CARDIO_COLUMNS, NULL FROM `cardio_logs`")
        // Never drop the old rows unless all of them are in the new table (the transaction is rolled back)
        check(count(connection, "cardio_logs_new") == count) { "cardio_logs copy incomplete" }
        connection.execSQL("DROP TABLE `cardio_logs`")
        connection.execSQL("ALTER TABLE `cardio_logs_new` RENAME TO `cardio_logs`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_cardio_logs_session_id` ON `cardio_logs` (`session_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_cardio_logs_exercise_id` ON `cardio_logs` (`exercise_id`)")

        // The copy sets the counter to the largest id; put back the old one, which may be higher (deleted rows)
        if (sequence != null) {
            val present = connection.prepare("SELECT COUNT(*) FROM sqlite_sequence WHERE name = 'cardio_logs'").use { it.step(); it.getLong(0) }
            if (present > 0) {
                connection.prepare("UPDATE sqlite_sequence SET seq = ?1 WHERE name = 'cardio_logs' AND seq < ?1").use { it.bindLong(1, sequence); it.step() }
            } else {
                connection.prepare("INSERT INTO sqlite_sequence (name, seq) VALUES ('cardio_logs', ?)").use { it.bindLong(1, sequence); it.step() }
            }
        }
    }

    private fun count(connection: SQLiteConnection, table: String): Long =
        connection.prepare("SELECT COUNT(*) FROM `$table`").use { it.step(); it.getLong(0) }

    /** What the matcher and the preset check read: the id, the name, the category, hidden or not. */
    private fun readExercises(connection: SQLiteConnection): List<ExerciseEntity> =
        connection.prepare("SELECT id, nome, categoria, is_deleted FROM exercises ORDER BY id").use { st ->
            buildList {
                while (st.step()) {
                    add(ExerciseEntity(id = st.getInt(0), nome = st.getText(1), categoria = st.getText(2), isDeleted = st.getLong(3) != 0L))
                }
            }
        }

    private fun readSkipped(connection: SQLiteConnection): Set<Int> =
        connection.prepare("SELECT id FROM skipped_presets").use { st -> buildSet { while (st.step()) add(st.getInt(0)) } }

    private fun readUnlinked(connection: SQLiteConnection): List<CardioExerciseMatcher.Row> =
        connection.prepare("SELECT id, categoria FROM cardio_logs WHERE exercise_id IS NULL ORDER BY id").use { st ->
            buildList { while (st.step()) add(CardioExerciseMatcher.Row(logId = st.getInt(0), name = st.getText(1))) }
        }

    /** Every column given, the NOT NULL ones included, so nothing is left to a default. */
    private fun insertExercise(connection: SQLiteConnection, e: ExerciseEntity, orIgnore: Boolean) {
        val verb = if (orIgnore) "INSERT OR IGNORE" else "INSERT"
        connection.prepare(
            "$verb INTO exercises (id, nome, categoria, descrizione, video_url, media_path, plate_calc, plate_bar_kg, " +
                "is_deleted, is_favorite, muscles, equipment) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { st ->
            st.bindLong(1, e.id.toLong())
            st.bindText(2, e.nome)
            st.bindText(3, e.categoria)
            st.bindTextOrNull(4, e.descrizione)
            st.bindTextOrNull(5, e.videoUrl)
            st.bindTextOrNull(6, e.mediaPath)
            st.bindLong(7, if (e.plateCalculator) 1L else 0L)
            e.plateBarKg?.let { st.bindDouble(8, it.toDouble()) } ?: st.bindNull(8)
            st.bindLong(9, if (e.isDeleted) 1L else 0L)
            st.bindLong(10, if (e.isFavorite) 1L else 0L)
            st.bindTextOrNull(11, e.muscles)
            st.bindTextOrNull(12, e.equipment)
            st.step()
        }
    }

    private fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) = if (value == null) bindNull(index) else bindText(index, value)
}
