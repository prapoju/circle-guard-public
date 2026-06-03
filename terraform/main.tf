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

module "platform" {
  source = "./modules/platform"
  platform_location = var.platform_location
  platform_rg_name = var.platform_rg_name
  platform_vn_name = var.platform_vn_name
  platform_subnet_name = var.platform_subnet_name
  platform_sa_diagnostics_name = var.platform_sa_diagnostics_name
  platform_jenkins_vm = var.platform_jenkins_vm
  platform_jenkins_storage_image_reference = var.platform_jenkins_storage_image_reference
  platform_jenkins_admin = var.platform_jenkins_admin
  jenkins_custom_data_path = var.jenkins_custom_data_path
  jenkins_public_key_path = var.jenkins_public_key_path
}
