import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.connectifychattingapp"

    // Fixed: Standard integer for SDK version
    compileSdk = 36

    // Fixed: Packaging options MUST be here to fix the 16 KB error
    packaging {
        jniLibs {
            // This aligns Agora's .so files to 16 KB boundaries
            useLegacyPackaging = false
        }
    }
    defaultConfig {
        applicationId = "com.example.connectifychattingapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- API KEY CONFIGURATION ---
        // This reads the key from local.properties safely
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        val apiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

        // Generate the BuildConfig field
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")

        externalNativeBuild {
            cmake {
                // This is the critical flag for 16 KB support
                arguments("-DANDROID_EXTRACT_NATIVE_LIBS=FALSE")
                cppFlags("-Wl,-z,max-page-size=16384")
            }
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
        viewBinding =true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.recyclerview)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.code.gson:gson:2.10.1")
    // Gemini SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.6.0")
    // Async support for Java
    implementation("com.google.guava:guava:31.1-android")
    implementation("io.agora.rtc:full-sdk:4.6.1")
}