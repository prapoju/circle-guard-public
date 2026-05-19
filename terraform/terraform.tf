# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

terraform {

  backend "azurerm" {
    use_cli= true
    use_azuread_auth = true
    storage_account_name = "cgpaccount"
    container_name = "tfstate"
    key = "prod.terraform.tfstate"

  }

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.53.0"
    }

    random = {
        source = "hashicorp/random"
        version = "~> 3.7.2"
    }
  }

  required_version = ">= 1.1"
}
