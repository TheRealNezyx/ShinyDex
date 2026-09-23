import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ShinyDex face-off server: the small room service that lets several phones share a hunt.
//
//   gradlew :server:run      starts it on http://0.0.0.0:8080 (PORT overrides the port)
//   gradlew :server:test     runs the room-logic and HTTP tests
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("com.espinosa.shinydex.server.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.gson)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
}
