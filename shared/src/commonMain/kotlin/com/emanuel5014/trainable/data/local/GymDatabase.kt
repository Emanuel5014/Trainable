package com.emanuel5014.trainable.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.ExerciseMergeDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.dao.PhysicalCheckDao
import com.emanuel5014.trainable.data.local.dao.OneRepMaxDao
import com.emanuel5014.trainable.data.local.dao.JournalDao
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.local.entity.SessionTagEntity
import com.emanuel5014.trainable.data.local.entity.SessionTagRefEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseBlockEntity
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeMoveEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.SkippedPresetEntity
import com.emanuel5014.trainable.data.local.entity.UserEntity
import com.emanuel5014.trainable.data.local.entity.WeightLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [ // 20 Entities
        UserEntity::class,
        WeightLogEntity::class,
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanImageEntity::class,
        PlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        SessionExerciseSwapEntity::class,
        CardioLogEntity::class,
        PhysicalCheckEntity::class,
        CustomCategoryEntity::class,
        PlanExerciseBlockEntity::class,
        OneRepMaxEntity::class,
        SessionPrescriptionEditEntity::class,
        SessionTagEntity::class,
        SessionTagRefEntity::class,
        ExerciseMergeEntity::class,
        ExerciseMergeMoveEntity::class,
        SkippedPresetEntity::class
    ],
    version = 43,
    exportSchema = false
)
@ConstructedBy(GymDatabaseConstructor::class)
abstract class GymDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun physicalCheckDao(): PhysicalCheckDao
    abstract fun oneRepMaxDao(): OneRepMaxDao
    abstract fun journalDao(): JournalDao
    abstract fun exerciseMergeDao(): ExerciseMergeDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1")
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN sessioni_target_settimana INTEGER NOT NULL DEFAULT 4")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN is_finished INTEGER NOT NULL DEFAULT 0")
                // For existing sessions, assume they are finished since we didn't track unfinished before
                connection.execSQL("UPDATE workout_sessions SET is_finished = 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN ordine INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN image_uri TEXT")
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS session_exercise_swaps (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        session_id INTEGER NOT NULL,
                        original_exercise_id INTEGER NOT NULL,
                        replacement_exercise_id INTEGER NOT NULL,
                        FOREIGN KEY(session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE,
                        FOREIGN KEY(original_exercise_id) REFERENCES plan_exercises(id) ON DELETE CASCADE,
                        FOREIGN KEY(replacement_exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercise_swaps_session_id ON session_exercise_swaps(session_id)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercise_swaps_original_exercise_id ON session_exercise_swaps(original_exercise_id)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercise_swaps_replacement_exercise_id ON session_exercise_swaps(replacement_exercise_id)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN rest_timer_end_time INTEGER")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN total_rest_seconds INTEGER")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN ordine_esercizio INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS workout_plan_images (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plan_id INTEGER NOT NULL,
                        image_uri TEXT NOT NULL,
                        ordine INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(plan_id) REFERENCES workout_plans(id) ON DELETE CASCADE
                    )
                """)
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plan_images_plan_id ON workout_plan_images(plan_id)")
                
                // Migrate existing images
                connection.execSQL("""
                    INSERT INTO workout_plan_images (plan_id, image_uri, ordine)
                    SELECT id, image_uri, 0 FROM workout_plans WHERE image_uri IS NOT NULL
                """)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN giorni_settimana TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS cardio_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        session_id INTEGER NOT NULL,
                        categoria TEXT NOT NULL,
                        distanza REAL NOT NULL,
                        durata_secondi INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                """)
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_cardio_logs_session_id ON cardio_logs(session_id)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN superset_id TEXT")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN superset_id TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN rest_timer_seconds INTEGER")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN warmup_timer_end_time INTEGER")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN total_warmup_seconds INTEGER")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS physical_checks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        peso REAL,
                        note TEXT,
                        fotoFilenames TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN duration_ms INTEGER")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN exercise_type TEXT NOT NULL DEFAULT 'strength'")
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN durata_target_secondi INTEGER")
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN distanza_target_km REAL")
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN cardio_categoria TEXT")
                connection.execSQL("ALTER TABLE cardio_logs ADD COLUMN ordine_esercizio INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE cardio_logs ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 1")

                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (150, 'Running', 'Cardio')")
                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (151, 'Cycling', 'Cardio')")
                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (152, 'Walking', 'Cardio')")
                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (153, 'Elliptical', 'Cardio')")
                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (154, 'Rowing Machine', 'Cardio')")
                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (155, 'Stairmaster', 'Cardio')")
                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (156, 'Jump Rope', 'Cardio')")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_seconds INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_running INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_paused INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_started_at INTEGER")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE cardio_logs ADD COLUMN durata_target_secondi INTEGER")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("UPDATE exercises SET nome = 'Treadmill' WHERE id = 150")
                connection.execSQL("UPDATE exercises SET nome = 'Stationary Bike' WHERE id = 151")
                connection.execSQL("UPDATE exercises SET nome = 'Elliptical' WHERE id = 152")
                connection.execSQL("UPDATE exercises SET nome = 'Stairmaster' WHERE id = 153")
                connection.execSQL("UPDATE exercises SET nome = 'Assault Bike' WHERE id = 155")
                connection.execSQL("UPDATE exercises SET nome = 'SkiErg' WHERE id = 156")
                connection.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (157, 'Jump Rope', 'Cardio')")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN durata_secondi INTEGER")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_seconds INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_running INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_paused INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_started_at INTEGER")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(connection: SQLiteConnection) {
                // Periodized plans (weeks)
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN weeks_count INTEGER NOT NULL DEFAULT 1")
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN current_week INTEGER NOT NULL DEFAULT 1")
                connection.execSQL("ALTER TABLE workout_plans ADD COLUMN auto_advance_week INTEGER NOT NULL DEFAULT 1")
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN excluded_weeks TEXT")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN program_week INTEGER")

                // Advanced prescription blocks (%1RM, techniques, ...)
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `plan_exercise_blocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `plan_exercise_id` INTEGER NOT NULL, `week` INTEGER NOT NULL, `ordine` INTEGER NOT NULL,
                        `sets` INTEGER NOT NULL, `reps` TEXT NOT NULL, `rep_mode` TEXT NOT NULL, `total_reps` INTEGER,
                        `intensity_type` TEXT NOT NULL, `intensity_value` REAL, `techniques` TEXT,
                        `rest_seconds` INTEGER, `note` TEXT,
                        FOREIGN KEY(`plan_exercise_id`) REFERENCES `plan_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"""
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_exercise_blocks_plan_exercise_id` ON `plan_exercise_blocks` (`plan_exercise_id`)")

                // 1RM history
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `one_rep_maxes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `exercise_id` INTEGER NOT NULL, `weight_kg` REAL NOT NULL, `date` INTEGER NOT NULL,
                        `source` TEXT NOT NULL, `note` TEXT,
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"""
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_one_rep_maxes_exercise_id` ON `one_rep_maxes` (`exercise_id`)")

                // set_logs: rpe INTEGER -> REAL (RPE 7.5) requires a table rebuild, plus prescription snapshot columns
                connection.execSQL(
                    """CREATE TABLE IF NOT EXISTS `set_logs_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `session_id` INTEGER NOT NULL, `exercise_id` INTEGER NOT NULL,
                        `peso_sollevato` REAL NOT NULL, `reps_effettive` INTEGER NOT NULL, `numero_serie` INTEGER NOT NULL,
                        `rpe` REAL, `is_warmup` INTEGER NOT NULL, `note` TEXT, `ordine_esercizio` INTEGER NOT NULL,
                        `superset_id` TEXT, `rest_timer_seconds` INTEGER, `is_completed` INTEGER NOT NULL,
                        `durata_secondi` INTEGER,
                        `target_percent` REAL, `target_rpe` REAL, `target_reps` TEXT, `rep_mode` TEXT,
                        `block_index` INTEGER, `techniques` TEXT, `is_extra` INTEGER NOT NULL DEFAULT 0,
                        `target_total_reps` INTEGER,
                        FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"""
                )
                connection.execSQL(
                    """INSERT INTO `set_logs_new` (`id`, `session_id`, `exercise_id`, `peso_sollevato`, `reps_effettive`,
                        `numero_serie`, `rpe`, `is_warmup`, `note`, `ordine_esercizio`, `superset_id`,
                        `rest_timer_seconds`, `is_completed`, `durata_secondi`)
                        SELECT `id`, `session_id`, `exercise_id`, `peso_sollevato`, `reps_effettive`,
                        `numero_serie`, `rpe`, `is_warmup`, `note`, `ordine_esercizio`, `superset_id`,
                        `rest_timer_seconds`, `is_completed`, `durata_secondi` FROM `set_logs`"""
                )
                connection.execSQL("DROP TABLE `set_logs`")
                connection.execSQL("ALTER TABLE `set_logs_new` RENAME TO `set_logs`")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_set_logs_session_id` ON `set_logs` (`session_id`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_set_logs_exercise_id` ON `set_logs` (`exercise_id`)")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(connection: SQLiteConnection) {
                // The user's own image/GIF of an exercise (file name inside the exercise_media folder)
                connection.execSQL("ALTER TABLE exercises ADD COLUMN media_path TEXT")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(connection: SQLiteConnection) {
                // Per-exercise plate calculator: on/off and the weight of the bar
                connection.execSQL("ALTER TABLE exercises ADD COLUMN plate_calc INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE exercises ADD COLUMN plate_bar_kg REAL")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(connection: SQLiteConnection) {
                // Rep ranges ("10-15") and a starting weight per exercise of a routine
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN reps_max INTEGER")
                connection.execSQL("ALTER TABLE plan_exercises ADD COLUMN weight_preset REAL")
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(connection: SQLiteConnection) {
                // Time & Reps sets: timed like Time & Weight ones, so their kind has to be written down
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN exercise_type TEXT")
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(connection: SQLiteConnection) {
                // Cardio in sets, each with its own note; what was logged so far is the first set
                connection.execSQL("ALTER TABLE cardio_logs ADD COLUMN numero_serie INTEGER NOT NULL DEFAULT 1")
                connection.execSQL("ALTER TABLE cardio_logs ADD COLUMN note TEXT")
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(connection: SQLiteConnection) {
                // How a set really went: mini-sets after short pauses, help from a spotter, partials, a changed technique
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN rep_parts TEXT")
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN intra_rests TEXT")
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN assisted_reps INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN partial_reps INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE set_logs ADD COLUMN performed_techniques TEXT")
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(connection: SQLiteConnection) {
                // The changes made to the prescription of an exercise during its workout, kept next to the session
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_prescription_edits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `session_id` INTEGER NOT NULL,
                        `exercise_id` INTEGER NOT NULL,
                        `plan_exercise_id` INTEGER,
                        `program_week` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `kind` TEXT NOT NULL,
                        `scope` TEXT NOT NULL,
                        `before_blocks` TEXT NOT NULL,
                        `after_blocks` TEXT NOT NULL,
                        FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_session_prescription_edits_session_id` ON `session_prescription_edits` (`session_id`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_session_prescription_edits_exercise_id` ON `session_prescription_edits` (`exercise_id`)")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(connection: SQLiteConnection) {
                // A deleted custom exercise stays in the table (hidden) while history and routines still use it
                connection.execSQL("ALTER TABLE exercises ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(connection: SQLiteConnection) {
                // The workout journal: a rating and a note on the workout, and tags that can be put on any workout
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN journal_note TEXT")
                connection.execSQL("ALTER TABLE workout_sessions ADD COLUMN rating INTEGER")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `session_tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `name_key` TEXT NOT NULL, `color_index` INTEGER NOT NULL)"
                )
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_tags_name_key` ON `session_tags` (`name_key`)")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `session_tag_refs` (`session_id` INTEGER NOT NULL, `tag_id` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`session_id`, `tag_id`), " +
                        "FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`tag_id`) REFERENCES `session_tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_session_tag_refs_tag_id` ON `session_tag_refs` (`tag_id`)")
            }
        }

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(connection: SQLiteConnection) {
                // How hard a cardio set felt (RPE); strength and timed sets already had it in set_logs
                connection.execSQL("ALTER TABLE cardio_logs ADD COLUMN rpe REAL")
            }
        }

        val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(connection: SQLiteConnection) {
                // A tag can have a colour picked freely instead of one of the palette slots
                connection.execSQL("ALTER TABLE session_tags ADD COLUMN custom_color INTEGER")
            }
        }

        val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(connection: SQLiteConnection) {
                // The log of exercise merges (loser merged into winner) and the rows each one moved, to undo it
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercise_merges` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`winner_id` INTEGER NOT NULL, `loser_id` INTEGER NOT NULL, `loser_name` TEXT NOT NULL, " +
                        "`loser_was_hidden` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`winner_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`loser_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_merges_winner_id` ON `exercise_merges` (`winner_id`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_merges_loser_id` ON `exercise_merges` (`loser_id`)")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercise_merge_moves` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`merge_id` INTEGER NOT NULL, `source` TEXT NOT NULL, `row_id` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`merge_id`) REFERENCES `exercise_merges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_merge_moves_merge_id` ON `exercise_merge_moves` (`merge_id`)")
            }
        }

        val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(connection: SQLiteConnection) {
                // The favorite star of an exercise, and whether a merge moved a star to the winner (to take it back on undo)
                connection.execSQL("ALTER TABLE exercises ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE exercise_merges ADD COLUMN favorite_moved INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(connection: SQLiteConnection) {
                // The muscles and the equipment the user chose for an exercise; null leaves a preset with its catalog values
                connection.execSQL("ALTER TABLE exercises ADD COLUMN muscles TEXT")
                connection.execSQL("ALTER TABLE exercises ADD COLUMN equipment TEXT")
            }
        }

        val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(connection: SQLiteConnection) {
                // The run of the exercise a program edit was made to, when it was done twice in the workout; null for the old ones
                connection.execSQL("ALTER TABLE session_prescription_edits ADD COLUMN ordine_esercizio INTEGER")
            }
        }

        val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(connection: SQLiteConnection) {
                // Smith Machine Shrugs is a trapezius exercise, so a back one (issue #51). Data only: a shrug the user
                // already moved to another category, and the custom exercises, are not touched.
                connection.execSQL("UPDATE exercises SET categoria = 'Back' WHERE id = 75 AND categoria = 'Shoulders'")
            }
        }

        val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(connection: SQLiteConnection) {
                // Every cardio log gets the id of its exercise (issue #64): cardio_logs.exercise_id, the presets 164-166
                // and the skipped_presets table. Adds rows and fills the new column only; see CardioLinking.
                CardioLinking.migrate(connection)
            }
        }

        const val DATABASE_NAME = "gym_tracking_database"

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
            MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
            MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
            MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26,
            MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32,
            MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38,
            MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41, MIGRATION_41_42, MIGRATION_42_43
        )
    }
}

// Room generates the actual object for every platform
@Suppress("KotlinNoActualForExpect")
expect object GymDatabaseConstructor : RoomDatabaseConstructor<GymDatabase> {
    override fun initialize(): GymDatabase
}
