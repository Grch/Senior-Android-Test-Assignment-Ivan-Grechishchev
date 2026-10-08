plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.grch.tradernettestassignment.feature.quotes.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        // Only for android.util.Log calls; new Android API use must not rely on this flag.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":feature:quotes:domain"))

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
    // android.jar stubs org.json; use the real implementation on the JVM unit-test classpath.
    testImplementation(libs.org.json)
}
