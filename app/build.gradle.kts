plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.softdevcode.finflow_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.softdevcode.finflow_android"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.2.2-finflow"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://finflow-backend-production.up.railway.app/\"")
        }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "BASE_URL", "\"https://finflow-backend-production.up.railway.app/\"")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    // JUnit 4 for local JVM unit tests (src/test)
    testImplementation("junit:junit:4.13.2")

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)

    // Activity + Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.litert.support.api)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    implementation(libs.play.services.games.v2)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // DataStore (token storage)
    implementation(libs.datastore.preferences)

    // Kotlin Coroutines
    implementation(libs.coroutines.android)

    // Charts
    implementation(libs.mpandroidchart)

    // Coil (images)
    implementation(libs.coil.compose)

    // Splash screen
    implementation(libs.splash.screen)

    // JWT decode
    implementation(libs.jwt.decode)
}
