/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function;

import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azuretools.utils.JsonUtils;
import com.microsoft.intellij.util.TextUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

public class AzureFunctionsUtils {
    private static List<FunctionTemplate> functionTemplates;

    public static FunctionTemplate getFunctionTemplate(String trigger) throws AzureExecutionException {
        if (functionTemplates == null) {
            functionTemplates = FunctionUtils.loadAllFunctionTemplates();
        }
        return functionTemplates.stream()
            .filter(template -> StringUtils.equalsIgnoreCase(trigger, template.getFunction()))
            .findFirst().orElseThrow(() -> new AzureExecutionException("No such template"));
    }

    public static void applyKeyValueToLocalSettingFile(File localSettingFile, String key, String value) throws IOException {
        if (!localSettingFile.getParentFile().isDirectory()) {
            throw new IOException("Cannot save file to a non-existing directory: " + localSettingFile.getParent());
        }
        final JsonObject localSettingRoot = localSettingFile.exists() ?
            JsonUtils.readJsonFile(localSettingFile) : new JsonObject();
        if (localSettingRoot.has("IsEncrypted")) {
            localSettingRoot.add("IsEncrypted", new JsonPrimitive(false));
        }
        JsonObject appSettings = localSettingRoot.getAsJsonObject("Values");
        if (appSettings == null) {
            appSettings = new JsonObject();
            localSettingRoot.add("Values", appSettings);
        }

        appSettings.addProperty(key, value);
        JsonUtils.writeJsonToFile(localSettingFile, localSettingRoot);
    }

    public static String normalizeClassName(String className) {
        if (StringUtils.isBlank(className)) {
            return "untitled";
        }
        final String trimClassName = className.endsWith(".java") ? className.substring(0, className.length() - 5) : className;
        return trimClassName.replaceAll("[^\\.a-zA-Z0-9\\_\\$]+", "").replace('.', '_');
    }

    public static void copyResourceFileWithVariableSubstituted(String fileName, Map<String, String> variables, File targetFolder) throws IOException {
        String resourceFilePath = String.format("/azurefunction/templates/project/%s.template", StringUtils.replace(fileName, ".", "_"));
        URL url = AzureFunctionsUtils.class.getResource(resourceFilePath);
        if (url == null) {
            return;
        }
        final String templateContent = IOUtils.toString(AzureFunctionsUtils.class.getResourceAsStream(resourceFilePath), "utf8");
        FileUtils.writeStringToFile(new File(targetFolder, fileName), TextUtils.replaceREPL(variables, templateContent), "utf8");
    }

    public static File createFunctionProjectToTempFolder(final String groupId, final String artifactId,
                                                         final String version, final String tool)
        throws IOException {
        final File folder = Files.createTempDir();
        final Map<String, String> map = new HashMap<>();
        map.put("groupId", groupId);
        map.put("artifactId", artifactId);
        map.put("version", version);
        map.put("timestamp", Long.toString(new Date().getTime()));
        for (String file : Arrays.asList(".gitignore", "host.json", "local.settings.json")) {
            copyResourceFileWithVariableSubstituted(file, map, folder);
        }
        if (StringUtils.equalsIgnoreCase("maven", tool)) {
            copyResourceFileWithVariableSubstituted("pom.xml", map, folder);
        } else if (StringUtils.equalsIgnoreCase("gradle", tool)) {
            copyResourceFileWithVariableSubstituted("build.gradle", map, folder);
            copyResourceFileWithVariableSubstituted("settings.gradle", map, folder);
        }
        return folder;
    }

    public static String generateFunctionClassByTrigger(String trigger, String packageName, String className) throws IOException {
        final File tempFile = StreamUtil.getResourceFile(String.format("/azurefunction/templates/%s.template", trigger));
        if (tempFile.exists()) {
            final String templateText = FileUtils.readFileToString(tempFile, Charset.defaultCharset());
            final Map<String, String> map = new HashMap<>();
            map.put("packageName", packageName);
            map.put("className", className);
            return TextUtils.replaceREPL(map, templateText);
        }
        return null;
    }

    public static String substituteParametersInTemplate(final FunctionTemplate template, final Map<String, String> params)
        throws AzureExecutionException {
        String ret = template.getFiles().get("function.java");
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null) {
                throw new AzureExecutionException("Required property:" + entry.getKey() + " is missing");
            }
            ret = ret.replace(String.format("$%s$", entry.getKey()), entry.getValue());
        }
        return ret;
    }
}
