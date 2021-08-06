/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

public class AzureMySQLService {
    private static final AzureMySQLService instance = new AzureMySQLService();

    public static AzureMySQLService getInstance() {
        return AzureMySQLService.instance;
    }
}
