# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

data "digitalocean_kubernetes_versions" "current" {}

resource "digitalocean_kubernetes_cluster" "doks_cluster" {
  name    = var.doks_cluster_name
  region  = var.doks_region
  version = data.digitalocean_kubernetes_versions.current.latest_version

  # Non-HA control plane is free; only the worker nodes are billed.
  ha = false

  node_pool {
    name       = "stage"
    size       = var.doks_node_size
    node_count = var.doks_node_count

    labels = {
      environment = "cgp-stage"
    }
  }

  tags = ["cgp"]
}
