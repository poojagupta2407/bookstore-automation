// ============================================================
// Jenkins pipeline - runs the test suite on your Jenkins server.
// Explained line by line in Jenkins-Guide.md, Lesson 9A.
//
// The GitHub Actions twin of this file is .github/workflows/ci.yml.
// ============================================================

pipeline {

    // Run on any available agent. You have one: your laptop.
    agent any

    // Use the JDK and Maven registered under these names in
    // Manage Jenkins > Tools.
    tools {
        jdk   'jdk-21'
        maven 'maven-3.9.9'
    }

    // Environment variables for every stage. ConfigReader reads
    // BOOKSTORE_HEADLESS before config.properties, so the browser runs
    // headless here without editing any committed file.
    environment {
        BOOKSTORE_HEADLESS = 'true'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')               // stop a hung build
        buildDiscarder(logRotator(numToKeepStr: '20'))   // keep the last 20 builds
        disableConcurrentBuilds()                        // one build at a time
    }

    // Ask GitHub for new commits roughly every 5 minutes.
    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {

        stage('Versions') {
            steps {
                bat 'java -version'
                bat 'mvn -v'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn -B -ntp clean test'
            }
        }
    }

    // Runs after the stages - pass or fail.
    post {
        always {
            junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'target/cucumber-reports/**', allowEmptyArchive: true
        }
        success {
            echo 'All scenarios passed.'
        }
        failure {
            echo 'Build failed - open Console Output, then the archived cucumber.html.'
        }
    }
}