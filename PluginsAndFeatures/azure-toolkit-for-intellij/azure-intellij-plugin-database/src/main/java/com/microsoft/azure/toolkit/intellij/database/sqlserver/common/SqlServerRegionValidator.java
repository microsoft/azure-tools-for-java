/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.common;

import com.microsoft.azure.toolkit.intellij.database.BaseRegionValidator;
import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import lombok.RequiredArgsConstructor;

public class SqlServerRegionValidator extends BaseRegionValidator {

    public SqlServerRegionValidator(RegionComboBox input) {
        super(input, (sid, region) -> Azure.az(AzureSqlServer.class).checkRegionCapability(sid, region));
    }
}
