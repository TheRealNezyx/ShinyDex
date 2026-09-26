// Análisis de composición de software (SCA) de ShinyDex.
//
// Queda fuera del build normal a propósito: OWASP Dependency-Check descarga la National
// Vulnerability Database la primera vez, lo que es lento y requiere una llave de la NVD.
// Se activa explícitamente:
//
//   gradlew -PenableSca=true -PnvdApiKey=TU_LLAVE :app:dependencyCheckAnalyze
//
// El reporte HTML/JSON queda en app/build/reports/dependency-check-report.*

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
    // Falla el build con severidad alta (CVSS >= 7.0) o peor.
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
