import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Servidor de Face-off de ShinyDex: el servicio de salas que permite a varios celulares
// compartir una caza.
//
//   gradlew :server:test     corre las pruebas de las reglas de las salas y de la API HTTP
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
