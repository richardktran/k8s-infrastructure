# How to setup VM/VPS with Jenkins, K8s

## Setup VPS/VM:

1. Create 2 or more VPS/VM with following minimum requirement:
    
    
    |  | Master | Worker1 |
    | --- | --- | --- |
    | Core | 4 | 2 |
    | Ram | 4GB | 4GB |
    | Storage | 20GB+10gb extend | 20GB + 10GB extend |
    | Operation | Ubuntu 20.04+ | Ubuntu 20.04+ |
    | Network IP | 172.16.171.135 (example) | 172.16.171.133 (example) |

## Setup master node:

1. Install some tools in Ubuntu:
    
    ```bash
    sudo apt update
    sudo apt install vim git curl -y
    ```
    
2. Clone setup files and setup K8s for **master node**:
    
    ```bash
    git clone https://github.com/richardktran/k8s-infrastructure.git
    cp ./k8s-infrastructure/nodes-setup/master.sh ./master.sh
    cp ./k8s-infrastructure/nodes-setup/common.sh ./common.sh
    cp ./k8s-infrastructure/nodes-setup/jenkins.sh ./jenkins-setup.sh
    sudo chmod +x common.sh
    sudo chmod +x master.sh
    sudo chmod +x jenkins-setup.sh
    ```
    
    Run the **common file** to setup Docker, CRI for Docker, Kubeadm, Kubelet and Kubectl:
    
    ```bash
    ./common.sh
    ```
    
    Run the setup-jenkins file to setup jenkins:
    
    ```bash
    ./jenkins-setup.sh
    ```
    
    After run this command, it will show the admin password of Jenkins, access to the **[MasterIP]:8080** to go to the Jenkins dashboard, enter the password to setup Jenkins.
    
    Run the master file to init the master cluster, setup network for k8s
    
    ```bash
    ./master.sh
    ```
    

## Setup worker node:

1. Install some tools in Ubuntu:
    
    ```bash
    sudo apt update
    sudo apt install vim git curl -y
    ```
    
2. Clone setup files and setup K8s for **master node**:
    
    ```bash
    git clone https://github.com/richardktran/k8s-infrastructure.git
    cp ./k8s-infrastructure/nodes-setup/node.sh ./node.sh
    cp ./k8s-infrastructure/nodes-setup/common.sh ./common.sh
    sudo chmod +x common.sh
    sudo chmod +x node.sh
    ```
    
    Run the **common file** to setup Docker, CRI for Docker, Kubeadm, Kubelet and Kubectl:
    
    ```bash
    ./common.sh
    ```
    
    Run the master file to init the master cluster, setup network for k8s
    
    ```bash
    ./node.sh
    ```
    
    Copy the join command of master node and run it.
    
    Example:
    
    ```bash
    sudo kubeadm join 172.16.171.135:6443 --token 0piwjz.7tromi63dv28sgfo --discovery-token-ca-cert-hash sha256:ccaf5a2641b14aaa472ef0c53aa8b179ba4e5f550b8c0574f4710268205db11c --cri-socket=unix:///var/run/cri-dockerd.sock
    ```
    
    To see the join command, go to master node and run the following command:
    
    ```bash
    kubeadm token create --print-join-command
    ```
    

## Setup Jenkins

### Create a job

1. Select create job, enter job name and select Pipeline project

![Untitled](.readme/Untitled.png)

1. Scroll down to the pipeline section, select the Pipeline script from SCM option. In the SCM, choose Git and enter the Repository URL ([git@github.com](mailto:git@github.com):richardktran/k8s-infrastructure.git)
    
    ![Untitled](.readme/Untitled%201.png)
    
2.  We will get the error, add the Credentials by add new credentials, choose **SSH Username with private key,** enter the username and private key.
    1. To get the private key, we have to generate the ssh key in Jenkins user of master node. 
        
        ```bash
        sudo su jenkins
        mkdir .ssh
        cd ~/.ssh
        ssh-keygen -t rsa -b 4096 -C "richardktran.dev@gmail.com"
        cat ~/.ssh/id_rsa.pub
        ```
        
    2. Put info of key to know_hosts
        
        ```bash
        ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
        ```
        
    3. Copy content of the id_rsa.pub file and paste to your github, copy content of the id_rsa file and paste to your credential of Jenkins.
        
        ![Untitled](.readme/Untitled%202.png)
        
    4. The result like this:
        
        ![Untitled](.readme/Untitled%203.png)
        
