plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.1.20-1.0.31"
    kotlin("plugin.serialization") version "2.1.20"
}

android {
    namespace = "com.{{org}}.{{project_name|camel_case}}"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.{{org}}.{{project_name|camel_case}}"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = "21" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    api(libs.commons.math3)
    implementation(libs.guava)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Lynx dependencies
    implementation(libs.lynx)
    implementation(libs.lynx.jssdk)
    implementation(libs.lynx.trace)

    implementation(libs.lynx.service.log)
    implementation(libs.lynx.service.http)
    implementation(libs.lynx.service.image)

    implementation(libs.primjs)
    implementation(libs.fresco)
    implementation(libs.animated.gif)
    implementation(libs.animated.webp)
    implementation(libs.webpsupport)
    implementation(libs.animated.base)
    implementation(libs.okhttp)

    implementation(libs.lynxpo)
}
