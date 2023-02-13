package com.microsoft.azure.toolkit.intellij.function.components;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingTemplate;

import java.util.Map;

public interface TriggerPanelProvider {
    AzureFormPanel<Map<String, String>> createPanel(BindingTemplate template);
}
