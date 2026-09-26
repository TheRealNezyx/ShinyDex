// Pipeline de CI de ShinyDex.
//
// Etapas: build -> pruebas unitarias -> SAST -> SCA -> DAST -> package.
// SCA y DAST se activan con parámetros del job porque necesitan servicios externos
// (la base de datos NVD y un servidor MobSF).
//
// Requisitos del agente:
//   - un JDK 21 que Gradle pueda encontrar (la tarea detektSast corre sobre él)
//   - ANDROID_HOME apuntando a un SDK con la plataforma 37 y build-tools instalados
//   - las licencias del SDK de Android aceptadas (sdkmanager --licenses)

def sh_(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

def gradlew(String args) {
    sh_(isUnix() ? "./gradlew --no-daemon ${args}" : "gradlew.bat --no-daemon ${args}")
}

pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    parameters {
        booleanParam(
            name: 'RUN_SCA',
            defaultValue: false,
            description: 'Corre OWASP Dependency-Check (necesita la credencial NVD_API_KEY).'
        )
        booleanParam(
            name: 'RUN_DAST',
            defaultValue: false,
            description: 'Corre el escaneo dinámico de MobSF (necesita un servidor MobSF).'
        )
    }

    environment {
        GRADLE_OPTS = '-Dorg.gradle.jvmargs=-Xmx2g'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                gradlew(':app:assembleDebug :server:installDist')
            }
        }

        stage('Unit tests') {
            steps {
                // Lógica de la app, reglas de las salas de Face-off y su API HTTP.
                gradlew(':app:testDebugUnitTest :server:test')
            }
            post {
                always {
                    junit allowEmptyResults: true,
                        testResults: 'app/build/test-results/testDebugUnitTest/*.xml, ' +
                            'server/build/test-results/test/*.xml'
                }
            }
        }

        stage('SAST') {
            steps {
                // detekt sobre la app y el servidor, en un proceso con JDK 21 (ver app/build.gradle.kts).
                gradlew(':app:detektSast')
                // Android Lint: análisis estático propio de Android; genera SARIF para Jenkins.
                gradlew(':app:lintDebug')
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true,
                        artifacts: 'app/build/reports/detekt/**, app/build/reports/lint-results-*.*'
                    recordIssues(
                        enabledForFailure: true,
                        tools: [
                            sarif(pattern: 'app/build/reports/lint-results-debug.sarif'),
                            detekt(pattern: 'app/build/reports/detekt/detekt.xml')
                        ]
                    )
                }
            }
        }

        stage('SCA') {
            when { expression { params.RUN_SCA } }
            steps {
                withCredentials([string(credentialsId: 'NVD_API_KEY', variable: 'NVD_KEY')]) {
                    gradlew("-PenableSca=true -PnvdApiKey=${NVD_KEY} :app:dependencyCheckAnalyze")
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true,
                        artifacts: 'app/build/reports/dependency-check-report.*'
                }
            }
        }

        stage('DAST') {
            when { expression { params.RUN_DAST } }
            steps {
                gradlew(':app:assembleDebug')
                withCredentials([string(credentialsId: 'MOBSF_API_KEY', variable: 'MOBSF_API_KEY')]) {
                    sh_(
                        isUnix()
                            ? 'bash security/mobsf-scan.sh app/build/outputs/apk/debug/app-debug.apk'
                            : 'powershell -File security/mobsf-scan.ps1 ' +
                              '-Apk app/build/outputs/apk/debug/app-debug.apk'
                    )
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'security/reports/**'
                }
            }
        }

        stage('Package') {
            steps {
                gradlew(':app:assembleRelease :server:installDist')
            }
            post {
                success {
                    archiveArtifacts artifacts: 'app/build/outputs/apk/**/*.apk, server/build/install/**',
                        fingerprint: true
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline de ShinyDex terminado con estado: ${currentBuild.currentResult}"
        }
    }
}
