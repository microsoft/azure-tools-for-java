/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.po;


import com.microsoft.azure.toolkit.intellij.link.base.ResourceType;
import com.microsoft.azure.toolkit.intellij.link.mysql.JdbcUrl;
import com.microsoft.azure.toolkit.intellij.link.mysql.PasswordSaveType;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MySQLResourcePO extends BaseResourcePO {

    private String url;
    private String username;
    private String database;
    @Setter
    private PasswordSaveType passwordSave;

    @lombok.Builder
    private MySQLResourcePO(String id, String resourceId, String url, String username, PasswordSaveType passwordSave) {
        super(id, resourceId, ResourceType.AZURE_DATABASE_FOR_MYSQL);
        this.url = url;
        this.username = username;
        this.database = JdbcUrl.from(url).getDatabase();
        this.passwordSave = passwordSave;
    }

    @Override
    public String getBusinessUniqueKey() {
        return getBusinessUniqueKey(getResourceId(), database);
    }

    public static String getBusinessUniqueKey(String resourceId, String database) {
        return ResourceType.AZURE_DATABASE_FOR_MYSQL + "#" + resourceId + "#" + database;
    }
}
