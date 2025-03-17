plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.amazon.ivs.chatdemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amazon.ivs.chatdemo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "STREAM_URL", "\"https://760b256a3da8.us-east-1.playback.live-video.net/api/video/v1/us-east-1.049054135175.channel.6tM2Z9kY16nH.m3u8\"")
        buildConfigField("String", "API_URL", "\"https://ENTER_YOUR_API_URL_HERE\"")
        buildConfigField("String", "CHAT_ROOM_ID", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.exifinterface)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Glide
    implementation(libs.glide)
    ksp(libs.compiler)

    // Retrofit
    implementation(libs.okhttp)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Timber
    implementation(libs.timber)

    // Amazon IVS
    implementation(libs.ivs.player)
    implementation(libs.ivs.chat.messaging)
}
