plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.espinosa.shinydex"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.espinosa.shinydex"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // SAST hardening: shrink + obfuscate release builds.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // SAST: fail the build on security-relevant lint issues.
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
    }
}

val DETEKT_JDK = 21

// SAST: detekt.
//
// The Gradle plugin runs detekt inside the daemon, and detekt 1.23.x cannot parse a Java 25
// version string -- which is exactly what this project's daemon runs on (see
// gradle/gradle-daemon-jvm.properties). Running the detekt CLI in a forked JDK 21 process
// sidesteps that completely, and works the same from Android Studio, the terminal and CI.
//
//   gradlew :app:detektSast
val detektCli: Configuration by configurations.creating

val detektReports = layout.buildDirectory.dir("reports/detekt")

tasks.register<JavaExec>("detektSast") {
    group = "verification"
    description = "Runs detekt (SAST) over the app and server sources in a forked JDK 21 process."

    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(DETEKT_JDK)) },
    )
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")

    val reports = detektReports.get().asFile
    outputs.dir(reports)
    inputs.dir(layout.projectDirectory.dir("src/main/java"))
    inputs.dir(rootProject.layout.projectDirectory.dir("server/src/main/kotlin"))
    inputs.file(rootProject.file("config/detekt/detekt.yml"))

    argumentProviders.add {
        listOf(
            // The app and the face-off server are analysed together, under one rule set.
            "--input", file("src/main/java").absolutePath + "," +
                rootProject.file("server/src/main/kotlin").absolutePath,
            "--config", rootProject.file("config/detekt/detekt.yml").absolutePath,
            "--build-upon-default-config",
            "--jvm-target", "17",
            "--report", "html:" + File(reports, "detekt.html").absolutePath,
            "--report", "xml:" + File(reports, "detekt.xml").absolutePath,
            "--report", "sarif:" + File(reports, "detekt.sarif").absolutePath,
        )
    }
    doFirst { reports.mkdirs() }
}

dependencies {
    detektCli(libs.detekt.cli)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// SCA (Software Composition Analysis) is opt-in so the day-to-day build stays fast:
//   gradlew -PenableSca=true -PnvdApiKey=<key> :app:dependencyCheckAnalyze
if (providers.gradleProperty("enableSca").getOrElse("false").toBoolean()) {
    apply(from = rootProject.file("security/sca.gradle.kts"))
}
