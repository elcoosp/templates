plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.1.20-RC3-1.0.31"
    kotlin("plugin.serialization") version "2.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
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
    implementation("org.lynxsdk.lynx:lynx:3.2.0-rc.0")
    implementation("org.lynxsdk.lynx:lynx-jssdk:3.2.0-rc.0")
    implementation("org.lynxsdk.lynx:lynx-trace:3.2.0-rc.0")
    implementation("org.lynxsdk.lynx:primjs:2.11.1-rc.2")

    implementation("org.lynxsdk.lynx:lynx-service-image:3.2.0-rc.0")
    implementation("com.facebook.fresco:fresco:2.6.0")
    implementation("com.facebook.fresco:animated-gif:2.6.0")
    implementation("com.facebook.fresco:animated-webp:2.6.0")
    implementation("com.facebook.fresco:webpsupport:2.6.0")
    implementation("com.facebook.fresco:animated-base:2.6.0")
    implementation("org.lynxsdk.lynx:lynx-service-log:3.2.0-rc.0")
    implementation("org.lynxsdk.lynx:lynx-service-http:3.2.0-rc.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Lynxpo
    implementation("com.google.dagger:dagger-compiler:2.55")
    ksp("com.google.dagger:dagger-compiler:2.55")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    // implementation("dev.adamko.kxstsgen:kxs-ts-gen-core:0.2.1")
    implementation(kotlin("reflect"))

    implementation("lynxpo.ktts:ktts-plugin:1.0.0")
    ksp("lynxpo.ktts:ktts-plugin:1.0.0")
}
