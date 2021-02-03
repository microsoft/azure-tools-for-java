/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class TextUtils {
    public static String replaceREPL(Map<String, String> variables, String text) {
        if (StringUtils.isNotBlank(text)) {
            final StrSubstitutor sub = new StrSubstitutor(variables, "$(", ")", '$');
            return sub.replace(text);
        }
        return text;
    }

    private TextUtils() {

    }
}
