plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.madproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.madproject"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.drawerlayout)
    implementation(libs.viewpager)
    implementation(libs.recyclerview)
    implementation(libs.palette)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    // Testing dependencies (temporarily disabled for build fix)
    // testImplementation(libs.junit)
    // testImplementation("org.robolectric:robolectric:4.11.1")
    // testImplementation("org.mockito:mockito-core:5.4.0")
    // testImplementation("androidx.test:core:1.5.0")
    // testImplementation("androidx.test.ext:junit:1.1.5")
    
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // androidTestImplementation("org.robolectric:robolectric:4.11.1")
    // androidTestImplementation("androidx.test:core:1.5.0")
}