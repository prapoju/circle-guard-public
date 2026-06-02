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

