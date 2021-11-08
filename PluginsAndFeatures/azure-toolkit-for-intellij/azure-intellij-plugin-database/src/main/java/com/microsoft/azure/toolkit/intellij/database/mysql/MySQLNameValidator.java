/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.mysql;

import com.microsoft.azure.toolkit.intellij.database.BaseNameValidator;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import lombok.RequiredArgsConstructor;

public class MySQLNameValidator extends BaseNameValidator {

    public MySQLNameValidator(ServerNameTextField input) {
        super(input, (sid, name) -> Azure.az(AzureMySql.class).checkNameAvailability(sid, name));
    }
}
