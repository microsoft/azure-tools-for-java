/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.microsoft.azure.toolkit.intellij.connector.database.SpringDatasourceCompletionContributor;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;

import java.util.ArrayList;
import java.util.List;

public class MySQLSpringDatasourceCompletionContributor extends SpringDatasourceCompletionContributor {

    @Override
    public List<LookupElement> generateLookupElements() {
        List<LookupElement> lookupElements = new ArrayList<>();
        lookupElements.add(LookupElementBuilder
                .create(MySQLDatabaseResource.Definition.AZURE_MYSQL.getType(), "spring.datasource.url")
                .withIcon(AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.BIND_INTO))
                .withInsertHandler(new SpringDatasourceCompletionContributor.MyInsertHandler(MySQLDatabaseResource.Definition.AZURE_MYSQL.getType()))
                .withBoldness(true)
                .withTypeText("String")
                .withTailText(" (Azure Database for MySQL)"));
        return lookupElements;
    }

}
