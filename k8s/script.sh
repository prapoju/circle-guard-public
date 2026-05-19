# We create the cluster
kind create cluster --config kind/kind-config.yaml

# validate nodes creation
kind get nodes -n circle-guard-public




# JENKINS CONFIGURATION

# helm
helm repo add jenkinsci https://charts.jenkins.io
helm repo update
helm search repo jenkinsci

# KUBECTL
kubectl apply -f jenkins/jenkins-namespace.yaml

kubectl apply -f jenkins/jenkins-01-volume.yaml

# Validate volume. THe volue exist, but the node affinity is none because the resource is not required by now. 
kubectl get pv -n jenkins


kubectl apply -f jenkins/jenkins-02-sa.yaml



chart=jenkinsci/jenkins
helm install jenkins -n jenkins -f jenkins/jenkins-values.yaml $chart

# But there is an error because of the permises
kubectl get pods -n jenkins

# Show logs
kubectl logs jenkins-0 -n jenkins
kubectl logs jenkins-0 -n jenkins -c init

# Show the pod and node name
kubectl get pod jenkins-0 -n jenkins -o wide

# Change permises inside the node
docker exec circle-guard-public-worker3  chown -R 1000:1000 /data/jenkins-volume



# Restart the pod

kubectl delete pod jenkins-0 -n jenkins

# Get passwod
jsonpath="{.data.jenkins-admin-password}"
secret=$(kubectl get secret -n jenkins jenkins -o jsonpath=$jsonpath)
echo $(echo $secret | base64 --decode)

# Example password
# ovRgMEgS3G6tOKBee3dBA2


# wait until the status is running ~4 m
kubectl get pods -n jenkins

# get the password again if you can't sign in as admin
kubectl -n jenkins port-forward jenkins-0 8080:8080

