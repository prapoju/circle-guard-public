# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

output "cluster_name" {
  value = digitalocean_kubernetes_cluster.doks_cluster.name
}

output "cluster_endpoint" {
  value     = digitalocean_kubernetes_cluster.doks_cluster.endpoint
  sensitive = true
}

output "cluster_version" {
  value = digitalocean_kubernetes_cluster.doks_cluster.version
}

output "node_pool_name" {
  value = digitalocean_kubernetes_cluster.doks_cluster.node_pool[0].name
}
