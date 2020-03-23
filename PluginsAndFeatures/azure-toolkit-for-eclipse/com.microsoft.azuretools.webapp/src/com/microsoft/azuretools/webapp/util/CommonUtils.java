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

package com.microsoft.azuretools.webapp.util;

import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.webapp.Activator;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.widgets.Combo;

public class CommonUtils {

    public static final String SUBSCRIPTION = "subscription";
    public static final String WEBAPP_NAME = "webapp_name";
    public static final String SLOT_NAME = "slot_name";
    public static final String SLOT_CONF = "slot_conf";
    public static final String RUNTIME_OS = "runtime_os";
    public static final String RUNTIME_LINUX = "runtime_linux";
    public static final String RUNTIME_JAVAVERSION = "runtime_javaversion";
    public static final String RUNTIME_WEBCONTAINER = "runtime_webcontainer";
    public static final String ASP_NAME = "asp_name";
    public static final String ASP_CREATE_LOCATION = "asp_create_location";
    public static final String ASP_CREATE_PRICING = "asp_create_pricing";
    public static final String RG_NAME = "rg_name";
    private static final IEclipsePreferences node = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);

    public static String getPreference(String name) {
        try {
            return node.get(name, "");
        } catch (Exception ignore) {
            return "";
        }
    }

    public static void setPreference(String name, String value) {
        try {
            node.put(name, value);
        } catch (Exception ignore) {
        }
    }

    public static void selectComboIndex(Combo combo, String target) {
        if (combo != null && !StringUtils.isNullOrWhiteSpace(target) && combo.getItemCount() > 0) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItem(i).equals(target)) {
                    combo.select(i);
                    break;
                }
            }
        }
    }

    public static String getSelectedItem(Combo combo) {
        int index = combo.getSelectionIndex();
        if (index < 0) {
            return "";
        }
        return combo.getItem(index);
    }
}
