import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

// Release signing. Credentials come from the environment (preferred) or a
// git-ignored `key.properties`. When neither is present — e.g. on the F-Droid
// build server — the release APK is left unsigned, which is exactly what
// F-Droid's reproducible build wants: it signs and byte-compares against the
// published binary itself.
val keystoreProperties = Properties()
rootProject.file("key.properties").let { f ->
    if (f.exists()) f.inputStream().use { keystoreProperties.load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env) ?: keystoreProperties.getProperty(prop)

android {
    namespace = "com.github.mkalmousli.floating_mute"
    compileSdk = 34

    viewBinding.enable = true

    android.buildFeatures.buildConfig = true
    defaultConfig {
        applicationId = "com.github.mkalmousli.floating_mute"
        minSdk = 21
        targetSdk = 34
        versionCode = 4
        versionName = "4.0.0"

        defaultConfig {
            // BUILD_TIME is not constant, so here we type it fixed:
            // Next version will remove this info for the F-Droid.
            buildConfigField( "String", "BUILD_TIME", "\"Sat Sep 06 12:00:00 UTC 2026\"")
            buildConfigField( "String", "RELEASE_DAY", "\"2026/09/06\"")
        }
    }

    signingConfigs {
        create("release") {
            val store = signingValue("MKALMOUSLI_SIGN_STORE", "storeFile")
            val storePass = signingValue("MKALMOUSLI_SIGN_PASS", "storePassword")
            val alias = signingValue("MKALMOUSLI_SIGN_ALIAS", "keyAlias")
            if (store != null && storePass != null && alias != null) {
                storeFile = file(store)
                storePassword = storePass
                keyAlias = alias
                keyPassword = signingValue("MKALMOUSLI_SIGN_KEY_PASS", "keyPassword") ?: storePass
            }
        }
    }

    buildTypes {
        release {
            // Only sign when credentials were supplied; F-Droid builds unsigned.
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // disable DependencyInfoBlock, for F-Droid.
    //See: https://gitlab.com/fdroid/admin/-/issues/367
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}