package com.microsoft.azure.toolkit.intellij.connector.code.provider;

import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.code.model.AnnotationIdentifier;
import com.microsoft.azure.toolkit.intellij.connector.code.model.CompletionItem;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationCompletionMetadata {
    private AzureIcon icon;
    private AnnotationIdentifier identifier;
    // get completion item from existing connections
    private Function<Module, List<Connection<? extends AzResource, ?>>> connectedResourcesFunction;
    private BiFunction<Connection<? extends AzResource, ?>, Map<String, String>, List<CompletionItem>> completionItemsFunction;
    // function to create new resource connection
    private ResourceDefinition resourceDefinition;
    private Function<Module, List<? extends AzResource>> azureResourcesFunction;
}
