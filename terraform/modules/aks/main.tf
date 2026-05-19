# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

resource "azurerm_resource_group" "aks_group" {
  name     = var.aks_resource_group_name
  location =  var.aks_location 

  tags = {
    environment = "cgp"
  }
}

resource "azurerm_kubernetes_cluster" "aks_cluster" {
  name                = var.aks_cluster_name
  location            = azurerm_resource_group.aks_group.location
  resource_group_name = azurerm_resource_group.aks_group.name
  dns_prefix          = var.aks_dns_prefix 
  kubernetes_version  = var.aks_kubernetes_version 

  default_node_pool {
    name            = "default"
    node_count      = var.aks_default_pool_node_count
    vm_size         = var.aks_default_pool_vm_size 
    os_disk_size_gb = var.aks_default_pool_os_disk_size_gb
  }

  service_principal {
    client_id     = var.aks_app_id
    client_secret = var.aks_password
  }

  role_based_access_control_enabled = true

  tags = {
    environment = "cgp"
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "aks_node_pool_master" {
  name                  = "master"
  kubernetes_cluster_id = azurerm_kubernetes_cluster.aks_cluster.id
  vm_size               = var.aks_master_pool_vm_size 
  node_count            = var.aks_master_pool_node_count
  tags = {
    Environment = "cgp-master"
  }
}

resource "azurerm_kubernetes_cluster_node_pool" "aks_node_pool_stage" {
  name                  = "stage"
  kubernetes_cluster_id = azurerm_kubernetes_cluster.aks_cluster.id
  vm_size               = var.aks_stage_pool_vm_size 
  node_count            = var.aks_stage_pool_node_count
  tags = {
    Environment = "cgp-stage"
  }
}
