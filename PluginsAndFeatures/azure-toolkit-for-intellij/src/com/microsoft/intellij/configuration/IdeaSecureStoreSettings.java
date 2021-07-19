/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.intellij.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@State(name = "Azure Toolkit for IntelliJ", storages = {@Storage("azure-secure-store-settings.xml")})
public class IdeaSecureStoreSettings implements PersistentStateComponent<IdeaSecureStoreSettings.Data> {
    private final Data data = new Data();

    public static IdeaSecureStoreSettings getInstance() {
        return ServiceManager.getService(IdeaSecureStoreSettings.class);
    }

    @Override
    public @Nonnull Data getState() {
        return data;
    }

    @Override
    public void loadState(@Nonnull Data state) {
        XmlSerializerUtil.copyBean(state, data);
    }

    public static class Data {
        @XCollection(style = XCollection.Style.v2)
        @Getter
        private List<String> keys = new ArrayList<>();
    }
}
