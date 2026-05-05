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

# Jenkins agent stage role binding

kubectl apply -f stage/stage-namespace.yaml

kubectl apply -f stage/stage-role.yaml

kubectl apply -f stage/stage-sa.yaml

kubectl apply -f stage/stage-agent-binding.yaml

# The token is created automatically and is mounted here. The token
# is injected automatically
# /var/run/secrets/kubernetes.io/serviceaccount/token

#DEBUGGING 
# Create a temporal agent
kubectl apply -f jenkins/agent-pod.yaml

kubectl exec -it agent-pod -n stage -c kubectl -- sh



# DEBUGGING
# 
# TO DEBUG YOU CAN CREATE A TEMPORTA POD THAT WORKS AS AN JENKINS agent
# TOKEN kubectl create token default
kubectl apply -f jenkins/agent-pod.yaml
kubectl exec -it agent-pod -n jenkins -c kubectl -- sh
#ovRgMEgS3G6tOKBee3dBA2
kubectl get secret jenkins-token -o jsonpath='{.data.token}' | base64 --decode
kubectl create clusterrolebinding jenkins-admin \
  --clusterrole=cluster-admin \
  --serviceaccount=default:default

kubectl --server=https://kubernetes.default.svc \
--token=eyJhbGciOiJSUzI1NiIsImtpZCI6IkFJcl9LQzV4NHRWeFlSYVhhS0FlcER5akhyNko4dkdPaDdTam14WUZCMkEifQ.eyJhdWQiOlsiaHR0cHM6Ly9rdWJlcm5ldGVzLmRlZmF1bHQuc3ZjLmNsdXN0ZXIubG9jYWwiXSwiZXhwIjoxNzc3OTA2MTc0LCJpYXQiOjE3Nzc5MDI1NzQsImlzcyI6Imh0dHBzOi8va3ViZXJuZXRlcy5kZWZhdWx0LnN2Yy5jbHVzdGVyLmxvY2FsIiwianRpIjoiNmYyNDhhNmYtOGJjNS00MGFlLTg1ZmQtMWRjNTM4ZDY2YjQ2Iiwia3ViZXJuZXRlcy5pbyI6eyJuYW1lc3BhY2UiOiJkZWZhdWx0Iiwic2VydmljZWFjY291bnQiOnsibmFtZSI6ImRlZmF1bHQiLCJ1aWQiOiI5ZjlhZThjZC1iNDk4LTQ1OWQtYWRlNC1hZTgyOGJmODVkMjEifX0sIm5iZiI6MTc3NzkwMjU3NCwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50OmRlZmF1bHQ6ZGVmYXVsdCJ9.EpLHGrPNv5fMdbshX0V1d-6dcKVeaaQKB4KJmdHnXxVfry-FqOXDazqfeDb_GNsO0azgX4MXcnrAjSraUJmMAuFnKgRf7fxFxbXLRixMUpPlDkf-iZ5dbzLsIV1N8jjyoPFq8EAY54Qyy3R7HzGfVvY_UjKOaChDT0EpFlUXY4YlsnHDiAoeeg3FDOUTxgK53jwxY1Q7c2MCLK65isdofqbgBYJLpmPwvy9pbbUpNCJUgy0iL3Qd7AnMiODps4lLGdXd3RF-QCp7xYmzBf39WGm1J-atUiwdy331GljWBeWWB0p8sz5EkHezCYeU_uYtd3Sl_xcPtenqHI1nFzwt2A \
--insecure-skip-tls-verify \
get nodes




# sonarqube

kubectl get nodes
kubectl get pods -n jenkins -o wide


# Choose a node name preferably not used by the jenkins pod. In my case it was ci-cd-demo-worker2
#
# we stop kubernetes from scheduling pods in the node
kubectl taint nodes ci-cd-demo-worker2 sonarqube=true:NoSchedule --overwrite

kubectl label node ci-cd-demo-worker2 sonarqube=true
# Rules in already applied in the values.yaml
# - sonarqube ignores the tail rule
# - sonarqube uses nodes labeled as sonarqube
# - monitoring passcode
# - community edition enabled

helm repo add sonarqube https://SonarSource.github.io/helm-chart-sonarqube

helm repo update

kubectl apply -f sonarqube/sonarqube-namespace.yaml

helm upgrade --install \
  -n sonarqube \
  -f sonarqube/values.yaml \
  sonarqube sonarqube/sonarqube

# Wait for 5 minutes
kubectl get pods -n sonarqube -o wide

# Get your token. By ingressing to the page. It is possible to use the ingress controller
#

kubectl port-forward svc/sonarqube-sonarqube 9000:9000 -n sonarqube

# Generate your token
# Example: sqa_94a2eea9c7d8524f352be0aa97c2f6633b466c0e
# Manage jenkins, system SONARQUBE SERVERS,
# Server URL http://sonarqube-sonarqube.sonarqube:9000
# Add secret  text paste secret scope global
#
#
# Configure the webhook
# Install sonarqube quality gate plugin
# 1) Create the web hook for the project cicd-demo. its key is my-app. If the project
# doesn't exist run the pipeline and check again.
# Read https://v1-32.docs.kubernetes.io/docs/concepts/services-networking/dns-pod-service/
# https://www.jenkins.io/doc/pipeline/steps/sonar/
# Project settings, webhooks create
# Name: jenkins-agent
# url: http://jenkins.jenkins.svc.cluster.local:8080/sonarqube-webhook/
# kubectl exec -it sonarqube-sonarqube-0 -n sonarqube -- sh
# Now add the quality gate configuration
# Manage jenkins
# Quality gate sonar qube
# Sonaqube url http://sonarqube-sonarqube.sonarqube:9000
# Token: The token that we used previously
# 2) Add quality gate condition. Add condition security hospots reviewed is less than 100
# 3) Go to the project quality gate always use a specific quality gate and add it.

# Create namespace

# Apply the service yaml
kubectl apply -f manifests/app/service.yml

# Apply the deployment
kubectl apply -f manifests/app/deployment.yml

# Go to plugins and install kubernetes CLI
# Add docker credentials Modify
# add .kube/config file
# make sure the server is 
# server: https://kubernetes.default.svc
# You can check the application with this command
# kubectl port-forward svc/my-app-service 8081:8080 -n app



