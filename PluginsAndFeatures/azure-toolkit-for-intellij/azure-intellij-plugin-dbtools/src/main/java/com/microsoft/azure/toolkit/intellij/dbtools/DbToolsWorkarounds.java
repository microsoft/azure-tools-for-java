/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.dbtools;

import com.intellij.database.dataSource.DatabaseDriverImpl;
import com.intellij.database.dataSource.DatabaseDriverManager;
import com.intellij.database.dataSource.url.template.UrlTemplate;
import com.intellij.database.dataSource.url.ui.ParametersLayoutUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DbToolsWorkarounds  {
    public static void loadDriverTemplate(String driverId, String templateKey, String templateName, String template) {
        final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
        final DatabaseDriverImpl driver = ((DatabaseDriverImpl) manager.getDriver(driverId));
        if (Objects.nonNull(driver)) {
            final List<UrlTemplate> templates = new LinkedList<>(driver.getUrlTemplates());
            templates.removeIf(t -> t.getTemplate().contains(templateKey));
            templates.add(0, new UrlTemplate(templateName, template));
            driver.setURLTemplates(templates);
        }
    }

    @SuppressWarnings("unchecked")
    public static void makeParameterShowAtTop(String parameterName) {
        try {
            final Field HEADS = FieldUtils.getField(ParametersLayoutUtils.class, "HEADS", true);
            final List<String> heads = (List<String>) FieldUtils.readStaticField(HEADS, true);
            if (!heads.contains(parameterName)) {
                final Object[] old = heads.toArray();
                heads.set(0, parameterName);
                for (int i = 0; i < old.length - 1; i++) {
                    heads.set(i + 1, (String) old[i]);
                }
            }
        } catch (final Throwable ignored) {
        }
    }
}

