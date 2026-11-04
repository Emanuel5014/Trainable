import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Compose UI shared by the Android and the iOS app (same look on both). On Android the Compose libraries resolve
// to the app's own androidx versions; what only Android can draw (Liquid Glass) goes behind expect/actual.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.emanuel5014.trainable.sharedui"
        compileSdk = 37
        minSdk = 28
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        androidResources {
            enable = true
        }
    }

    // The one framework the iOS app links: this module and, exported, the shared logic it builds on
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":shared"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api(libs.jb.compose.runtime)
            api(libs.jb.compose.foundation)
            api(libs.jb.compose.ui)
            api(libs.jb.compose.material3)
            implementation(libs.jb.compose.icons.extended)
            implementation(libs.jb.compose.resources)
            api(libs.jb.navigation.compose)
            implementation(libs.haze)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

compose.resources {
    packageOfResClass = "com.emanuel5014.trainable.sharedui.resources"
}
