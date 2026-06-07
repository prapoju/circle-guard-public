# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

provider "digitalocean" {
  token = var.do_token
}

module "doks" {
  source            = "../modules/doks"
  doks_cluster_name = var.doks_cluster_name
  doks_region       = var.doks_region
  doks_node_size    = var.doks_node_size
  doks_node_count   = var.doks_node_count
}
