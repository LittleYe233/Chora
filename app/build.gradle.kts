import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "2.0.0"
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.craftworks.music"
    compileSdk = 36

    androidResources {
        generateLocaleConfig = true
    }

    // START signingConfigs
    val properties = Properties()
    val propertiesFile: File? = rootProject.file("local.properties")
    if (propertiesFile != null && propertiesFile.exists()) {
        properties.load(propertiesFile.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file("key.jks")
            storePassword = properties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = "key0"
            keyPassword = properties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }
    // END signingConfigs

    // START build config
    // A hack to determine if it is a release build
    val isReleaseTask = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
    val baseVersionName = "1.30.0"
    val baseVersionCode = 300
    val patchVersionCode = 0

    defaultConfig {
        applicationId = "com.craftworks.music"
        minSdk = 23
        targetSdk = 36
        versionCode = if (isReleaseTask) {
            // x.y.z -> xyz000 + patch
            (baseVersionCode * 1000) + patchVersionCode
        } else {
            // x.y.z -> xyz
            baseVersionCode
        }
        versionName = baseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = false
            isProfileable = true
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Chora Debug")
        }
    }
    // END build config

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    kotlin {
        jvmToolchain(21)
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.reorderable)
    implementation(libs.androidx.media)

    implementation(libs.androidx.material.icons.core)

    implementation(libs.konsume.xml)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.composefadingedges)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}