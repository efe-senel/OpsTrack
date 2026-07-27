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

        stage('Prepare Compose Environment') {
            steps {
                withCredentials([file(
                    credentialsId: 'opstrack-env',
                    variable: 'OPSTRACK_ENV_FILE'
                )]) {
                    sh(
                        label: 'Install Compose environment file',
                        script: '''
                            set +x
                            umask 077
                            rm -f opstrack-devops/.env
                            cp -- "$OPSTRACK_ENV_FILE" opstrack-devops/.env
                            chmod 600 opstrack-devops/.env
                            test "$(stat -c '%a' opstrack-devops/.env)" = "600"
                        '''
                    )
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
        stage('Deploy') {
            steps {
                dir('opstrack-devops') {
                     sh 'docker compose up -d --remove-orphans'
                    sh 'docker compose ps'
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
            sh(
                label: 'Remove Compose environment file',
                script: '''
                    set +x
                    rm -f opstrack-devops/.env
                '''
            )
            echo "Build result: ${currentBuild.currentResult}"
        }
    }
}
