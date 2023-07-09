pipeline {
  agent any

  // triggers {
  //   cron('TZ=Asia/Saigon \n 0 * * * *') // 4AM in VietNam time
  // }

  environment {
    KUBECONFIG="$HOME/.kube/config"
  }
  stages {
    stage('Clean up containers and images') {
      steps {
        sh '''
        docker container prune -f | exit 0
        docker image prune -a -f
        '''
      }
    }
  }
}
