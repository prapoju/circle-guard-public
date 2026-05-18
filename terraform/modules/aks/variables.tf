# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0


variable "aks_location"{
  description = "AKW cluster location"
  type= string
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


variable "aks_default_os_disk_size_gb" {
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


