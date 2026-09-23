// ShinyDex CI pipeline.
//
// Stages: build -> unit tests -> SAST -> SCA -> DAST -> package.
// The SCA and DAST stages are opt-in through job parameters because they need external
// services (the NVD feed and a MobSF server respectively).
//
// Agent requirements:
//   - a JDK 21 installation Gradle can discover (the detektSast task forks onto it)
//   - ANDROID_HOME pointing at an SDK with platform 37 and build-tools installed
//   - the Android SDK licences accepted (sdkmanager --licenses)

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
            description: 'Run OWASP Dependency-Check (needs NVD_API_KEY credential).'
        )
        booleanParam(
            name: 'RUN_DAST',
            defaultValue: false,
            description: 'Run the MobSF dynamic scan (needs a reachable MobSF server).'
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
                // App logic plus the face-off server's room rules and HTTP API.
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
                // detekt over the app and the server, forked onto JDK 21 (see app/build.gradle.kts).
                gradlew(':app:detektSast')
                // Android Lint: platform-specific static analysis, emits SARIF for Jenkins.
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
            echo "ShinyDex pipeline finished with status: ${currentBuild.currentResult}"
        }
    }
}
