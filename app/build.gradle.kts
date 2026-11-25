plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** The app version; versionCode follows from it, so a release only changes this line. */
val appVersionName = "3.1.0-beta1"

/**
 * versionCode from a versionName such as "3.0.0", "3.0.0-beta2" or "3.0.0-rc1":
 * MAJOR·1 000 000 + MINOR·10 000 + PATCH·100 + stage, where stage is N for betaN (1-49), 50+N for rcN (1-48) and 99
 * for the final release. 3.0.0-beta2 → 3000002, 3.0.0-rc1 → 3000051, 3.0.0 → 3000099, 3.0.1 → 3000199.
 *
 * It orders versions like the updater does (beta < rc < final, see AppVersion), so Android itself refuses to install
 * an older version over a newer one, whose database it could not read. Versions up to 3.0.0-beta2 shipped with
 * versionCode 2, below every code made here.
 */
fun versionCodeOf(name: String): Int {
    val match = Regex("""(\d+)\.(\d+)\.(\d+)(?:-(beta|rc)\.?(\d+))?""").matchEntire(name)
        ?: error("versionName \"$name\" must look like 3.0.0, 3.0.0-beta2 or 3.0.0-rc1")
    val (major, minor, patch, label, number) = match.destructured
    require(minor.toInt() < 100 && patch.toInt() < 100) { "versionName \"$name\": minor and patch must stay below 100" }
    val stage = when (label) {
        "" -> 99
        "beta" -> number.toInt().also { require(it in 1..49) { "versionName \"$name\": beta number must be 1-49" } }
        else -> 50 + number.toInt().also { require(it in 1..48) { "versionName \"$name\": rc number must be 1-48" } }
    }
    return major.toInt() * 1_000_000 + minor.toInt() * 10_000 + patch.toInt() * 100 + stage
}

android {
    namespace = "com.emanuel5014.trainable"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "com.emanuel5014.trainable"
        minSdk = 28
        targetSdk = 36
        versionCode = versionCodeOf(appVersionName)
        versionName = appVersionName

        manifestPlaceholders["appName"] = "Trainable"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Release list the updater reads instead of GitHub's; empty = GitHub. Set only in debug builds (see below).
        buildConfigField("String", "UPDATE_FEED_URL", "\"\"")
        // SHA-256 of the certificate the GitHub releases are signed with; empty = not checked (see release below)
        buildConfigField("String", "RELEASE_CERT_SHA256", "\"\"")
    }

    buildTypes {
        release {
            // The key every GitHub release is signed with (CN=Emanuel5014). A copy signed otherwise cannot take
            // those updates, so the updater says so instead of downloading them.
            buildConfigField(
                "String", "RELEASE_CERT_SHA256",
                "\"e7410df96be8deec8243dce02f58da247ca086c31a8a5254ea8b668fbc8be479\""
            )
            isMinifyEnabled = false
            manifestPlaceholders["appName"] = "Trainable"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appName"] = "Trainable (debug)"
            // Testing the updater: -Ptrainable.updateFeed=http://127.0.0.1:8765/releases.json turns it on in debug and
            // fast builds and points it at a local release list. Never reaches a release build.
            buildConfigField(
                "String", "UPDATE_FEED_URL",
                "\"${(project.findProperty("trainable.updateFeed") as String?).orEmpty()}\""
            )
            // Testing the "other key" case: -Ptrainable.updateCert=<sha256 that is not the debug key's>
            buildConfigField(
                "String", "RELEASE_CERT_SHA256",
                "\"${(project.findProperty("trainable.updateCert") as String?).orEmpty()}\""
            )
        }
        // The debug build without the debugger. A debuggable app runs its code interpreted, so animations stutter on a
        // phone; this one runs like a release build. Same app id and signing key as debug, so the data stays.
        create("fast") {
            initWith(getByName("debug"))
            isDebuggable = false
            manifestPlaceholders["appName"] = "Trainable (debug)"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "META-INF/native-image/**"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Jetpack Glance AppWidgets
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Room
    implementation(project(":shared"))
    implementation(project(":sharedUi"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Navigation & Serialization
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    
    // Datastore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Material Components
    implementation(libs.material)

    // OkHttp
    implementation(libs.okhttp)

    // LiteRT-LM (on-device LLM inference, Gemma 4)
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")

// Haze - Frosted Glass Blur
    implementation(libs.haze)
    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)

    // Ktor Server (Local Web Dashboard)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)

    // Graphics Shapes (for morph animations)
    implementation(libs.androidx.graphics.shapes)

    // Biometric & Fragment support
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)

    // ProcessLifecycleOwner for background detection
    implementation(libs.lifecycle.process)


    testImplementation(libs.junit)
    // Real org.json for JVM unit tests (the Android stub returns defaults)
    testImplementation("org.json:json:20260814")
    // Nextcloud WebDAV over OkHttp against a local https server (same OkHttp version as the app)
    testImplementation("com.squareup.okhttp3:mockwebserver3:${libs.versions.okhttp.get()}")
    testImplementation("com.squareup.okhttp3:okhttp-tls:${libs.versions.okhttp.get()}")

    androidTestImplementation(libs.androidx.junit)
    // The shared UI's strings, for tests of the screens that use them
    androidTestImplementation(libs.jb.compose.resources)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}