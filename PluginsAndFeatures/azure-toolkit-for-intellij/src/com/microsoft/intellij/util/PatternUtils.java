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

package com.microsoft.intellij.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils of Regular Regex Pattern
 */
public class PatternUtils {

    public static final String PATTERN_WHOLE_WORD = "[^\\f\\r\\n\\t\\s,]+";
    public static final String PATTERN_WHOLE_NUMBER = "\\d{1,}";
    public static final String PATTERN_WHOLE_NUMBER_PORT = "\\d{1,5}";

    public static String parseWordByPattern(String source, String patternString) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        if (StringUtils.isBlank(patternString)) {
            return source;
        }
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(source);
        String result = null;
        while (matcher.find()) {
            result = matcher.group(0);
            break;
        }
        return result;
    }

    public static String parseLastWordByPattern(String source, String patternString) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        if (StringUtils.isBlank(patternString)) {
            return source;
        }
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(source);
        String result = null;
        while (matcher.find()) {
            result = matcher.group(0);
        }
        return result;
    }

    public static String parseWordByPatternAndPrefix(String source, String patternString, String prefix) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        if (StringUtils.isBlank(patternString)) {
            return source;
        }
        String resultWithPrefix = parseWordByPattern(source, prefix + patternString);
        String result = null;
        if (StringUtils.isNotBlank(resultWithPrefix)) {
            result = parseLastWordByPattern(resultWithPrefix, patternString);
        }
        return result;
    }

}
