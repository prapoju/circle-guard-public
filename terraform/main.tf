provider "azurerm" {
  subscription_id = var.subscription_id
  features {}
}

module "aks-cluster" {
  source = "./modules/aks"
  aks_app_id= var.aks_app_id
  aks_password    = var.aks_password
  aks_location                = var.aks_location
  aks_resource_group_name     = var.aks_resource_group_name
  aks_cluster_name            = var.aks_cluster_name
  aks_dns_prefix              = var.aks_dns_prefix
  aks_kubernetes_version      = var.aks_kubernetes_version
  aks_default_pool_node_count = var.aks_default_pool_node_count
  aks_default_pool_vm_size    = var.aks_default_pool_vm_size
  aks_default_pool_os_disk_size_gb = var.aks_default_pool_os_disk_size_gb
  aks_stage_pool_node_count = var.aks_stage_pool_node_count
  aks_stage_pool_vm_size    = var.aks_stage_pool_vm_size
  aks_master_pool_node_count = var.aks_master_pool_node_count
  aks_master_pool_vm_size    = var.aks_master_pool_vm_size
}
