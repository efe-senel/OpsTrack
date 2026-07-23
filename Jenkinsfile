pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Backend Tests') {
            steps {
                dir('opstrack-devops') {
                    sh 'chmod +x mvnw'
                    sh './mvnw --batch-mode clean test'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('opstrack-devops/frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Validate Docker Compose') {
            steps {
                dir('opstrack-devops') {
                    sh 'docker compose config --quiet'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                dir('opstrack-devops') {
                    sh 'docker compose build app frontend'
                }
            }
        }
    }

    post {
        success {
            echo 'OpsTrack CI pipeline successfully completed.'
        }

        failure {
            echo 'OpsTrack CI pipeline failed. Check the failed stage logs.'
        }

        always {
            echo "Build result: ${currentBuild.currentResult}"
        }
    }
}
