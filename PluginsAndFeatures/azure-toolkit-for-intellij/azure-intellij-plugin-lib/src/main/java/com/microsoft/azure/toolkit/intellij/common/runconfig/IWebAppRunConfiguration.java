/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.runconfig;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.openapi.module.Module;
import com.intellij.util.messages.Topic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public interface IWebAppRunConfiguration {
    Topic<ModuleChangedListener> MODULE_CHANGED = Topic.create("runconfiguration.module.changed", ModuleChangedListener.class);

    interface ModuleChangedListener {
        void moduleMayChanged(@Nonnull RunConfiguration config, @Nullable ConfigurationSettingsEditorWrapper editor);
    }

    void setApplicationSettings(Map<String, String> env);

    Map<String, String> getApplicationSettings();

    Module getModule();
}
