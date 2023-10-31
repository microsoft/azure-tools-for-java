/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.model;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CompletionItem {
    public static Function<Connection<? extends AzResource, ?>, List<String>> DEFAULT_LOOKUP_VALUES_FUNCTION = connection ->
            Arrays.asList(connection.getResource().getName(), connection.getEnvPrefix());

    private String value;
    private String displayName;
    private String hintMessage;
    private AzureIcon icon;
    private List<String> lookupValues;
}
