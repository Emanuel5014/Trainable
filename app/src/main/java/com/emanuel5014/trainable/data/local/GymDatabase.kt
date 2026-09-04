package com.emanuel5014.trainable.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.dao.PhysicalCheckDao
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.UserEntity
import com.emanuel5014.trainable.data.local.entity.WeightLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ // 11 Entities
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
        CustomCategoryEntity::class
    ],
    version = 25,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun physicalCheckDao(): PhysicalCheckDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_plans ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE workout_plans ADD COLUMN sessioni_target_settimana INTEGER NOT NULL DEFAULT 4")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN is_finished INTEGER NOT NULL DEFAULT 0")
                // For existing sessions, assume they are finished since we didn't track unfinished before
                db.execSQL("UPDATE workout_sessions SET is_finished = 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_plans ADD COLUMN ordine INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_plans ADD COLUMN image_uri TEXT")
                db.execSQL("""
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
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercise_swaps_session_id ON session_exercise_swaps(session_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercise_swaps_original_exercise_id ON session_exercise_swaps(original_exercise_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercise_swaps_replacement_exercise_id ON session_exercise_swaps(replacement_exercise_id)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN rest_timer_end_time INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN total_rest_seconds INTEGER")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN ordine_esercizio INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS workout_plan_images (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plan_id INTEGER NOT NULL,
                        image_uri TEXT NOT NULL,
                        ordine INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(plan_id) REFERENCES workout_plans(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plan_images_plan_id ON workout_plan_images(plan_id)")
                
                // Migrate existing images
                db.execSQL("""
                    INSERT INTO workout_plan_images (plan_id, image_uri, ordine)
                    SELECT id, image_uri, 0 FROM workout_plans WHERE image_uri IS NOT NULL
                """)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_plans ADD COLUMN giorni_settimana TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cardio_logs_session_id ON cardio_logs(session_id)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan_exercises ADD COLUMN superset_id TEXT")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN superset_id TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN rest_timer_seconds INTEGER")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN warmup_timer_end_time INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN total_warmup_seconds INTEGER")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN duration_ms INTEGER")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan_exercises ADD COLUMN exercise_type TEXT NOT NULL DEFAULT 'strength'")
                db.execSQL("ALTER TABLE plan_exercises ADD COLUMN durata_target_secondi INTEGER")
                db.execSQL("ALTER TABLE plan_exercises ADD COLUMN distanza_target_km REAL")
                db.execSQL("ALTER TABLE plan_exercises ADD COLUMN cardio_categoria TEXT")
                db.execSQL("ALTER TABLE cardio_logs ADD COLUMN ordine_esercizio INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cardio_logs ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 1")

                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (150, 'Running', 'Cardio')")
                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (151, 'Cycling', 'Cardio')")
                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (152, 'Walking', 'Cardio')")
                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (153, 'Elliptical', 'Cardio')")
                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (154, 'Rowing Machine', 'Cardio')")
                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (155, 'Stairmaster', 'Cardio')")
                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (156, 'Jump Rope', 'Cardio')")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_seconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_running INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_paused INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN cardio_timer_started_at INTEGER")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cardio_logs ADD COLUMN durata_target_secondi INTEGER")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE exercises SET nome = 'Treadmill' WHERE id = 150")
                db.execSQL("UPDATE exercises SET nome = 'Stationary Bike' WHERE id = 151")
                db.execSQL("UPDATE exercises SET nome = 'Elliptical' WHERE id = 152")
                db.execSQL("UPDATE exercises SET nome = 'Stairmaster' WHERE id = 153")
                db.execSQL("UPDATE exercises SET nome = 'Assault Bike' WHERE id = 155")
                db.execSQL("UPDATE exercises SET nome = 'SkiErg' WHERE id = 156")
                db.execSQL("INSERT OR IGNORE INTO exercises (id, nome, categoria) VALUES (157, 'Jump Rope', 'Cardio')")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN durata_secondi INTEGER")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_seconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_running INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_paused INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN set_timer_started_at INTEGER")
            }
        }

        @Volatile
        private var INSTANCE: GymDatabase? = null

        private const val DATABASE_NAME = "gym_tracking_database"

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
                        MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                        MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22,
                        MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25
                    )
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val dbInstance = INSTANCE
                            if (dbInstance != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateInitialData(dbInstance.exerciseDao(), dbInstance.userDao())
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            val dbInstance = INSTANCE
                            if (dbInstance != null) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    syncExercises(dbInstance.exerciseDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        suspend fun resetDatabase(context: Context) {
            closeDatabase()
            
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val walFile = context.getDatabasePath("$DATABASE_NAME-wal")
            val shmFile = context.getDatabasePath("$DATABASE_NAME-shm")
            val journalFile = context.getDatabasePath("$DATABASE_NAME-journal")

            listOf(dbFile, walFile, shmFile, journalFile).forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        }

        private suspend fun populateInitialData(exerciseDao: ExerciseDao, userDao: UserDao) {
            try {
                userDao.insertUser(
                    UserEntity(
                        id = 1,
                        username = "Athlete",
                        dataIscrizione = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // User might already exist, ignore
            }

            syncExercises(exerciseDao)
        }

        private suspend fun syncExercises(exerciseDao: ExerciseDao) {
            try {
                exerciseDao.insertExercises(ExerciseData.initialExercises)
            } catch (e: Exception) {
                // Exercises might already exist, ignore
            }
        }
    }
}
