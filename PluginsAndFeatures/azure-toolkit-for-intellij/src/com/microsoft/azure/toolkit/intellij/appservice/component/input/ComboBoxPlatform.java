package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

public class ComboBoxPlatform extends AzureComboBox<Platform> {
    private static final List<Platform> platforms = ImmutableList.copyOf(new Platform[]{
            Platform.Linux.JAVA8_TOMCAT9,
            Platform.Linux.JAVA8_TOMCAT85,
            Platform.Linux.JAVA8_JBOSS72,
            Platform.Linux.JAVA8,
            Platform.Linux.JAVA11_TOMCAT9,
            Platform.Linux.JAVA11_TOMCAT85,
            Platform.Linux.JAVA11,
            Platform.Windows.JAVA8_TOMCAT9,
            Platform.Windows.JAVA8_TOMCAT85,
            Platform.Windows.JAVA8_JBOSS72,
            Platform.Windows.JAVA8,
            Platform.Windows.JAVA11_TOMCAT9,
            Platform.Windows.JAVA11_TOMCAT85,
            Platform.Windows.JAVA11
    });

    @NotNull
    @Override
    protected List<? extends Platform> loadItems() throws Exception {
        return platforms;
    }
}
