/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class ObjectConvertUtils {
    private static JsonFactory jsonFactory = new JsonFactory();
    private static ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
    private static XmlMapper xmlMapper = new XmlMapper();

    public static <T> Optional<T> convertJsonToObject(@NotNull String jsonString, @NotNull Class<T> clazz) throws IOException {
        return Optional.ofNullable(objectMapper.readValue(jsonString, clazz));
    }

    public static <T> T convertToObjectQuietly(@NotNull String jsonString, @NotNull Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            // ignore the exception
        }
        return null;
    }

    public static <T> Optional<T> convertEntityToObject(@NotNull HttpEntity entity, @NotNull Class<T> clazz) throws IOException {
        // To handle complex response Content-Type value.
        // Ref to: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type
        // Like --
        //   Content-Type: application/xml; charset=UTF-8
        final String type = entity.getContentType().getValue().toLowerCase().split(";")[0].trim();

        switch (type) {
            case "application/json" :
                return convertJsonToObject(EntityUtils.toString(entity), clazz);
            case "application/xml" :
                return convertXmlToObject(EntityUtils.toString(entity), clazz);
            default:
        }
        return Optional.empty();
    }

    public static <T> Optional<List<T>> convertEntityToList(@NotNull HttpEntity entity, @NotNull Class<T> clazz) throws IOException {
        // To handle complex response Content-Type value.
        // Ref to: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type
        // Like --
        //   Content-Type: application/xml; charset=UTF-8
        final String type = entity.getContentType().getValue().toLowerCase().split(";")[0].trim();

        switch (type) {
            case "application/json" :
                return convertJsonToList(EntityUtils.toString(entity), clazz);
            case "application/xml" :
                return convertXmlToList(EntityUtils.toString(entity), clazz);
            default:
        }
        return Optional.empty();
    }

    public static <T> Optional<List<T>> convertJsonToList(@NotNull String jsonString, Class<T> clazz) throws IOException {
        List<T> myLists = objectMapper.readValue(jsonString, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
        return Optional.ofNullable(myLists);
    }

    public static <K, V> Optional<Map<K, V>> convertJsonToMap(@NotNull String jsonString) {
        try {
            Map<K, V> map = objectMapper.readValue(jsonString, new TypeReference<Map<K, V>>() {
            });
            return Optional.ofNullable(map);
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    public static <T> Optional<List<T>> convertXmlToList(@NotNull String jsonString, Class<T> clazz) throws IOException {
        List<T> myLists = xmlMapper.readValue(jsonString, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
        return Optional.ofNullable(myLists);
    }

    public static <T> Optional<String> convertObjectToJsonString(@NotNull T obj) {
        try {
            return Optional.ofNullable(objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static <T> Optional<String> convertObjectToXmlString(@NotNull T obj) {
        try {
            return Optional.ofNullable(xmlMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static <T> Optional<T> convertXmlToObject(@NotNull String xmlString, Class<T> clazz) throws IOException {
        return Optional.ofNullable(xmlMapper.readValue(xmlString, clazz));
    }
}
