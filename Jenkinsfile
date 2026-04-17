pipeline {
    agent any

    tools {
        jdk 'jdk17'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')
        NEXUS_USERNAME = credentials('nexus-username')
        NEXUS_PASSWORD = credentials('nexus-password')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir('ldms-backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Test') {
            steps {
                dir('ldms-backend') {
                    sh 'mvn test'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('ldms-backend') {
                    sh 'mvn sonar:sonar -Dsonar.token=$SONAR_TOKEN -Dsonar.host.url=http://localhost:9000'
                }
            }
        }

        stage('Publish to Nexus') {
            steps {
                dir('ldms-backend') {
                    sh 'mvn deploy -Dnexus.username=$NEXUS_USERNAME -Dnexus.password=$NEXUS_PASSWORD'
                }
            }
        }
    }
}
