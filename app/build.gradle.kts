plugins {
    alias(libs.plugins.android.application)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")

}

android {
    namespace = "io.github.monitbisht.iterack"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.monitbisht.iterack"
        minSdk = 30
        targetSdk = 35
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
    buildFeatures {
        viewBinding = true
    }

}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.circularprogressbar)
    implementation(libs.circleimageview)
    implementation(libs.materialcalendarview)
    implementation(libs.mpandroidchart)

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:5.0.5")

    // Material Components for Android
    implementation("com.google.android.material:material:1.12.0")

    // Slide Button
    implementation("com.ncorti:slidetoact:0.11.0")

    // Work Manager
    val work_version = "2.11.0"
    implementation ("androidx.work:work-runtime:$work_version")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))


    // When using the BoM, don't specify versions in Firebase dependencies

    //For Firebase Analytics
    implementation("com.google.firebase:firebase-analytics")

    // For Cloud Firestore (Database)
    implementation("com.google.firebase:firebase-firestore")

    // For Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // For Firebase AI Logic(Gemini)
    implementation("com.google.firebase:firebase-ai")

    // Required for one-shot operations (to use `ListenableFuture` from Guava Android)
    implementation("com.google.guava:guava:31.0.1-android")

    // Required for streaming operations (to use `Publisher` from Reactive Streams)
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    //The dependencies for the Credential Manager libraries with their versions
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

}


