/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.provider;

import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.code.model.CompletionItem;
import com.microsoft.azure.toolkit.intellij.connector.code.model.ParameterIdentifier;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MethodCompletionMetadata {
    @Nullable
    private AzureIcon icon;
    @NonNull
    private ParameterIdentifier identifier;
    // get completion item from existing connections
    @Nonnull
    private Function<Module, List<Connection<? extends AzResource, ?>>> connectedResourcesFunction;
    @Nonnull
    private Function<Connection<? extends AzResource, ?>, List<CompletionItem>> completionItemsFunction;
    // function to create new resource connection
    @Nullable
    private ResourceDefinition resourceDefinition;
    @Nullable
    private Function<Module, List<? extends AzResource>> azureResourcesFunction;
}
