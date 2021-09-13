/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.common.store;

import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DefaultMachineStore implements IMachineStore {

    private String dataFile;
    private Map<String, String> map = new HashMap<>();

    public DefaultMachineStore(String dataFile) {
        this.dataFile = dataFile;
        if (Files.exists(Paths.get(dataFile))) {
            load();
        }
    }

    @Nullable
    public String getProperty(@javax.annotation.Nullable String service, @Nonnull String key) {
        return map.get(combineKey(service, key));
    }

    @Nullable
    public String getProperty(@javax.annotation.Nullable String service, @Nonnull String key, @Nullable String defaultValue) {
        return StringUtils.firstNonBlank(map.get(combineKey(service, key)), defaultValue);
    }

    public void setProperty(@javax.annotation.Nullable String service, @Nonnull String key, @Nullable String value) {
        String hashKey = combineKey(service, key);
        if (value == null) {
            map.remove(hashKey);
            return;
        }
        map.put(hashKey, value);
        save();
    }

    private static String combineKey(String service, String key) {
        return StringUtils.isBlank(service) ? key : String.format("%s.%s", service, key);
    }

    public void load() {
        try {
            if (Files.exists(Paths.get(dataFile))) {
                String json = FileUtils.readFileToString(new File(dataFile), "utf8");
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                map = JsonUtils.getGson().fromJson(json, type);
            }
        } catch (Exception ex) {
            throw new AzureToolkitRuntimeException("Cannot load property.", ex);
        }
    }

    public void save() {
        try {
            FileUtils.writeStringToFile(new File(dataFile), JsonUtils.toJson(map), "utf8");
        } catch (Exception ex) {
            throw new AzureToolkitRuntimeException("Cannot save property", ex);
        }
    }

}
