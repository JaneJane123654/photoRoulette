import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.GradleException
import java.util.Properties

plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
}

android {
    namespace = "com.example.photoroulette"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.photoroulette"
        minSdk = 23
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.3"
    }

    val signingSecrets = Properties().apply {
        val secretsFile = rootProject.file(".signing_secrets")
        if (!secretsFile.exists()) {
            throw GradleException("Missing .signing_secrets in project root")
        }
        secretsFile.inputStream().use(::load)
    }

    val storePasswordValue = signingSecrets.getProperty("STORE_PASSWORD")
        ?: throw GradleException("STORE_PASSWORD missing in .signing_secrets")
    val keyPasswordValue = signingSecrets.getProperty("KEY_PASSWORD")
        ?: throw GradleException("KEY_PASSWORD missing in .signing_secrets")

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.keystore")
            storePassword = storePasswordValue
            keyAlias = "key0"
            keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.documentfile:documentfile:1.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
