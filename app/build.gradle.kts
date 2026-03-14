import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.connectifychattingapp"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.connectifychattingapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- API KEY CONFIGURATION ---
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        val apiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")

        externalNativeBuild {
            cmake {
                // Critical flags for 16 KB page size support (Agora)
                arguments("-DANDROID_EXTRACT_NATIVE_LIBS=FALSE")
                cppFlags("-Wl,-z,max-page-size=16384")
            }
        }
    }

    // Critical: Packaging options to fix potential 16 KB loading errors
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures{
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX & UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    // Auth & Google Services
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Firebase
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Image Loading & UI Helpers
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.squareup.picasso:picasso:2.8")

    // --- GLIDE FIXED FOR JAVA IN KTS ---
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.messaging)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Tools
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:31.1-android")

    // SDKs
    implementation("com.google.ai.client.generativeai:generativeai:0.6.0")
    implementation("io.agora.rtc:full-sdk:4.6.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}