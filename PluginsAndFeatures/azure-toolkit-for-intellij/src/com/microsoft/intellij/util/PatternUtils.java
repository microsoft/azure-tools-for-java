package com.microsoft.intellij.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils of Regular Regex Pattern
 */
public class PatternUtils {

    public final static String PATTERN_WHOLE_WORD = "[^\\f\\r\\n\\t\\s,]+";
    public final static String PATTERN_WHOLE_NUMBER = "\\d{1,}";
    public final static String PATTERN_WHOLE_NUMBER_PORT = "\\d{1,5}";

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
        while(matcher.find()) {
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
        while(matcher.find()) {
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
