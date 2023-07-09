pipeline {
  agent any 
  environment {
    KUBECONFIG="$HOME/.kube/config"
    DOMAIN_NAME='richardktran.dev'
    ENVIRONMENT='development'
    DOCKER_USERNAME='richardktran'
    PROJECT_NAME='richardktran-blog'
    SERVICE_NAME='customer-web'
    APP_IMAGE = "${DOCKER_USERNAME}/${SERVICE_NAME}:${ENVIRONMENT}-${BUILD_NUMBER}"
  }

  parameters {
    string (name: "gitBranch", defaultValue: "develop", description: "Branch to build")
    string (name: "git_sha", defaultValue: "HEAD", description: "sha to build")
  }

  stages {
    stage('Checkout') {
      steps {
        dir('var/www/') {
          checkout ( [$class: 'GitSCM',
            extensions: [[$class: 'CloneOption', timeout: 30]],
            branches: [[name: "${gitBranch}" ]],
            userRemoteConfigs: [[
              credentialsId: "github-token",
              url: "git@github.com:RichardKTranBlog/customer-web.git"]
            ]]
          )
          echo 'Git Checkout Completed'
        }
      }
    }

    stage('Build image') {
      steps {
        dir('var/www/') {
          sh """
            docker build -t ${APP_IMAGE} . --network=host
            docker tag ${APP_IMAGE} ${APP_IMAGE}
          """
          echo 'Build image completed'
        }
      }
    }

    stage('Push image to registry') {
      steps {
        dir('var/www/') {
          withCredentials([string(credentialsId: 'docker-pwd', variable: 'DOCKER_PASSWORD')])  {
            sh('echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin')
          }
          sh """
            docker push ${APP_IMAGE}
          """
          echo 'Push image to registry completed'
        }
      }
    }

    stage('Deploy') {
      steps {
        dir("${PROJECT_NAME}/deployment/environment-dev/${SERVICE_NAME}") {
          sh """
            sed -i "s#__image__#$APP_IMAGE#g" deployment.yaml
            kubectl apply -f deployment.yaml -n $ENVIRONMENT
            kubectl apply -f service.yaml -n $ENVIRONMENT
            kubectl apply -f ingress.yaml
          """
          echo 'Deploy to k8s completed'
        }
      }
    }
  } // End stages
  post {
      always {
        // Clean up docker images
        sh """
          docker rmi ${APP_IMAGE}
        """
        echo 'Clean up docker images completed'
      }
    }
} 