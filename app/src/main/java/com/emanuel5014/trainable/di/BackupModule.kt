package com.emanuel5014.trainable.di

import com.emanuel5014.trainable.data.backup.AutoBackupRunner
import com.emanuel5014.trainable.data.backup.NextcloudBackupService
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDav
import com.emanuel5014.trainable.data.remote.nextcloud.OkHttpWebDavHttp
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.util.NextcloudCryptoManager
import com.emanuel5014.trainable.util.backup.AndroidBackupArchiver
import com.emanuel5014.trainable.util.backup.SafBackupFolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The shared backup/Nextcloud classes (no DI annotations in :shared) on top of the Android ports. */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides @Singleton
    fun provideNextcloudWebDav(http: OkHttpWebDavHttp) = NextcloudWebDav(http)

    @Provides @Singleton
    fun provideNextcloudBackupService(prefs: UserPreferencesRepository, webDav: NextcloudWebDav,
                                      crypto: NextcloudCryptoManager, archiver: AndroidBackupArchiver) =
        NextcloudBackupService(prefs, webDav, crypto, archiver) { System.currentTimeMillis() }

    @Provides
    fun provideAutoBackupRunner(prefs: UserPreferencesRepository, archiver: AndroidBackupArchiver, folder: SafBackupFolder,
                                nextcloud: NextcloudBackupService) =
        AutoBackupRunner(prefs, archiver, folder, nextcloud) { System.currentTimeMillis() }
}
