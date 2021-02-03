/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

//import java.util.logging.Logger;

public class JsonHelper {
//    private static final Logger log = Logger.getLogger(JsonHelper.class.getName());

    public static <T> T deserialize(Class<T> cls, String json) throws IOException {
//        log.log(Level.FINEST, "structure: " + cls.getName());
//        log.log(Level.FINEST, "json string: " + json);
        if(json == null) return null;
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, cls);
    }

    public static <T> T deserialize(Class<T> cls,InputStream is) throws IOException {
        if(is == null) return null;
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(is, cls);
    }

    public static <T> String serialize(T jsonObject) throws IOException {
        if(jsonObject == null) return null;
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(jsonObject);
    }
}
