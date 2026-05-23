output "resource_group_name" {
  value = azurerm_resource_group.rg.name
}

output "jenkins_public_ip" {
  value = azurerm_linux_virtual_machine.jenkins_vm.public_ip_address
}
