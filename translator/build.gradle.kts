plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "us.neotechnica.panther.translator"
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
        // Baselined Lint warnings. Regenerate with
        // `./gradlew updateLintBaseline` after fixing any of them.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(project(":subsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.webkit)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.mlkit.language.id)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
