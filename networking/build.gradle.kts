plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "us.neotechnica.panther.networking"
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
        buildConfig = true
        compose = true
    }

    lint {
        // Warnings that predate Lint joining the gate. Regenerate with
        // `./gradlew updateLintBaseline` after fixing any of them.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    api(project(":design-system"))
    api(project(":subsystem"))
    api(project(":translator"))
    api(platform(libs.firebase.bom))
    api(libs.firebase.analytics)
    api(libs.firebase.appcheck)
    api(libs.firebase.auth)
    api(libs.firebase.database)
    api(libs.firebase.messaging)
    api(libs.firebase.storage)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
