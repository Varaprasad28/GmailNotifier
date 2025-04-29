plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.gmailnotifier"
    compileSdk = 35
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    defaultConfig {
        applicationId = "com.example.gmailnotifier"
        minSdk = 26
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.google.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.gmail.api)
    implementation(libs.gmail.api1)
    implementation(libs.oauth.client.jetty)
    implementation(libs.google.api.client)


}
