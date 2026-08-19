// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // detekt 1.23.x embeds a Kotlin compiler that rejects the newer --jvm-target
        // value auto-detected from the AGP 9 / Kotlin 2.2 toolchain; pin to a supported one.
        jvmTarget = "17"
    }
}
