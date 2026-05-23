resource "azurerm_resource_group" "rg" {
  name     = var.platform_rg_name 
  location = var.platform_location
}

resource "azurerm_virtual_network" "main" {
  name                = var.platform_vn_name
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_subnet" "internal" {
  name                 = var.platform_subnet_name 
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.0.1.0/24"]
}
resource "azurerm_storage_account" "vm_diagnostics_sa" {
  name                     = var.platform_sa_diagnostics_name 
  location                 = azurerm_resource_group.rg.location
  resource_group_name      = azurerm_resource_group.rg.name
  account_tier             = "Standard"
  account_replication_type = "LRS"
}




# JENKINS VM

# Create public IPs
resource "azurerm_public_ip" "jenkins_public_ip" {
  name                = "${var.platform_jenkins_vm.name}_public_ip"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  allocation_method   = "Static"
}


# Create Network Security Group 
resource "azurerm_network_security_group" "jenkins_nsg" {
  name                = "${var.platform_jenkins_vm.name}_nsg"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

}

resource "azurerm_network_security_rule" "jenkins_ssh_rule" {
    name                       = "SSH"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
    resource_group_name = azurerm_resource_group.rg.name
    network_security_group_name = azurerm_network_security_group.jenkins_nsg.name
}

resource "azurerm_network_security_rule" "jenkins_port" {
    name                       = "jenkins_port"
    priority                   = 1002
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8080"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
    resource_group_name = azurerm_resource_group.rg.name
    network_security_group_name = azurerm_network_security_group.jenkins_nsg.name
}




# Create network interface

resource "azurerm_network_interface" "jenkins_nic" {
  name                = "${var.platform_jenkins_vm.name}_nic" 
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  ip_configuration {
    name                          = "my_nic_configuration"
    subnet_id                     = azurerm_subnet.internal.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.jenkins_public_ip.id
  }
}


# Connect the security group to the network interface
resource "azurerm_network_interface_security_group_association" "jenkins_nic_asso" {
  network_interface_id      = azurerm_network_interface.jenkins_nic.id
  network_security_group_id = azurerm_network_security_group.jenkins_nsg.id
}






resource "azurerm_linux_virtual_machine" "jenkins_vm" {
  name                  = var.platform_vn_name
  location              = azurerm_resource_group.rg.location
  resource_group_name   = azurerm_resource_group.rg.name
  network_interface_ids = [azurerm_network_interface.jenkins_nic.id]
  size                  = var.platform_jenkins_vm.size
  admin_username = var.platform_jenkins_admin

  os_disk {
    name                 = "myOsDisk"
    caching              = "ReadWrite"
    storage_account_type = "Premium_LRS"
  }

  source_image_reference {
    publisher = var.platform_jenkins_storage_image_reference.publisher
    offer     = var.platform_jenkins_storage_image_reference.offer
    sku       = var.platform_jenkins_storage_image_reference.sku
    version   = var.platform_jenkins_storage_image_reference.version
  }

  computer_name  = "hostname"

  admin_ssh_key {
    username   = var.platform_jenkins_admin
    public_key = file(var.jenkins_public_key_path)
  }

  boot_diagnostics {
    storage_account_uri = azurerm_storage_account.vm_diagnostics_sa.primary_blob_endpoint
  }
  tags = {
    environment="platform"
  }
  custom_data = base64encode(file(var.jenkins_custom_data_path))
}
