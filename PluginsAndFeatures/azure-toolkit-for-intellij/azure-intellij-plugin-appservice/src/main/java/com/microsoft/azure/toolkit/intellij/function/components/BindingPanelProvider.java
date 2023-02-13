package com.microsoft.azure.toolkit.intellij.function.components;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingTemplate;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BindingPanelProvider {

    private static final ExtensionPointName<TriggerPanelProvider> exPoints =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.triggerPanelProvider");
    private static List<TriggerPanelProvider> providers;

    public synchronized static List<TriggerPanelProvider> getTaskProviders() {
        if (CollectionUtils.isEmpty(providers)) {
            providers = exPoints.extensions().collect(Collectors.toList());
        }
        return providers;
    }

    public static AzureFormPanel<Map<String, String>> createPanel(@Nonnull BindingTemplate template) {
        return getTaskProviders().stream()
                .map(provider -> provider.createPanel(template))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
