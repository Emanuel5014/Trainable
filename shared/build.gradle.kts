import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Code shared by the Android app and the future iOS app: the domain rules and the Room entities they work on.
// Nothing here may use Android or JVM APIs directly: what differs per platform goes behind an expect/actual pair.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
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
            // Only the annotations: the database itself is still built in the app
            api(libs.room.common)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
