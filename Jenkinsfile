pipeline {
    agent any
    
    environment {
        REGISTRY = 'ghcr.io'
        BACKEND_IMAGE = "${REGISTRY}/arivasudevan-s-20542/chart-db-fullstack/backend"
        FRONTEND_IMAGE = "${REGISTRY}/arivasudevan-s-20542/chart-db-fullstack/frontend"
        IMAGE_TAG = "${env.BRANCH_NAME == 'main' ? 'latest' : 'feature-latest'}"
        JAVA_HOME = '/usr/lib/jvm/java-21-openjdk-amd64'
        MAVEN_HOME = '/usr/share/maven'
        PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 1, unit: 'HOURS')
        timestamps()
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    env.BUILD_TIME = sh(returnStdout: true, script: 'date -Iseconds').trim()
                }
            }
        }
        
        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh '''
                        mvn clean package -DskipTests -Pgithub
                    '''
                }
            }
        }
        
        stage('Test Backend') {
            steps {
                dir('backend') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh '''
                        npm ci
                        npm run build
                    '''
                }
            }
        }
        
        stage('Test Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm run test || true'
                    sh 'npm run lint || true'
                }
            }
        }
        
        stage('Build & Push Docker Images') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    branch pattern: 'feature/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    docker.withRegistry("https://${REGISTRY}", 'github-packages-token') {
                        // Build and push backend
                        def backendImage = docker.build(
                            "${BACKEND_IMAGE}:${IMAGE_TAG}",
                            "--build-arg BUILD_VERSION=${env.BUILD_NUMBER} " +
                            "--build-arg BUILD_COMMIT=${env.GIT_COMMIT_SHORT} " +
                            "./backend"
                        )
                        backendImage.push()
                        backendImage.push("${env.GIT_COMMIT_SHORT}")
                        
                        // Build and push frontend
                        def frontendImage = docker.build(
                            "${FRONTEND_IMAGE}:${IMAGE_TAG}",
                            "--build-arg VITE_API_URL=${env.VITE_API_URL ?: '/api'} " +
                            "--build-arg VITE_WS_URL=${env.VITE_WS_URL ?: '/ws'} " +
                            "--build-arg VITE_BUILD_VERSION=${env.BUILD_NUMBER} " +
                            "--build-arg VITE_BUILD_COMMIT=${env.GIT_COMMIT} " +
                            "--build-arg VITE_BUILD_TIME=${env.BUILD_TIME} " +
                            "./frontend"
                        )
                        frontendImage.push()
                        frontendImage.push("${env.GIT_COMMIT_SHORT}")
                    }
                }
            }
        }
        
        stage('Deploy to Server') {
            when {
                anyOf {
                    branch 'main'
                    branch pattern: 'feature/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'ghcr-token', variable: 'GHCR_TOKEN'),
                        string(credentialsId: 'postgres-password', variable: 'POSTGRES_PASSWORD'),
                        string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET'),
                        string(credentialsId: 'google-client-id', variable: 'GOOGLE_CLIENT_ID'),
                        string(credentialsId: 'google-client-secret', variable: 'GOOGLE_CLIENT_SECRET'),
                        string(credentialsId: 'github-oauth-client-id', variable: 'GITHUB_CLIENT_ID'),
                        string(credentialsId: 'github-oauth-client-secret', variable: 'GITHUB_CLIENT_SECRET'),
                        string(credentialsId: 'zoho-client-id', variable: 'ZOHO_CLIENT_ID'),
                        string(credentialsId: 'zoho-client-secret', variable: 'ZOHO_CLIENT_SECRET')
                    ]) {
                        sh '''
                            # Deploy directory
                            DEPLOY_DIR=~/chartdb
                            mkdir -p ${DEPLOY_DIR}
                            
                            # Copy docker-compose file
                            cp docker-compose.prod.yml ${DEPLOY_DIR}/
                            
                            # Create .env file
                            cat > ${DEPLOY_DIR}/.env << EOF
# Database Configuration
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
JWT_SECRET=${JWT_SECRET}
# OAuth2 Social Login
GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID}
GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET}
ZOHO_CLIENT_ID=${ZOHO_CLIENT_ID}
ZOHO_CLIENT_SECRET=${ZOHO_CLIENT_SECRET}
# Image Tag
IMAGE_TAG=${IMAGE_TAG}
EOF
                            
                            # Login to GitHub Container Registry
                            echo "${GHCR_TOKEN}" | docker login ghcr.io -u arivasudevan-s-20542 --password-stdin
                            
                            # Deploy with docker compose
                            cd ${DEPLOY_DIR}
                            docker compose -f docker-compose.prod.yml pull
                            docker compose -f docker-compose.prod.yml up -d
                            
                            # Clean up old images
                            docker image prune -f
                            
                            echo "Deployment completed successfully!"
                        '''
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
        always {
            cleanWs()
        }
    }
}
