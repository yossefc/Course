plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("org.jetbrains.kotlin.kapt") // Utilisation de kapt comme plugin ici, pas via un alias
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.coursessupermarche"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.coursessupermarche"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}
configurations.all {
    resolutionStrategy {
        // Utiliser une version spécifique de androidx.media3
        // Versions 1.0.0, 1.1.0, 1.2.0 sont compatibles avec compileSdk 34
        force("androidx.media3:media3-common:1.0.0")

        // Le package media3-common-ktx peut ne pas être disponible en 1.0.0,
        // mais si une dépendance le requiert, nous pouvons forcer la version la plus ancienne disponible
        force("androidx.media3:media3-common-ktx:1.0.0-alpha03")

        // Autres composants Media3 qui pourraient être utilisés
        force("androidx.media3:media3-exoplayer:1.0.0")
        force("androidx.media3:media3-ui:1.0.0")
        force("androidx.media3:media3-session:1.0.0")
        force("androidx.media3:media3-datasource:1.0.0")
    }
}
dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)


    // Activity et Fragment (une seule version)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.common.ktx.v121)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.material.v1120)

    configurations.all {
        exclude(group = "androidx.media3", module = "media3-common-ktx")
    }
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.android)
    implementation(libs.androidx.media3.common.ktx)

    // Room pour le stockage local
    val roomVersion = "2.6.1"
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Support pour Kotlin
    kapt(libs.androidx.room.compiler)

    // Hilt pour l'injection de dépendances
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}