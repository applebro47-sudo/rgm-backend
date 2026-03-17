plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.kotlinKapt)
}

android {
    namespace = "com.pushkar.RGM"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pushkar.RGM"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.material)
    implementation(libs.androidxConstraintlayout)
    implementation(libs.androidxNavigationFragmentKtx)
    implementation(libs.androidxNavigationUiKtx)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Firebase
    implementation(platform(libs.firebaseBom))
    implementation(libs.firebaseAuthKtx)
    implementation(libs.firebaseDatabaseKtx)
    implementation(libs.firebaseStorageKtx)

    // Room Database
    implementation(libs.roomRuntime)
    implementation(libs.roomKtx)
    kapt(libs.roomCompiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.androidxEspressoCore)
}
