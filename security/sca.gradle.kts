// Software Composition Analysis (SCA) for ShinyDex.
//
// Kept out of the normal build on purpose: OWASP Dependency-Check downloads the National
// Vulnerability Database on first run, which is slow and needs an NVD API key. Enable it
// explicitly when you want it:
//
//   gradlew -PenableSca=true -PnvdApiKey=YOUR_KEY :app:dependencyCheckAnalyze
//
// The HTML/JSON report lands in app/build/reports/dependency-check-report.*

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.owasp:dependency-check-gradle:10.0.4")
    }
}

apply(plugin = "org.owasp.dependencycheck")

extensions.configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension>(
    "dependencyCheck",
) {
    // Fail the build on High severity (CVSS >= 7.0) or worse.
    failBuildOnCVSS = 7.0f
    formats = listOf("HTML", "JSON")
    scanConfigurations = listOf("releaseRuntimeClasspath")

    val key = providers.gradleProperty("nvdApiKey").orNull
    if (!key.isNullOrBlank()) {
        nvd.apiKey = key
    }

    analyzers.assemblyEnabled = false
    analyzers.nodeEnabled = false
    analyzers.nuspecEnabled = false
}
