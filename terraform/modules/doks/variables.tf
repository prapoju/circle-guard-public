# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

variable "doks_cluster_name" {
  description = "DOKS cluster name"
  type        = string
}

variable "doks_region" {
  description = "DigitalOcean region slug (must support DOKS + Spaces, e.g. nyc3)"
  type        = string
}

variable "doks_node_size" {
  description = "Droplet size slug for the node pool"
  type        = string
}

variable "doks_node_count" {
  description = "Number of nodes in the node pool"
  type        = number
}
