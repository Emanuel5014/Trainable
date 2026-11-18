package com.emanuel5014.trainable.util.backup

/** Entry names of the existing, unversioned Android ZIP format, also used for iOS backups. */
object BackupFormat {
    const val DATABASE = "gym_tracking_database"
    const val SETTINGS = "settings.json"
    const val ANALYTICS_SETTINGS = "analytics_settings.json"
    const val APP_PREFERENCES = "app_preferences.json"
    const val IMAGES = "images/"
    const val EXERCISE_MEDIA = "exercise_media/"
    const val PHYSICAL_CHECKS = "physical_checks/"
}
