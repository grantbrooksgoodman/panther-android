import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
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
        minSdk = 28
        targetSdk = 37
        versionName = "1.0.0"
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
            buildConfigField("String", "NETWORK_ENVIRONMENT", "\"development\"")
        }
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "NETWORK_ENVIRONMENT", "\"staging\"")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "NETWORK_ENVIRONMENT", "\"production\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Warnings that predate Lint joining the gate. Regenerate with
        // `./gradlew updateLintBaseline` after fixing any of them.
        baseline = file("lint-baseline.xml")
    }
}

// --- Per-build build-number stamp (mirrors the iOS Run Script phase) ---
// Once per Gradle invocation, stampBuildInfo increments version.properties
// and writes build_info.properties, which every variant packages as a
// generated asset (through its copy<Variant>BuildInfo task) and the app
// reads at runtime through Build.initialize. The build number, build date,
// and first-compile date therefore advance exactly as they do on iOS via the
// Run Script build phase, and every variant's versionCode is that same build
// number, as the iOS CFBundleVersion is.
abstract class StampBuildInfoTask : DefaultTask() {
    @get:Internal
    abstract val versionPropertiesFile: RegularFileProperty

    @get:OutputFile
    abstract val buildInfoFile: RegularFileProperty

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

        val assetFile = buildInfoFile.get().asFile
        assetFile.parentFile.mkdirs()
        assetFile.writeText("buildNumber=$buildNumber\nbuildDate=$buildDate\nfirstCompileDate=$firstCompileDate\n")
    }
}

abstract class CopyBuildInfoTask : DefaultTask() {
    @get:InputFile
    abstract val buildInfoFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        buildInfoFile.get().asFile.copyTo(
            outputDirectory.get().asFile.resolve("build_info.properties"),
            overwrite = true,
        )
    }
}

val stampBuildInfo =
    tasks.register<StampBuildInfoTask>("stampBuildInfo") {
        versionPropertiesFile.set(layout.projectDirectory.file("version.properties"))
        buildInfoFile.set(layout.buildDirectory.file("generated/buildInfo/build_info.properties"))
        outputs.upToDateWhen { false }
    }

val buildNumber =
    stampBuildInfo
        .flatMap { it.buildInfoFile }
        .map { file ->
            file.asFile
                .readLines()
                .first { it.startsWith("buildNumber=") }
                .substringAfter("=")
                .toInt()
        }

androidComponents {
    onVariants { variant ->
        val copyTask =
            tasks.register<CopyBuildInfoTask>("copy${variant.name.replaceFirstChar { it.uppercase() }}BuildInfo") {
                buildInfoFile.set(stampBuildInfo.flatMap { it.buildInfoFile })
            }
        variant.sources.assets?.addGeneratedSourceDirectory(copyTask, CopyBuildInfoTask::outputDirectory)
        variant.outputs.forEach { it.versionCode.set(buildNumber) }
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
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.play.review.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.common)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.org.json)
    debugImplementation(libs.firebase.appcheck.debug)
    debugImplementation(libs.androidx.compose.ui.tooling)
    releaseImplementation(libs.firebase.appcheck.playintegrity)
}
