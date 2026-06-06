# Copyright (c) HashiCorp, Inc.
# SPDX-License-Identifier: MPL-2.0

# State lives in a DigitalOcean Space (S3-compatible), separate from the Azure
# Blob backend. Auth uses Spaces access keys exported as AWS_ACCESS_KEY_ID and
# AWS_SECRET_ACCESS_KEY before running terraform init.
terraform {
  backend "s3" {
    endpoints = {
      s3 = "https://nyc3.digitaloceanspaces.com"
    }
    bucket = "circleguard-tfstate-do"
    key    = "terraform/state/terraform.tfstate"
    region = "us-east-1"

    # DO Spaces is not AWS: skip AWS-specific validations and checksums.
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    skip_region_validation      = true
    skip_requesting_account_id  = true
    skip_s3_checksum            = true
  }

  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.0"
    }
  }

  required_version = ">= 1.6"
}
