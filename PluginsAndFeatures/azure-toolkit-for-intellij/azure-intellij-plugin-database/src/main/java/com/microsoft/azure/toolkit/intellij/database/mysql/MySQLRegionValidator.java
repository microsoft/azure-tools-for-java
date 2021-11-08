/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql;

import com.microsoft.azure.toolkit.intellij.database.BaseRegionValidator;
import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;

public class MySQLRegionValidator extends BaseRegionValidator {
    public MySQLRegionValidator(RegionComboBox input) {
        super(input, (sid, region) -> Azure.az(AzureMySql.class).subscription(sid).checkRegionAvailability(region));
    }
}
