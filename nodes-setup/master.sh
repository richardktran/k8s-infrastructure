sudo apt-get install -y haproxy

# Turn off swap
sudo swapoff -a
# Setup kubeadm and start cluster
sudo kubeadm init --pod-network-cidr=192.168.0.0/16 --cri-socket=unix:///var/run/cri-dockerd.sock

sudo rm -rf $HOME/.kube
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# Setup Calico Network plugin
curl https://raw.githubusercontent.com/projectcalico/calico/v3.25.1/manifests/calico.yaml -O
kubectl apply -f calico.yaml

# Generete kubeadm join command
kubeadm token create --print-join-command

# Copy config to jenkins user
sudo rm -rf /var/lib/jenkins/.kube
sudo mkdir /var/lib/jenkins/.kube
sudo cp ~/.kube/config /var/lib/jenkins/.kube
sudo chown -R jenkins:jenkins /var/lib/jenkins/.kube


# To reset kubeadm, run this command: sudo kubeadm reset --cri-socket=unix:///var/run/cri-dockerd.sock