3. Change the branch to build is “main” branch. Put the path to the Jenkinsfile in Script Path and click Save.
    
    ![Untitled](.readme/Untitled%204.png)
    
4. Setup some credential declare on Jenkinsfile.
    - Get docker password from dockerhub
    - Github token get from id_rsa file.

![Untitled](.readme/Untitled%205.png)

**NOTE**: By default, when build image. Jenkins will use jenkins network with bridge driver, we have to change to host network in Jenkinsfile on the build image stage to be able to access internet.

![Untitled](.readme/Untitled%206.png)

## Setup HAProxy and Ingress-nginx

### Setup ingress-nginx

1. Check the current kubernetes version by run the following command:
    
    ```bash
    kubectl version
    ```

2. Go to https://github.com/kubernetes/ingress-nginx/ and find the version of ingress-nginx that match with the kubernetes version.
    ```bash
        helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
        helm search repo ingress-nginx --versions
    ```

3. Install ingress-nginx:
    ```bash
        CHART_VERSION="4.6.1"
        APP_VERSION="1.7.1"

        helm template ingress-nginx ingress-nginx \
        --repo https://kubernetes.github.io/ingress-nginx \
        --version ${CHART_VERSION} \
        --namespace ingress-nginx 
        > ./nginx-ingress.${APP_VERSION}.yaml
    ```

4. Deploy the Ingress controller
    ```bash
        kubectl create namespace ingress-nginx
        kubectl apply -f ./nginx-ingress.${APP_VERSION}.yaml
    ```

### Config HAProxy
1. Check the port of ingress-nginx:
    ```bash
        kubectl get svc -n ingress-nginx
    ```
    If it show like this:
    ```bash
        NAME                                 TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                      AGE
        ingress-nginx-controller             LoadBalancer   10.97.10.71     <pending>     80:31301/TCP,443:30929/TCP   47m
        ingress-nginx-controller-admission   ClusterIP      10.111.105.15   <none>        443/TCP                      47m
    ```
    It mean the port of ingress-nginx is 31301 for http and 30929 for https.

2. Open HAProxy config:
    ```bash
        sudo vim /etc/haproxy/haproxy.cfg
    ```

3. Add the following config to the end of that file, replace the port of ingress-nginx with the port you get from step 1, replace the ip of master and worker node with your ip.
    ```bash
        frontend http_front
            bind *:80
            default_backend http_back

        backend http_back
            balance roundrobin
            server master 172.16.171.135:31301 check
            server worker1 172.16.171.133:31301 check
    ```

4. Restart HAProxy:
    ```bash
        sudo systemctl restart haproxy
    ```

5. In the external machine, add the following config to the end of /etc/hosts file:
    ```bash
    172.16.171.135 gateway.richardktran.local
    ```
6. Config your ingress in the value.yaml, example:
    ```bash
    ingress:
        enabled: true
        className: "nginx"
        annotations:
            kubernetes.io/ingress.class: nginx
            nginx.ingress.kubernetes.io/proxy-body-size: "20m"
            nginx.ingress.kubernetes.io/limit-rps: "15"
            nginx.ingress.kubernetes.io/limit-rpm: "450"
            # kubernetes.io/ingress.class: nginx
            # kubernetes.io/tls-acme: "true"
        hosts:
            - host: gateway.richardktran.local
            paths:
                - path: /
                pathType: ImplementationSpecific
        tls: []
    ```

7. Rebuild your helm chart and deploy it.
8. Access to the gateway.richardktran.local to see the result.

## Note
In case kubelet is not running, run the following command:
```bash
Please perform below steps on the master node. It works like charm.

1. sudo -i

2. swapoff -a

3. exit

4. strace -eopenat kubectl version

```