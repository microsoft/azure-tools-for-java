/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;

/**
 * Base Azure resource interface.
 */
public interface AzureResource extends Resource {

    /**
     * Azure related resource Id.
     */
    ResourceId getServerId();

}
