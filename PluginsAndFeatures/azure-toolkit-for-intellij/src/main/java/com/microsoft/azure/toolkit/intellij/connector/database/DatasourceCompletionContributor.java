/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;

import java.util.List;
import java.util.stream.Collectors;

public class DatasourceCompletionContributor extends SpringDatasourceCompletionContributor {

    @Override
    public List<LookupElement> generateLookupElements() {
        return DatabaseResource.Definition.values().stream().map(definition -> LookupElementBuilder
                        .create(definition.getName(), "spring.datasource.url")
                        .withIcon(AzureIcons.getIcon("/icons/connector/connect.svg"))
                        .withInsertHandler(new MyInsertHandler(definition))
                        .withBoldness(true)
                        .withTypeText("String")
                        .withTailText(String.format(" (%s)", definition.getTitle())))
                .collect(Collectors.toList());
    }
}
