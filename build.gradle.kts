// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // The ktlint Gradle plugin bundles an older engine; pin the one
    // `.editorconfig` is written against.
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(rootProject.libs.versions.ktlintEngine)
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // detekt 1.23.x embeds a Kotlin compiler that rejects the newer --jvm-target
        // value auto-detected from the AGP 9 / Kotlin 2.4 toolchain; pin to a supported one.
        jvmTarget = "17"
    }

    // Run the project's own rules (the `panther` rule set in
    // config/detekt/detekt.yml) in every module, including the one that
    // defines them.
    dependencies {
        add("detektPlugins", project(":detekt-rules"))
    }

    // Every Kotlin compilation first formats the module's sources with
    // ktlint, then lints them with ktlint and detekt once it finishes. Lint
    // errors fail the build; findings weighted 0 in detekt.yml are reported
    // without failing it.
    val formatTasks = tasks.matching { it.name.endsWith("SourceSetFormat") }
    val lintTasks = tasks.matching { it.name.endsWith("SourceSetCheck") || it.name == "detekt" }
    tasks.matching { it.name.startsWith("compile") && it.name.endsWith("Kotlin") }.configureEach {
        dependsOn(formatTasks)
        finalizedBy(lintTasks)
    }
}
