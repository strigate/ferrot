import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.BuildType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val googleServicesPropertiesFile = rootProject.file("google-services.properties")
val googleServicesProperties = Properties()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
if (googleServicesPropertiesFile.exists()) {
    googleServicesProperties.load(FileInputStream(googleServicesPropertiesFile))
}

android {
    namespace = "org.strigate.ferrot"
    compileSdk = 36
    defaultConfig {
        val version = "0.1.0"
        applicationId = "org.strigate.ferrot"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = buildVersionName(version, versionCode)
        stringField("VERSION_TAG", "v$version")
        applyFirebaseProperties()
        applyMockBootstrapProperty(false)
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("keystore") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro",
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("keystore")
            } else {
                signingConfigs.getByName("debug")
            }
            applyMockBootstrapProperty(false)
        }
        release {
            isDebuggable = false
            isProfileable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro",
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("keystore")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    splits {
        abi {
            isEnable = false
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf("**/*.zip.so")
        }
    }
    applicationVariants.all {
        outputs
            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .all {
                it.outputFileName = "ferrot_${if (isDebugBuildType()) "debug" else "release"}.apk"
                false
            }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}


private fun ApplicationDefaultConfig.applyMockBootstrapProperty(enabled: Boolean) {
    boolField("BOOTSTRAP_MOCK_DATA", enabled)
}

private fun BuildType.applyMockBootstrapProperty(enabled: Boolean) {
    boolField("BOOTSTRAP_MOCK_DATA", enabled)
}

private fun ApplicationDefaultConfig.applyFirebaseProperties(
    includeResString: Boolean = true,
) {
    val firebaseAppId = googleServicesProperties.getString("firebaseAppId")
    val firebaseApiKey = googleServicesProperties.getString("firebaseApiKey")
    val firebaseProjectId = googleServicesProperties.getString("firebaseProjectId")
    val firebaseSenderId = googleServicesProperties.getString("firebaseSenderId")
    val firebaseBucket = googleServicesProperties.getString("firebaseBucket")

    stringField("FIREBASE_APP_ID", firebaseAppId)
    stringField("FIREBASE_API_KEY", firebaseApiKey)
    stringField("FIREBASE_PROJECT_ID", firebaseProjectId)
    stringField("FIREBASE_SENDER_ID", firebaseSenderId)
    stringField("FIREBASE_BUCKET", firebaseBucket)
    if (includeResString && firebaseAppId.isNotBlank()) {
        resString("google_app_id", firebaseAppId)
    }
}

private fun ApplicationDefaultConfig.boolField(name: String, value: Boolean) {
    buildConfigField("boolean", name, value.toString())
}

private fun BuildType.boolField(name: String, value: Boolean) {
    buildConfigField("boolean", name, value.toString())
}

private fun ApplicationDefaultConfig.stringField(name: String, value: String) {
    buildConfigField("String", name, value.escapeForBuildConfig())
}

private fun ApplicationDefaultConfig.resString(name: String, value: String) {
    resValue("string", name, value.escapeForBuildConfig())
}

private fun Properties.getString(key: String): String = (this[key] as? String)?.trim().orEmpty()

private fun String.escapeForBuildConfig(): String =
    "\"" + this.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun buildVersionName(baseVersion: String, versionCode: Int?): String {
    val timestamp = SimpleDateFormat("yyyyMMdd").format(Date())
    val buildType = if (isDebugBuildType()) "D" else "R"
    val versionCodePart = versionCode?.let { "-$it-" } ?: "-"
    return "$baseVersion$versionCodePart$buildType-$timestamp"
}

private fun isDebugBuildType(): Boolean {
    val taskNames = gradle.startParameter.taskNames
    val isDebugTask = taskNames.any { taskName ->
        taskName.contains("Debug", ignoreCase = true)
    }
    return isDebugTask
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.datastore.preferences)
    // Lifecycle
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    // Material
    implementation(libs.android.material)
    // Activity
    implementation(libs.androidx.activity.compose)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // Work Manager
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    // Hilt
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Youtubedl-Android
    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)
    implementation(libs.youtubedl.android.aria2c)
    // UUID Creator
    implementation(libs.uuid.creator)
    // Coil
    implementation(libs.coil)
    implementation(libs.coil.compose)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
