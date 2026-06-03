# Blob Storage Backend Configuration

## 1. Create Resource Group

```bash
az group create \
  --name cgp \
  --location eastus
```

## 2. Create Storage Account

```bash
az storage account create \
  --name cgpaccount \
  --resource-group cgp \
  --sku Standard_LRS
```

## 3. Create Blob Container

```bash
az storage container create \
  --name tfstate \
  --account-name cgpaccount
```

## 4. Assign Blob Storage Permissions

```bash
az role assignment create \
  --assignee <USER_ID> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUB_ID>/resourceGroups/cgp/providers/Microsoft.Storage/storageAccounts/cgpaccount
```

---

# Collaborators

Assign the same Blob Storage permissions to collaborators:

```bash
az role assignment create \
  --assignee <USER_ID> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUB_ID>/resourceGroups/cgp/providers/Microsoft.Storage/storageAccounts/cgpaccount
```

Also share the application credentials with collaborators.

---

# Application Credentials

Create a Service Principal for Terraform authentication:

```bash
az ad sp create-for-rbac \
  --role="Contributor" \
  --scopes="/subscriptions/<SUBSCRIPTION_ID>"
```

---

# Terraform Setup

## 1. Configure Variables

Create your `.tfvars` file based on:

```text
terraform.tfvars.example
```
Generate your ssh keys for jenkins
ssh-keygen -t ed25519


## 2. Install Terraform Providers and Plugins

```bash
terraform init
```

## 3. Deploy Infrastructure

```bash
terraform apply
```


--- 
# Jenkins  VM setup

## Sonarqube
https://docs.sonarsource.com/sonarqube-server/9.9/requirements/prerequisites-and-overview

Validate that this
sysctl vm.max_map_count
sysctl fs.file-max
ulimit -n
ulimit -u

vm.max_map_count is greater than or equal to 524288
fs.file-max is greater than or equal to 131072
the user running SonarQube can open at least 131072 file descriptors
the user running SonarQube can open at least 8192 threads

Sonarqube docker file

scp -r ./sonarqube <USERNAME>@<IP>:~/

sudo usermod -aG docker $USER
Exit And ssh againg

cd sonarqube
docker compose up -d


## Jenkins
Install suggested plugins
Create your first user
Install the Copy artifacts plugin




### Kubectl configuration
Connect `kubectl` to the AKS cluster:

```bash
az aks get-credentials \
  --resource-group cgp-rc \
  --name cgp-cluster
```

> If you encounter configuration conflicts, delete the kubeconfig file and try again:
>
> ```bash
> rm -f ~/.kube/config
> ``

Go to /terraform/k8s
And run script.sh

To generate your token

Install this plugin 
KubernetesVersion
4423.vb_59f230b_ce53
Cloud Providers
Cluster Management
kubernetes
Agent Management
This plugin integrates Jenkins with Kubernetes


Credentials
Add credentials
secret text
Scop global: kubernetes token


Manage jenkins Clouds
New cloud 
Name:aks
type: kubernetes


Now you have to fill these four

Kubernetes URL
They are obtained from 

kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'




Kubernetes server certificate key
They are obtained from


kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d


Credentials

The token that was created before.



JenkinsUrl
http://<PUBLICIP>:8080/

Web socket: Choose it


###Docker hub credentials
Go to docker hub, Personal access token
Generate new token

Description: cgp
Expiration: 90 days
Read and write access

Now go to jenkins
Credentials
Username with password

Username: Docker hub account
password: token
id: dockerhub-credentials

###Sonarqube

Instal these plugins
Sonar Quality GatesVersion
364.v67a_f255f340f
SonarQube ScannerVersion
2.18.2

Enable local webhooks validation
Forcing local webhooks validation prevents the creation and triggering of local webhooks
Disabling this setting can expose the instance to security risks
Disable this n administration settings

Create the cgp project

Create a webhook to <PUBLIC-IP>:8080

Account token generate token for cgp project
90 days

1) Go to 
Jenkins System add sonarqube server

When addind the web hook make sure you write
http://<PUBLIC-IP>:8080/sonarqube-webhook/


---

# Kubectl Configuration

---

# Important Commands

## Stop Cluster

```bash
az aks stop \
  -n cgp-cluster \
  -g cgp-rc
```

## Start Cluster

```bash
az aks start \
  -n cgp-cluster \
  -g cgp-rc
```

## Destroy Infrastructure

```bash
terraform destroy
```

## Destroy cluster and activate it again
terraform destroy -target="module.aks-cluster"



terraform plan -target=module.aks-cluster -out aks
terraform apply "aks"
rm -f ~/.kube/config

az aks get-credentials \
  --resource-group cgp-rc \
  --name cgp-cluster

cd k8s 
./script.sh

Update kubernetes token In jenkins

Url
kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'

Certificate Key
kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d
