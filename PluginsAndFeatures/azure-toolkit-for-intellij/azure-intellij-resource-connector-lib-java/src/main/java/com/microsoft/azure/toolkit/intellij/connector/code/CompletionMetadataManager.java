package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class CompletionMetadataManager {
    private static final ExtensionPointName<CompletionMetadataProvider> exPoints =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.completionMetadataProvider");
    private static List<CompletionMetadataProvider> providers;

    public synchronized static List<CompletionMetadataProvider> getInputProviders() {
        if (CollectionUtils.isEmpty(providers)) {
            providers = exPoints.extensions().collect(Collectors.toList());
        }
        return providers;
    }

}
