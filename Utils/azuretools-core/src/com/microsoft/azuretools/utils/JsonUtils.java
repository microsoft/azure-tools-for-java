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

package com.microsoft.azuretools.utils;

import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;

public class JsonUtils {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Gson getGson() {
        return GSON;
    }

    public static <T> T deepCopyWithJson(T source) {
        return (T) GSON.fromJson(GSON.toJson(source), source.getClass());
    }

    public static void writeJsonToFile(File targetFile, JsonObject jsonObject) throws IOException {
        try (Writer writer = new FileWriter(targetFile)) {
            GSON.toJson(jsonObject, writer);
        }
    }

    public static void writeJsonToFile(File targetFile, Map<String, Object> json) throws IOException {
        try (Writer writer = new FileWriter(targetFile)) {
            GSON.toJson(json, writer);
        }
    }

    public static JsonObject readJsonFile(File target) {
        try (FileInputStream fis = new FileInputStream(target);
             InputStreamReader isr = new InputStreamReader(fis)) {
            final Gson gson = new Gson();
            return gson.fromJson(isr, JsonObject.class);
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    public static <T> T fromJsonString(String jsonString, Class<T> clz) {
        return GSON.fromJson(jsonString, clz);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }
}
