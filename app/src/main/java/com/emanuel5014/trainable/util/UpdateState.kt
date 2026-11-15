package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.data.remote.GitHubRelease

/** Why a check, a download or an install did not work. Each one has its own message in the dialog. */
enum class UpdateError {
    /** No connection, timeout, connection dropped (after the automatic retries). */
    NETWORK,
    /** GitHub refused the request because this network asked too often (60 an hour without an account). */
    RATE_LIMITED,
    /** GitHub answered with an error or with something that is not a release list. */
    SERVER,
    /** The release has no .apk file. */
    NO_APK,
    /** Not enough free space to download the APK and hand it to Android. */
    STORAGE,
    /** The downloaded file does not match the size or the SHA-256 that GitHub lists. */
    CORRUPT,
    /**
     * This copy of the app is not signed with the key of the GitHub releases (another store, a build of its own):
     * Android would refuse the update, so it is not downloaded. Found before downloading.
     */
    OTHER_SOURCE,
    /** The release's APK is another app (another package name), found after downloading. */
    WRONG_APP,
    /** The APK is signed with a key Android will not accept over this install (found after downloading, or by Android). */
    INSTALL_CONFLICT,
    /** Android had no room to install. */
    INSTALL_STORAGE,
    /** Any other refusal of Android's installer; the detail holds its message. */
    INSTALL_FAILED
}

/** Where the update offered to the user stands. Every phase but [Idle] carries the release it is about. */
sealed interface UpdatePhase {
    /** Nothing to offer (not checked yet, up to date, or the offer was closed). */
    data object Idle : UpdatePhase

    data class Available(val release: GitHubRelease) : UpdatePhase

    data class Downloading(val release: GitHubRelease, val downloaded: Long, val total: Long) : UpdatePhase {
        /** 0..1, or null when the size is unknown. */
        val fraction: Float? get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
    }

    /** Checking size and SHA-256 of the finished file. */
    data class Verifying(val release: GitHubRelease) : UpdatePhase

    /** A verified APK is on disk; Android's installer is not running (not started yet, or the user backed out). */
    data class ReadyToInstall(val release: GitHubRelease) : UpdatePhase

    /** Handed to Android's installer, waiting for the user's confirmation or for Android. */
    data class Installing(val release: GitHubRelease) : UpdatePhase

    data class Failed(val release: GitHubRelease?, val error: UpdateError, val detail: String? = null) : UpdatePhase
}

/** Errors that trying again cannot fix: the dialog offers the release page instead of "Retry". */
val UpdateError.isFinal: Boolean
    get() = this == UpdateError.OTHER_SOURCE || this == UpdateError.WRONG_APP ||
        this == UpdateError.NO_APK || this == UpdateError.INSTALL_CONFLICT

/** The phase plus whether the update dialog is on screen; the dialog can be hidden while a download goes on. */
data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.Idle,
    val dialogOpen: Boolean = false
) {
    val release: GitHubRelease?
        get() = when (phase) {
            UpdatePhase.Idle -> null
            is UpdatePhase.Available -> phase.release
            is UpdatePhase.Downloading -> phase.release
            is UpdatePhase.Verifying -> phase.release
            is UpdatePhase.ReadyToInstall -> phase.release
            is UpdatePhase.Installing -> phase.release
            is UpdatePhase.Failed -> phase.release
        }

    /** True while work runs that the dialog must not restart (download, verification, installer open). */
    val isBusy: Boolean
        get() = phase is UpdatePhase.Downloading || phase is UpdatePhase.Verifying || phase is UpdatePhase.Installing
}

/** Result of one check, for the "Check for updates" button. */
sealed interface UpdateCheck {
    data object UpToDate : UpdateCheck
    /** Debug and fast builds never look for updates (their package name differs from the release one). */
    data object Disabled : UpdateCheck
    data class Available(val release: GitHubRelease) : UpdateCheck
    data class Failed(val error: UpdateError) : UpdateCheck
}
