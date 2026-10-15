plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "us.neotechnica.panther.subsystem"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Warnings that predate Lint joining the gate. Regenerate with
        // `./gradlew updateLintBaseline` after fixing any of them.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
