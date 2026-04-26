plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val pluginName = "FruitMergeAdMob"
val pluginVersion = "1.0.0"

android {
    namespace = "com.fruitmerge.admob"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        manifestPlaceholders["pluginName"] = pluginName
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
    // Godot Engine library — provided at build time
    compileOnly(fileTree("libs") { include("*.aar", "*.jar") })

    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:25.2.0")
}
