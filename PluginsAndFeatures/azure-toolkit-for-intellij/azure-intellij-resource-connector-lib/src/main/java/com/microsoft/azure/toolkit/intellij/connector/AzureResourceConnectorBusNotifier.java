/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.util.messages.Topic;

public interface AzureResourceConnectorBusNotifier {

    Topic<AzureResourceConnectorBusNotifier> AZURE_RESOURCE_CONNECTOR_TOPIC =
            Topic.create("azure-resource-connector-added-topic", AzureResourceConnectorBusNotifier.class);

    void afterAction(Connection<? extends Resource, ? extends Resource> connection);
}
