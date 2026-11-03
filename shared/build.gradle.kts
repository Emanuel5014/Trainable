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

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.room.runtime)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }
        iosMain.dependencies {
            // Android keeps the system SQLite (Room without a driver); iOS brings its own
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    // Room's code generator for every target
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.room.compiler) }
}

// Local check of the migrations on real backups (a test that is not in git):
// ./gradlew :shared:iosSimulatorArm64Test -PmigrationFixtures=<folder of .db files>
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    providers.gradleProperty("migrationFixtures").orNull?.let { environment("SIMCTL_CHILD_MIGRATION_FIXTURES", it) }
}
