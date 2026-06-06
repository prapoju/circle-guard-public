# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

variable "do_token" {
  description = "DigitalOcean API token (export TF_VAR_do_token to avoid committing it)"
  type        = string
  sensitive   = true
}

variable "doks_cluster_name" {
  description = "DOKS cluster name"
  type        = string
  default     = "circleguard-doks"
}

variable "doks_region" {
  description = "DigitalOcean region slug (must support DOKS + Spaces)"
  type        = string
  default     = "nyc3"
}

variable "doks_node_size" {
  description = "Droplet size slug for the node pool"
  type        = string
  default     = "s-2vcpu-4gb"
}

variable "doks_node_count" {
  description = "Number of nodes in the node pool"
  type        = number
  default     = 2
}
