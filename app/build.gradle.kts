plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.arcusfoundry.labs.pixelpilot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arcusfoundry.labs.pixelpilot"
        minSdk = 33
        targetSdk = 36
        // Derived from git tag in the release workflow. For local/debug builds
        // these default values are used, but they're never shipped to users.
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME") ?: "0.1.0-dev"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                val f = file(keystorePath)
                if (f.exists()) {
                    storeFile = f
                    storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD") ?: ""
                    keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "pixel-pilot"
                    keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
                    storeType = "pkcs12"
                }
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
            val sc = signingConfigs.getByName("release")
            if (sc.storeFile != null) {
                signingConfig = sc
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.common)
    implementation(libs.media3.effect)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
