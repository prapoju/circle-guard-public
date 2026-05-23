# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

# AKS
variable "aks_location"{
  description = "AKW cluster location"
  type= string
}
variable "subscription_id" {
  description = "sub id"
  type = string
}


variable "aks_resource_group_name"{
  description = "aks resource group name"
  type=string
}

variable "aks_cluster_name" {
  description = "AKS cluster name"
  type = string  
}

variable "aks_dns_prefix"{
  description = "dns aks prefix"
  type = string
}


variable "aks_kubernetes_version"{
  description = "kubernetes version"
  type = string
}

variable "aks_default_pool_node_count" {
  description = "Number of nodes used for the system pool "
  type=number
}

variable "aks_default_pool_vm_size" {
  description = "System pool vm size"
  type=string
}


variable "aks_default_pool_os_disk_size_gb" {
  description = "AKS default disk size"
  type=number
}

variable "aks_master_pool_vm_size" {
  description = "Master nodes pool vm size"
  type=string
}


variable "aks_master_pool_node_count" {
  description = "Number of nodes used for master "
  type=number
}




variable "aks_stage_pool_vm_size" {
  description = "stage nodes pool vm size"
  type=string
}


variable "aks_stage_pool_node_count" {
  description = "Number of nodes used for stage "
  type=number
}



variable "aks_app_id" {
  description = "Azure Kubernetes Service Cluster service principal"
  type= string
}

variable "aks_password" {
  description = "Azure Kubernetes Service Cluster password"
  type= string
  sensitive = true
}





# PLATFORM

variable "platform_location" {
  type = string
}
variable "platform_rg_name" {
  type = string
}

variable "platform_vn_name" {
  type = string
}
variable "platform_subnet_name" {
  type = string
}


variable "platform_sa_diagnostics_name" {
  type = string
}

variable "platform_jenkins_vm" {
  type = object({
    name =string
    size =string
  })
}

variable "platform_jenkins_storage_image_reference" {
  type = object({
    publisher = string
    offer     = string
    sku       = string
    version   = string
  }) 
  default = {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts"
    version   = "latest"
  }
}

variable "platform_jenkins_admin" {
  type = string
}

variable "jenkins_custom_data_path" {
  type = string
}

variable "jenkins_public_key_path" {
  type = string
}





