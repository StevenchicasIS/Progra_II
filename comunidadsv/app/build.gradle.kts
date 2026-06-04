plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.comunidadsv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.comunidadsv"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // ML Kit para visión (gratuito, on-device)
    implementation("com.google.mlkit:image-labeling:17.0.7")
// Para detección de texto ofensivo (TensorFlow Lite)
    implementation("org.tensorflow:tensorflow-lite-task-text:0.4.0")
    implementation("com.google.mlkit:image-labeling:17.0.7")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // OSMDUROID - Mapa gratuito
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}