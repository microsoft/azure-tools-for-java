/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.DocumentUtil;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class YamlUtils {

    public static void insertYamlConnection(@Nonnull final Connection<?, ?> connection, @Nonnull final InsertionContext context) {
        final YAMLFile yamlFile = context.getFile() instanceof YAMLFile ? (YAMLFile) context.getFile() : null;
        if (Objects.isNull(yamlFile)) {
            return;
        }
        final ResourceDefinition<?> definition = connection.getResource().getDefinition();
        final List<Pair<String, String>> springProperties = ((SpringSupported<?>) definition).getSpringProperties();
        for (final Pair<String, String> pair : springProperties) {
            final YAMLKeyValue target = YAMLUtil.getQualifiedKeyInFile(yamlFile, getKeyListForProperty(pair.getKey()));
            if (Objects.isNull(target) || StringUtils.isBlank(target.getValueText())) {
                final String key = pair.getKey();
                final String value = getPropertiesCompletionValue(pair.getValue(), connection);
                insertYamlKeyValue(key, value, yamlFile, context);
            }
        }
    }

    public static void insertYamlKeyValue(@Nonnull final String property, @Nullable String value,
                                          @Nonnull final YAMLFile yamlFile, @Nonnull final InsertionContext context) {
        final Document document = context.getDocument();
        final Pair<YAMLKeyValue, List<String>> latestKeyValue = getLatestKeyValue(yamlFile, property);
        final YAMLKeyValue parent = latestKeyValue.getKey();
        final YAMLValue parentValue = Optional.ofNullable(parent).map(YAMLKeyValue::getValue).orElse(null);
        final List<String> keyList = latestKeyValue.getValue();
        final int indent = Optional.ofNullable(parentValue)
                .map(YAMLUtil::getIndentToThisElement)
                .orElse(Objects.isNull(parent) ? 0 : YAMLUtil.getIndentToThisElement(parent) + 2);
        final StringBuilder insertContent = new StringBuilder(YAMLElementGenerator.createChainedKey(keyList, indent));
        insertContent.append(StringUtils.SPACE);
        if (StringUtils.isNotBlank(value)) {
            insertContent.append(value);
        }
        final int index = Optional.ofNullable(parentValue)
                .map(v -> v.getTextRange().getEndOffset())
                .orElseGet(() -> (Objects.isNull(parent) ? yamlFile : parent).getTextRange().getEndOffset());
        if (StringUtils.isNotBlank(getOffsetLineContent(document, index))) {
            insertContent.insert(0, StringUtils.repeat(' ', indent));
            insertContent.insert(0, StringUtils.LF);
        }
        final String content = insertContent.toString();
        document.insertString(index, content);
        context.commitDocument();
    }

    private static String[] getKeyListForProperty(@Nonnull final String property) {
        final boolean isCommented = property.startsWith("#");
        final String unCommentedKeyString = isCommented ? StringUtils.trim(property.substring(1)) : property;
        final String[] keys = unCommentedKeyString.split("\\.");
        if (isCommented) {
            keys[keys.length - 1] = "# " + keys[keys.length - 1];
        }
        return keys;
    }

    private static String getOffsetLineContent(@Nonnull final Document document, final int offset) {
        final int lineNumber = document.getLineNumber(offset);
        final TextRange range = DocumentUtil.getLineTextRange(document, lineNumber);
        return document.getText(range);
    }

    @Nonnull
    private static Pair<YAMLKeyValue, List<String>> getLatestKeyValue(@Nonnull final YAMLFile file, final String properties) {
        final String[] keys = getKeyListForProperty(properties);
        for (int i = keys.length - 1; i > 0; i--) {
            final YAMLKeyValue result = YAMLUtil.getQualifiedKeyInFile(file, ArrayUtils.subarray(keys, 0, i));
            if (Objects.nonNull(result)) {
                return Pair.of(result, Arrays.stream(ArrayUtils.subarray(keys, i, keys.length)).toList());
            }
        }
        return Pair.of(null, Arrays.stream(keys).toList());
    }

    public static String getPropertiesCompletionValue(@Nonnull final String template, @Nonnull final Connection<?, ?> connection) {
        return StringUtils.replace(template, Connection.ENV_PREFIX, connection.getEnvPrefix());
    }
}
