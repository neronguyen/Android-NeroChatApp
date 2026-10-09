import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.neronguyen.chat.core.network"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 26

        // TODO: Network & Build Configuration Refactoring
        // 1. [Architecture] Move BASE_URL management to :app module; inject @BaseUrl String via Hilt into this library.
        // 2. [Gradle Lifecycle] Do not throw exceptions directly inside release build block (breaks debug during Configuration phase); validate at task execution time.
        // 3. [CI/CD & Config Cache] Read BASE_URL via Gradle Providers (providers.gradleProperty / environmentVariable) instead of raw local.properties I/O.
        // 4. [Network & Security] Remove "http://localhost/" fallback (blocked cleartext traffic on Android 28+); enforce HTTPS with trailing '/' for release builds.
        val secretsFile = rootProject.file("local.properties")
        val properties = Properties()
        if (secretsFile.exists()) {
            secretsFile.inputStream().use { properties.load(it) }
        }

        val baseUrl = properties.getProperty("BASE_URL") ?: "http://localhost/"
        buildConfigField(type = "String", name = "BASE_URL", value = "\"$baseUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+ContextParameters")
    }
}

dependencies {
    implementation(projects.core.model)

    // Arrow
    implementation(libs.arrow.core)

    // Coil
    implementation(libs.coil.kt.network.okhttp)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // KotlinX
    implementation(libs.kotlinx.serialization.json)

    // Retrofit
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
}
