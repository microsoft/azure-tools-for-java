/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.util;

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

    public static String toJsonString(Object o) {
        return GSON.toJson(o);
    }

    public static <T> T fromJsonString(String jsonString, Class<T> clz) {
        return GSON.fromJson(jsonString, clz);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }
}
