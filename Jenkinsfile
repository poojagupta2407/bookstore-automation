// ============================================================
// Jenkins pipeline - runs the test suite on your Jenkins server.
// Explained in Jenkins-Guide.md, Lessons 9A and 9B.
//
// The GitHub Actions twin of this file is .github/workflows/ci.yml.
// ============================================================

// Runs a command with the right shell for the agent: sh on Linux/macOS,
// bat on Windows. Lets the same Jenkinsfile run on any agent.
def runCmd(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

pipeline {

    // Run on any available agent. You have one: your laptop.
    agent any

    // Use the JDK and Maven registered under these names in
    // Manage Jenkins > Tools.
    tools {
        jdk   'jdk-21'
        maven 'maven-3.9.9'
    }

    // The "Build with Parameters" form. Automatic builds (polling, cron)
    // use the defaults, so the defaults are the normal full run.
    parameters {
        string(name: 'TAGS', defaultValue: '@ui',
               description: 'Cucumber tag expression, e.g. @ui (all), @books, @negative, @toBeRunNow, "@books and not @negative". @wip and @disabled are always excluded.')
        choice(name: 'BROWSER', choices: ['chrome', 'edge', 'firefox'],
               description: 'Browser to run in. firefox only if it is installed.')
        booleanParam(name: 'HEADLESS', defaultValue: true,
                     description: 'Untick to watch the browser.')
    }

    // Parameters become environment variables, which the framework reads:
    //   BOOKSTORE_*           -> ConfigReader (before config.properties)
    //   CUCUMBER_FILTER_TAGS  -> Cucumber (replaces @CucumberOptions tags)
    environment {
        BOOKSTORE_BROWSER    = "${params.BROWSER}"
        BOOKSTORE_HEADLESS   = "${params.HEADLESS}"
        CUCUMBER_FILTER_TAGS = "(${params.TAGS}) and not @wip and not @disabled"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')               // stop a hung build
        buildDiscarder(logRotator(numToKeepStr: '20'))   // keep the last 20 builds
        disableConcurrentBuilds()                        // one build at a time
    }

    triggers {
        pollSCM('H/5 * * * *')   // new commits?  check about every 5 minutes
        cron('H 21 * * *')       // full regression every evening, between 21:00 and 21:59
    }

    stages {

        stage('Info') {
            steps {
                     echo "Branch: ${env.BRANCH_NAME} | Build: #${env.BUILD_NUMBER} | Job: ${env.JOB_NAME}"
                     echo "Commit: ${env.GIT_COMMIT}"
            }
        }

        stage('Versions') {
            steps {
                runCmd('java -version')
                runCmd('mvn -v')
            }
        }

        stage('Test') {
            steps {
                echo "Tags: ${env.CUCUMBER_FILTER_TAGS} | Browser: ${params.BROWSER} | Headless: ${params.HEADLESS}"
                runCmd('mvn -B -ntp clean test')
            }
        }
    }

    // Runs after the stages - pass or fail.
    post {
        always {
            junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            cucumber fileIncludePattern: '**/cucumber.json',
                     jsonReportDirectory: 'target/cucumber-reports',
                     reportTitle: 'BookStore Automation',
                     trendsLimit: 20
            archiveArtifacts artifacts: 'target/cucumber-reports/**', allowEmptyArchive: true
        }
        success {
            echo 'All scenarios passed.'
        }
        failure {
            echo 'Build failed - open Console Output, then Cucumber reports on the build page.'
        }
    }
}