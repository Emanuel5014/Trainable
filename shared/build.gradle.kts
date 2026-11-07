import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

// Code shared by the Android app and the future iOS app: the domain rules and the Room entities they work on.
// Nothing here may use Android or JVM APIs directly: what differs per platform goes behind an expect/actual pair.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.emanuel5014.trainable.shared"
        compileSdk = 37
        minSdk = 28
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        withHostTest {}
    }

    // The iOS framework is built by :sharedUi, which exports this module
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.room.runtime)
            api(libs.datastore.preferences.core)
            // @Immutable on the settings models the Compose UI reads
            implementation(libs.compose.runtime.annotation)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okio)
        }
        androidMain.dependencies {
            // The remote AI keeps OkHttp's own URL parser on Android (AiUrl.android.kt)
            implementation(libs.okhttp)
        }
        iosMain.dependencies {
            // Android keeps the system SQLite (Room without a driver); iOS brings its own
            implementation(libs.sqlite.bundled)
        }
        getByName("androidHostTest").dependencies {
            // Real org.json for JVM host tests (the Android stub only throws); the device keeps the system one
            implementation("org.json:json:20260814")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    // Room's code generator for every target
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.room.compiler) }
}

// Local checks on real data (tests that are not in git):
// ./gradlew :shared:iosSimulatorArm64Test -PmigrationFixtures=<folder of .db files> -PsettingsFixture=<settings.preferences_pb>
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    providers.gradleProperty("migrationFixtures").orNull?.let { environment("SIMCTL_CHILD_MIGRATION_FIXTURES", it) }
    providers.gradleProperty("settingsFixture").orNull?.let { environment("SIMCTL_CHILD_SETTINGS_FIXTURE", it) }
    providers.gradleProperty("backupFixtures").orNull?.let { environment("SIMCTL_CHILD_BACKUP_FIXTURES", it) }
    providers.gradleProperty("aiFakeServer").orNull?.let { environment("SIMCTL_CHILD_AI_FAKE_SERVER", it) }
    providers.gradleProperty("modelFileServer").orNull?.let { environment("SIMCTL_CHILD_MODEL_FILE_SERVER", it) }
}
