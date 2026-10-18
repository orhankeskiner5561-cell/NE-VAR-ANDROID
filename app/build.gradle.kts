plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.nevar.discovery"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.nevar.discovery"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("nevarVersionCode").orNull?.toIntOrNull() ?: 5
        versionName = providers.gradleProperty("nevarVersionName").orNull ?: "0.3.2"
    }
    signingConfigs {
        create("permanent") {
            storeFile = file(System.getenv("NEVAR_KEYSTORE_PATH") ?: "nevar-release.jks")
            storePassword = System.getenv("NEVAR_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("NEVAR_KEY_ALIAS")
            keyPassword = System.getenv("NEVAR_KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("permanent")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
