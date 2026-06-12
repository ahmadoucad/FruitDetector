// Fichier : app/build.gradle.kts (niveau application)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)          // KSP avant Hilt (obligatoire)
    alias(libs.plugins.kotlin.parcelize)    // Pour @Parcelize sur les sealed interfaces
    alias(libs.plugins.dagger.hilt)         // Injection de dépendances
    alias(libs.plugins.androidx.navigation.safeargs) // Navigation safe args
}

android {
    namespace = "fr.mastersd.sime.cheikhahmadoudiop.fruitdetector"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.mastersd.sime.cheikhahmadoudiop.fruitdetector"
        minSdk = 21                         // Android 5.0 comme recommandé dans le cours
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // View Binding (comme dans le cours de M. Blossier)
    buildFeatures {
        viewBinding = true
    }

    // Permet d'inclure le modèle TFLite dans le dossier assets sans compression
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Architecture Components (ViewModel + LiveData)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    // Fragment (by viewModels() comme dans le cours)
    implementation(libs.androidx.fragment)

    // Navigation (Fragments + NavGraph)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Hilt (Injection de dépendances)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    // Room (Base de données locale)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines (appels asynchrones comme dans le cours)
    implementation(libs.kotlinx.coroutines.android)

    // CameraX (caméra en temps réel)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // TensorFlow Lite (Interpreter classique pour la classification)
    // On utilise uniquement l'Interpreter, pas la Task Library,
    // ce qui évite les conflits de versions (litert vs tensorflow-lite).
    implementation(libs.tensorflow.lite)

    // Retrofit (API Open Food Facts)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // RecyclerView (liste de l'historique)
    implementation(libs.androidx.recyclerview)
}