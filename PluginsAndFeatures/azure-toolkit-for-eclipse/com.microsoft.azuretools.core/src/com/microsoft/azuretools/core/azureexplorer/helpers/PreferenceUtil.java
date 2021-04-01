/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.azureexplorer.helpers;


import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.utils.PluginUtil;

import java.util.StringTokenizer;

public class PreferenceUtil {
    private static final String PREFERENCE_DELIMITER = ";";

    public static void savePreference(String name, String value) {
        try {
            Preferences prefs = PluginUtil.getPrefs(com.microsoft.azuretools.core.utils.Messages.prefFileName);
            prefs.put(name, value);
            prefs.flush();
        } catch (BackingStoreException e) {
            Activator.getDefault().log("Error", e);
        }
    }

    public static String[] getPreferenceKeys() {
        String[] keys = null;
        try {
            Preferences prefs = PluginUtil.getPrefs(com.microsoft.azuretools.core.utils.Messages.prefFileName);
            keys = prefs.keys();
        } catch (BackingStoreException e) {
            Activator.getDefault().log("Error", e);
        }
        return keys;
    }

    public static String loadPreference(String name) {
        return loadPreference(name, null);
    }

    public static String loadPreference(String name, String defaultValue) {
        Preferences prefs = PluginUtil.getPrefs(com.microsoft.azuretools.core.utils.Messages.prefFileName);
        return prefs.get(name, defaultValue);
    }

    public static void unsetPreference(String name) {
        Preferences prefs = PluginUtil.getPrefs(com.microsoft.azuretools.core.utils.Messages.prefFileName);
        prefs.remove(name);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            Activator.getDefault().log("Error", e);
        }
    }

    public static void savePreferences(String name, String[] values) {
        Preferences prefs = PluginUtil.getPrefs(com.microsoft.azuretools.core.utils.Messages.prefFileName);
        prefs.put(name, convertToPreference(values));
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            Activator.getDefault().log("Error", e);
        }
    }

    public static String[] loadPreferences(String name) {
        Preferences prefs = PluginUtil.getPrefs(com.microsoft.azuretools.core.utils.Messages.prefFileName);
        String pref = prefs.get(name, null);
        return pref == null ? null : convertFromPreference(pref);
    }

    private static String convertToPreference(String[] elements) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < elements.length; i++) {
            buffer.append(elements[i]);
            buffer.append(PREFERENCE_DELIMITER);
        }
        return buffer.toString();
    }

    private static String[] convertFromPreference(String preferenceValue) {
        StringTokenizer tokenizer = new StringTokenizer(preferenceValue, PREFERENCE_DELIMITER);
        int tokenCount = tokenizer.countTokens();
        String[] elements = new String[tokenCount];
        for (int i = 0; i < tokenCount; i++) {
            elements[i] = tokenizer.nextToken();
        }
        return elements;
    }
}
