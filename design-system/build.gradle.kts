plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "us.neotechnica.panther.designsystem"
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
    buildFeatures {
        compose = true
    }

    lint {
        // Baselined Lint warnings. Regenerate with
        // `./gradlew updateLintBaseline` after fixing any of them.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    api(project(":subsystem"))
    implementation(project(":translator"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
