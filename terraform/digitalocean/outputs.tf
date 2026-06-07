# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

output "cluster_name" {
  value = module.doks.cluster_name
}

output "cluster_version" {
  value = module.doks.cluster_version
}

output "cluster_endpoint" {
  value     = module.doks.cluster_endpoint
  sensitive = true
}

output "get_credentials_command" {
  value = "doctl kubernetes cluster kubeconfig save ${module.doks.cluster_name}"
}
