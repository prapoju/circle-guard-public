
# From scratch

- Backend config

az group create \
  --name cgp \
  --location eastus


az storage account create \
  --name cgpaccount \
  --resource-group cgp \
  --sku Standard_LRS

az storage container create \
  --name tfstate \
  --account-name cgpaccount


az role assignment create \
  --assignee <TU_USER_ID> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUB_ID>/resourceGroups/cgp/providers/Microsoft.Storage/storageAccounts/cgpaccount


-Credentias settings



az ad sp create-for-rbac --role="Contributor" --scopes="/subscriptions/<SUBSCRIPTION_ID>"


- tfvars configuration based on terraform.tfvas.example

- Terraform plugins installation
terraform init

-  Terraform infrastructure 
terraform apply

- Kubectl configuration

az aks get-credentials --resource-group cgp-rc --name cgp-cluster


In case of errors delete ~/.kube/config and try again


- Test
❯ kubectl get namespaces
NAME              STATUS   AGE
default           Active   11m
kube-node-lease   Active   11m
kube-public       Active   11m
kube-system       Active   11m
# With cluster running

- Configure your ~/.kube/config file

# Stop cluster

az aks stop -n generous-troll-aks -g generous-troll-rg

# Delete cluster


