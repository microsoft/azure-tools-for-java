/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice.platform;


import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuntimeComboBox extends AzureComboBox<Runtime> {

    private List<Runtime> platformList;

    public RuntimeComboBox(Composite parent) {
        this(parent, Runtime.WEBAPP_RUNTIME);
    }

    public RuntimeComboBox(Composite parent, List<Runtime> platformList) {
        super(parent, false);
        setPlatformList(platformList);
    }

    public void setPlatformList(final List<Runtime> platformList) {
        this.platformList = ListUtils.unmodifiableList(platformList);
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Runtime ? getRuntimeDisplayName((Runtime) item) : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Runtime> loadItems() throws Exception {
        return platformList;
    }

    public String getRuntimeDisplayName(@Nonnull final Runtime runtime) {
        // TODO: move common lib
        final String os = runtime.getOperatingSystem().getValue();
        final String javaVersion = Objects.equals(runtime.getJavaVersion(), JavaVersion.OFF) ?
                null : String.format("Java %s", runtime.getJavaVersion().getValue());
        final String webContainer = Objects.equals(runtime.getWebContainer(), WebContainer.JAVA_OFF) ?
                null : runtime.getWebContainer().getValue();
        return Stream.of(os, javaVersion, webContainer)
                .filter(StringUtils::isNotEmpty)
                .map(StringUtils::capitalize).collect(Collectors.joining("-"));
    }
}
