// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.kover)
}

val slackComposeLintChecks = libs.slack.compose.lint.checks

allprojects {
    plugins.withId("com.android.library") {
        dependencies.add("lintChecks", slackComposeLintChecks)
    }
    plugins.withId("com.android.application") {
        dependencies.add("lintChecks", slackComposeLintChecks)
    }
}

// --- Static analysis & coverage -------------------------------------------------------------

val composeRulesDetekt = libs.compose.rules.detekt

// Generated code and previews are excluded from coverage. The root report aggregates the modules
// and needs the same filters as the per-module reports.
val koverExcludes: kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig.() -> Unit = {
    excludes {
        classes(
            "*_Factory",
            "*_Factory$*",
            "*_Provide*Factory*",
            "*_HiltModules*",
            "*_HiltComponents*",
            "Hilt_*",
            "*.HiltWrapper_*",
            "*.Hilt_*",
            "dagger.hilt.internal.*",
            "hilt_aggregated_deps.*",
            "*_MembersInjector",
            // Moshi-generated only; QuoteFrameJsonAdapter is hand-written
            "*.data.model.*JsonAdapter",
            "*ComposableSingletons*",
            "*.BuildConfig",
            "*.ui.theme.*",
            "*.R",
            "*.R$*",
        )
        // Compose UI is covered by instrumented tests, not JVM unit tests.
        annotatedBy(
            "androidx.compose.ui.tooling.preview.Preview",
            "androidx.compose.runtime.Composable",
        )
    }
}

kover.reports.filters(koverExcludes)

// Only real modules; :feature and :feature:quotes are empty containers without a build file.
val realModules = subprojects.filter { it.buildFile.exists() }

configure(realModules) {
    apply(plugin = "com.autonomousapps.dependency-analysis")
    apply(plugin = "dev.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        // Report-only: findings must not fail the build.
        ignoreFailures.set(true)
    }
    dependencies.add("detektPlugins", composeRulesDetekt)

    extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
        reports.filters(koverExcludes)
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        reports {
            html.required.set(true)
            sarif.required.set(true)
            checkstyle.required.set(true)
        }
    }
}

dependencies {
    realModules.forEach { kover(it) }
}
