
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


-Credentias settings



az ad sp create-for-rbac --role="Contributor" --scopes="/subscriptions/<SUBSCRIPTION_ID>"

- Environment variables

PWS
$Env:ARM_CLIENT_ID = "<APPID_VALUE>"
$Env:ARM_CLIENT_SECRET = "<PASSWORD_VALUE>"
$Env:ARM_SUBSCRIPTION_ID = "<SUBSCRIPTION_ID>"
$Env:ARM_TENANT_ID = "<TENANT_VALUE>"

Linux
export ARM_CLIENT_ID="<APPID_VALUE>"
export ARM_CLIENT_SECRET="<PASSWORD_VALUE>"
export ARM_SUBSCRIPTION_ID="<SUBSCRIPTION_ID>"
export ARM_TENANT_ID="<TENANT_VALUE>"


- tfvars configuration based on terraform.tfvas.example

- Terraform plugins installation
terraform init

-  Terraform infrastructure 
terraform apply

- Kubectl configuration

az aks get-credentials --resource-group generous-troll-rg --name generous-troll-aks

Or

az aks get-credentials --resource-group $(terraform output -raw resource_group_name) --name $(terraform output -raw kubernetes_cluster_name)

In case of errors delete ~/.kube/config and try again


- Test
kubectl get nodes
NAME                              STATUS   ROLES    AGE   VERSION
aks-default-33271581-vmss000000   Ready    <none>   17m   v1.34.7
aks-default-33271581-vmss000001   Ready    <none>   17m   v1.34.7

# With cluster running

- Configure your ~/.kube/config file

# Stop cluster

az aks stop -n generous-troll-aks -g generous-troll-rg

# Delete cluster


