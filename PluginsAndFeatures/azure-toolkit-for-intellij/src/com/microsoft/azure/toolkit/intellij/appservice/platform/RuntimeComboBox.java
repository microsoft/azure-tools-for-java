/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.platform;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuntimeComboBox extends AzureComboBox<Runtime> {

    private List<Runtime> platformList;

    public RuntimeComboBox() {
        this(Runtime.WEBAPP_RUNTIME);
    }

    public RuntimeComboBox(List<Runtime> platformList) {
        this.platformList = ListUtils.unmodifiableList(platformList);
    }

    public void setPlatformList(final List<Runtime> platformList) {
        this.platformList = ListUtils.unmodifiableList(platformList);
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Runtime) {
            final Runtime runtime = (Runtime) item;
            final String os = runtime.getOperatingSystem().getValue();
            final String javaVersion = runtime.getJavaVersion() == JavaVersion.OFF ? null : runtime.getJavaVersion().getValue();
            final String webContainer = runtime.getWebContainer() == WebContainer.JAVA_OFF ? null : runtime.getWebContainer().getValue();
            return Stream.of(os, javaVersion, webContainer)
                    .filter(StringUtils::isNotEmpty)
                    .map(StringUtils::capitalize).collect(Collectors.joining("-"));
        }
        return super.getItemText(item);
    }

    @NotNull
    @Override
    protected List<? extends Runtime> loadItems() throws Exception {
        return platformList;
    }
}
