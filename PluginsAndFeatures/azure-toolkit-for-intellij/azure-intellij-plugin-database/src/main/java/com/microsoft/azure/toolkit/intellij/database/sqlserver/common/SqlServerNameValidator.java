/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.sqlserver.common;

import com.microsoft.azure.toolkit.intellij.database.BaseNameValidator;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import lombok.RequiredArgsConstructor;

public class SqlServerNameValidator extends BaseNameValidator {
    public SqlServerNameValidator(ServerNameTextField input) {
        super(input, (sid, name) -> Azure.az(AzureSqlServer.class).checkNameAvailability(sid, name));
    }
}