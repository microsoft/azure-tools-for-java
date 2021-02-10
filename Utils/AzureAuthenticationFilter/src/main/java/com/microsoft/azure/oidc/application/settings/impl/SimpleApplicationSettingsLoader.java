/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.application.settings.impl;

import com.microsoft.azure.oidc.application.settings.ApplicationSettings;
import com.microsoft.azure.oidc.application.settings.ApplicationSettingsLoader;

public final class SimpleApplicationSettingsLoader implements ApplicationSettingsLoader {
    private static final ApplicationSettingsLoader INSTANCE = new SimpleApplicationSettingsLoader();

    @Override
    public ApplicationSettings load() {
        return new SimpleApplicationSettings();
    }

    public static ApplicationSettingsLoader getInstance() {
        return INSTANCE;
    }
}
