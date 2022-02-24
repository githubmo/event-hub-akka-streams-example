terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 2.65"
    }
  }

  required_version = ">= 1.1.0"
}

provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "example" {
  name     = "myTFResourceGroup"
  location = "uksouth"

  tags = {
    Environment = "Event Hub Example"
    Team        = "LB Example"
  }
}

resource "azurerm_eventhub_namespace" "example" {
  name                = "testhubname0123"
  location            = azurerm_resource_group.example.location
  resource_group_name = azurerm_resource_group.example.name
  sku                 = "Standard"
  capacity            = 1

  tags = {
    environment = "Production"
  }
}

resource "azurerm_eventhub" "example" {
  name                = "testhub0123"
  namespace_name      = azurerm_eventhub_namespace.example.name
  resource_group_name = azurerm_resource_group.example.name
  partition_count     = 2
  message_retention   = 1
}
resource "azurerm_eventgrid_system_topic" "example" {
  name                   = "example-system-topic"
  location               = "Global"
  resource_group_name    = azurerm_resource_group.example.name
  source_arm_resource_id = azurerm_resource_group.example.id
  topic_type             = "Microsoft.Resources.ResourceGroups"
}

resource "azurerm_eventgrid_system_topic_event_subscription" "example" {
  name                = "example-event-subscription"
  system_topic        = azurerm_eventgrid_system_topic.example.name
  resource_group_name = azurerm_resource_group.example.name

  eventhub_endpoint_id = azurerm_eventhub.example.id
}