/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

//import java.util.logging.Logger;

public class JsonHelper {
    //    private static final Logger log = Logger.getLogger(JsonHelper.class.getName());
    static ObjectMapper mapper = new ObjectMapper();

    @Nullable
    @Contract("_, null -> null")
    @AzureOperation(name = "common.deserialize_json", type = AzureOperation.Type.TASK)
    public static <T> T deserialize(Class<T> cls, String json) {
        if (json == null) return null;
        try {
            return mapper.readValue(json, cls);
        } catch (final JsonProcessingException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @Nullable
    @Contract("_, null -> null")
    @AzureOperation(name = "common.deserialize_json", type = AzureOperation.Type.TASK)
    public static <T> T deserialize(Class<T> cls, InputStream is) {
        if (is == null) return null;
        try {
            return mapper.readValue(is, cls);
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @Nullable
    @Contract("null -> null")
    @AzureOperation(name = "common.serialize_json", type = AzureOperation.Type.TASK)
    public static <T> String serialize(T jsonObject) {
        if (jsonObject == null) return null;
        try {
            return mapper.writeValueAsString(jsonObject);
        } catch (final JsonProcessingException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }
}
