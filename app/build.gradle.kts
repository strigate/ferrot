import com.android.build.api.dsl.ApplicationDefaultConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val googleServicesPropertiesFile = rootProject.file("google-services.properties")
val googleServicesProperties = Properties()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
}

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
if (googleServicesPropertiesFile.exists()) {
    googleServicesProperties.load(FileInputStream(googleServicesPropertiesFile))
}

object BuildInfo {
    const val PACKAGE_NAME = "org.strigate.ferrot"
    const val BASE_VERSION = "1.4.1"
    const val VERSION_CODE = 18
    const val VERSION_NAME = "$BASE_VERSION-$VERSION_CODE"
    const val RELEASE_APK_NAME = "ferrot"
}

android {
    namespace = BuildInfo.PACKAGE_NAME
    compileSdk = 36

    defaultConfig {
        applicationId = "org.strigate.ferrot"
        minSdk = 30
        targetSdk = 36
        versionCode = BuildInfo.VERSION_CODE
        versionName = BuildInfo.VERSION_NAME
        stringField("VERSION", BuildInfo.BASE_VERSION)
        stringField("VERSION_TAG", "v${BuildInfo.BASE_VERSION}")
        applyFirebaseProperties()
        ndk {
            ndkVersion = "29.0.14206865"
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
            versionNameSuffix = "-D"
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
            versionNameSuffix = "-R"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    splits {
        abi {
            isEnable = false
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf("**/*.so")
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

tasks.register<Copy>("renameReleaseApk") {
    val releaseDir = layout.buildDirectory.dir("outputs/apk/release")
    from(releaseDir)
    include("app-release.apk")
    into(releaseDir)
    rename {
        "${BuildInfo.RELEASE_APK_NAME}-release.apk"
    }
    doFirst {
        println("Renaming APK")
    }
}

tasks.named("renameReleaseApk") {
    mustRunAfter("createReleaseApkListingFileRedirect")
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy("renameReleaseApk")
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

private fun ApplicationDefaultConfig.stringField(name: String, value: String) {
    buildConfigField("String", name, value.escapeForBuildConfig())
}

private fun ApplicationDefaultConfig.resString(name: String, value: String) {
    resValue("string", name, value.escapeForBuildConfig())
}

private fun Properties.getString(key: String): String {
    return (this[key] as? String)?.trim().orEmpty()
}

private fun String.escapeForBuildConfig(): String {
    return "\"" + this.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

dependencies {
    // Android
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
    // Dagger / Hilt
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    // Hilt
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
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
