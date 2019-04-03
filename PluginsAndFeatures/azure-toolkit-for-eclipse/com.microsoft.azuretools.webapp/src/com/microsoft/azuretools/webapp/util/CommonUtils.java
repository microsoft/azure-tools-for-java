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
