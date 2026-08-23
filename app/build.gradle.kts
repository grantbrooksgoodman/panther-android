import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Release signing is driven by a gitignored keystore.properties at the
// repo root (see keystore.properties.template). Absent it, the release
// build stays unsigned so the config never blocks a fresh checkout.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
    }

android {
    namespace = "us.neotechnica.panther"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "us.neotechnica.panther"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            versionNameSuffix = "-dev"
            buildConfigField("String", "NETWORK_ENVIRONMENT", "\"development\"")
        }
        create("staging") {
            dimension = "environment"
            versionNameSuffix = "-staging"
            buildConfigField("String", "NETWORK_ENVIRONMENT", "\"staging\"")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "NETWORK_ENVIRONMENT", "\"production\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// --- Per-compile build-number stamp (mirrors the iOS Run Script phase) ---
// On every compile this increments version.properties and writes a
// build_info.properties asset (wired into each variant's generated
// assets) that the app reads at runtime through Build.initialize, so the
// build number, build date, and first-compile date advance exactly as
// they do on iOS via the Run Script build phase.
abstract class StampBuildInfoTask : DefaultTask() {
    @get:Internal
    abstract val versionPropertiesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun stamp() {
        val file = versionPropertiesFile.get().asFile
        val properties = Properties()
        if (file.exists()) file.inputStream().use { properties.load(it) }

        val placeholderFirstCompileDate = 1183100400L
        val buildNumber = (properties.getProperty("buildNumber") ?: "0").toInt() + 1
        val buildDate = System.currentTimeMillis() / 1000L
        val storedFirstCompileDate =
            (properties.getProperty("firstCompileDate") ?: placeholderFirstCompileDate.toString()).toLong()
        val firstCompileDate =
            if (storedFirstCompileDate == placeholderFirstCompileDate) buildDate else storedFirstCompileDate

        properties.setProperty("buildNumber", buildNumber.toString())
        properties.setProperty("buildDate", buildDate.toString())
        properties.setProperty("firstCompileDate", firstCompileDate.toString())
        file.outputStream().use {
            properties.store(it, "Auto-incremented per compile (mirrors the iOS Run Script build-number bump).")
        }

        val assetFile = outputDirectory.get().asFile.resolve("build_info.properties")
        assetFile.parentFile.mkdirs()
        assetFile.writeText("buildNumber=$buildNumber\nbuildDate=$buildDate\nfirstCompileDate=$firstCompileDate\n")
    }
}

androidComponents {
    onVariants { variant ->
        val stampTask =
            tasks.register<StampBuildInfoTask>("stamp${variant.name.replaceFirstChar { it.uppercase() }}BuildInfo") {
                versionPropertiesFile.set(layout.projectDirectory.file("version.properties"))
                outputs.upToDateWhen { false }
            }
        variant.sources.assets?.addGeneratedSourceDirectory(stampTask, StampBuildInfoTask::outputDirectory)
    }
}

dependencies {
    implementation(project(":subsystem"))
    implementation(project(":networking"))
    implementation(project(":design-system"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.libphonenumber)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
