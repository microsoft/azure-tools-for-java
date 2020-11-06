/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
