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

## 2. Install Terraform Providers and Plugins

```bash
terraform init
```

## 3. Deploy Infrastructure

```bash
terraform apply
```

---

# Kubectl Configuration

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
> ```

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
