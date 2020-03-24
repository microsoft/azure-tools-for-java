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
