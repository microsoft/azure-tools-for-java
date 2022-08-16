/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

public class Dbms {
    public static final com.intellij.database.Dbms AZ_COSMOS_MONGO = com.intellij.database.Dbms.create("AZ_COSMOS_MONGO", "Azure Cosmos DB (MongoDB)", () -> IntelliJAzureIcons.getIcon(AzureIcons.Cosmos.MODULE));
    public static final com.intellij.database.Dbms AZ_COSMOS_CASSANDRA = com.intellij.database.Dbms.create("AZ_COSMOS_CASSANDRA", "Azure Cosmos DB (Cassandra)", () -> IntelliJAzureIcons.getIcon(AzureIcons.Cosmos.MODULE));
}
