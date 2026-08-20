import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "io.github.darrindeyoung791.habitpulse"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.darrindeyoung791.habitpulse"
        minSdk = 26
        targetSdk = 37
        versionCode = 199
        versionName = "0.8.51-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("zh-rCN", "zh-rTW", "zh-rHK", "en-rUS", "en-rGB")
    }
    lint {
        abortOnError = false
    }
    compileSdkMinor = 0
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

val renameReleaseApk by tasks.registering {
    doFirst {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val versionName = android.defaultConfig.versionName ?: "unknown"
        val newName = "HabitPulse_${versionName}_${timestamp}.apk"

        // 遍历所有可能的 release APK 输出目录
        listOf(
            layout.buildDirectory.get().asFile.resolve("outputs/apk/release"),
            projectDir.resolve("app/release"),
            projectDir.resolve("release")
        ).forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.filter { it.extension == "apk" && !it.name.startsWith("HabitPulse") }?.forEach { apk ->
                    val newFile = File(dir, newName)
                    apk.copyTo(newFile, overwrite = true)
                    apk.delete()
                }
            }
        }
    }
}

afterEvaluate {
    tasks.named("assembleRelease") { finalizedBy(renameReleaseApk) }
    tasks.named("packageRelease") { finalizedBy(renameReleaseApk) }
}

dependencies {
    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Lifecycle and ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.core.splashscreen)
    // Material library required for DynamicColors.applyToActivitiesIfAvailable
    // (also bundles com.google.android.material.color.utilities used for seed-based accent palettes)
    implementation(libs.com.google.android.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // AboutLibraries for open source licenses display
    implementation(libs.aboutlibraries.compose.m3)

    // Reorderable for drag-and-drop sorting
    implementation(libs.reorderable)

    // SwipeRefreshLayout for pull-to-refresh
    implementation(libs.androidx.swiperefreshlayout)

    // Gson for JSON parsing
    implementation(libs.gson)

    // Biometric for gated API key reveal (CryptoObject + BiometricPrompt)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
}