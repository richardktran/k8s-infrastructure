def getTicketId(branch) {
    def rbBranch = branch =~ /rb-(\d+)/
    if (rbBranch) {
        return rbBranch[0][0]
    } else {
        return null
    }
}

def getServiceId(serviceName, ticketId) {
    if (ticketId != 'null') {
        return "${ticketId}-${serviceName}"
    } else {
        return serviceName
    }
}
pipeline {
  agent any 
  environment {
    KUBECONFIG="$HOME/.kube/config"
    DOMAIN_NAME='richardktran.dev'
    ENVIRONMENT='development'
    DOCKER_USERNAME='richardktran'
    PROJECT_NAME='richardktran-blog'
    APP_IMAGE = "${DOCKER_USERNAME}/${SERVICE_NAME}"
    DOCKER_TAG = "${ENVIRONMENT}-${BUILD_NUMBER}"
    FULL_IMAGE = "${APP_IMAGE}:${DOCKER_TAG}"
    TICKET_ID = getTicketId(gitBranch) // New variable to store rb-*
    SERVICE_NAME = 'api-gateway'

    SERVICE_ID = getServiceId(SERVICE_NAME, TICKET_ID)
  }

  parameters {
    string (name: "gitBranch", defaultValue: "develop", description: "Branch to build")
    string (name: "git_sha", defaultValue: "HEAD", description: "sha to build")
  }

  stages {
    stage('Detect RB Branch') {
      steps {
          script {
              if (TICKET_ID != 'null') {
                  echo "Detected RB Branch: ${TICKET_ID}"
              } else {
                  echo "RB Branch not found. Using default branch."
              }
              echo "SERVICE_ID: ${SERVICE_ID}"
          }
      }
    }
    // stage('Checkout') {
    //   steps {
    //     dir('var/www/') {
    //       checkout ( [$class: 'GitSCM',
    //         extensions: [[$class: 'CloneOption', timeout: 30]],
    //         branches: [[name: "${gitBranch}" ]],
    //         userRemoteConfigs: [[
    //           credentialsId: "github-token",
    //           url: "git@github.com:RichardKTranBlog/api-gateway.git"]
    //         ]]
    //       )
    //       echo 'Git Checkout Completed'
    //     }
    //   }
    // }

    // stage('Setup params') {
    //   steps {
    //     dir('var/www/') {
    //       sh "cp .env.example .env"
    //     }
    //   }
    // }

    // stage('Build image') {
    //   steps {
    //     dir('var/www/') {
    //       sh """
    //         docker build -t ${FULL_IMAGE} . --network=host
    //         docker tag ${FULL_IMAGE} ${FULL_IMAGE}
    //       """
    //       echo 'Build image completed'
    //     }
    //   }
    // }

    // stage('Push image to registry') {
    //   steps {
    //     dir('var/www/') {
    //       withCredentials([string(credentialsId: 'docker-pwd', variable: 'DOCKER_PASSWORD')])  {
    //         sh('echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin')
    //       }
    //       sh """
    //         docker push ${FULL_IMAGE}
    //       """
    //       echo 'Push image to registry completed'
    //     }
    //   }
    // }

    // stage('Deploy') {
    //   steps {
    //     dir("${PROJECT_NAME}/deployment/environment-dev/${SERVICE_NAME}") {
    //       sh """
    //         sed -i "s#__image__#$APP_IMAGE#g" values.yaml
    //         sed -i "s#__docker-tag__#$DOCKER_TAG#g" values.yaml
    //         helm upgrade ${SERVICE_NAME} --install \${WORKSPACE}/${PROJECT_NAME}/charts/backend -n ${ENVIRONMENT} -f values.yaml
    //       """
    //       echo 'Deploy to k8s completed'
    //     }
    //   }
    // }
  } // End stages
  post {
      always {
        // Clean up docker images
        sh """
          docker rmi ${FULL_IMAGE}
        """
        echo 'Clean up docker images completed'
      }
    }
} 