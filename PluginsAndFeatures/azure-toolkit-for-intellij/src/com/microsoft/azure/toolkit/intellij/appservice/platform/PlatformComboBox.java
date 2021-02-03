/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.platform;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.collections.ListUtils;

import java.util.List;

public class PlatformComboBox extends AzureComboBox<Platform> {
    private static final List<Platform> WEB_APP_PLAT_FORMS = ImmutableList.copyOf(new Platform[]{
        Platform.Linux.JAVA8_TOMCAT9,
        Platform.Linux.JAVA8_TOMCAT85,
        Platform.Linux.JAVA8_JBOSS72,
        Platform.Linux.JAVA8,
        Platform.Linux.JAVA11_TOMCAT9,
        Platform.Linux.JAVA11_TOMCAT85,
        Platform.Linux.JAVA11,
        Platform.Windows.JAVA8_TOMCAT9,
        Platform.Windows.JAVA8_TOMCAT85,
        Platform.Windows.JAVA8,
        Platform.Windows.JAVA11_TOMCAT9,
        Platform.Windows.JAVA11_TOMCAT85,
        Platform.Windows.JAVA11
    });

    private List<Platform> platformList;

    public PlatformComboBox() {
        this(WEB_APP_PLAT_FORMS);
    }

    public PlatformComboBox(List<Platform> platformList) {
        this.platformList = ListUtils.unmodifiableList(platformList);
    }

    public void setPlatformList(final List<Platform> platformList) {
        this.platformList = ListUtils.unmodifiableList(platformList);
    }

    @NotNull
    @Override
    protected List<? extends Platform> loadItems() throws Exception {
        return platformList;
    }
}
