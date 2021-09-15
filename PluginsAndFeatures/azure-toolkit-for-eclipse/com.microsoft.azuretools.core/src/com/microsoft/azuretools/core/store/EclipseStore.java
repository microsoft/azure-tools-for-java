/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.store;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.azure.toolkit.ide.common.store.IIdeStore;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azuretools.core.Activator;

public class EclipseStore implements IIdeStore {
    private IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);

    @Nullable
    @Override
    public String getProperty(@Nullable String service, @NotNull String key) {
        return node.get(combineKey(service, key), "");
    }

    @Nullable
    @Override
    public String getProperty(@Nullable String service, @NotNull String key, @Nullable String defaultValue) {
        return node.get(combineKey(service, key), defaultValue);
    }

    @Override
    public void setProperty(@Nullable String service, @NotNull String key, @Nullable String value) {
    	if (value == null) {
    		node.remove(combineKey(service, key));
    		return;
    		
    	}
        node.put(combineKey(service, key), value);
        try {
			node.flush();
		} catch (BackingStoreException e) {
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
		}
    }


    private static String combineKey(String service, String key) {
        return StringUtils.isBlank(service) ? key : String.format("%s.%s", service, key);
    }
}
