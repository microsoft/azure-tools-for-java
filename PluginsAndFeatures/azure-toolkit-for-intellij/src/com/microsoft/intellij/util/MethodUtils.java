/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.ui.libraries.AILibraryHandler;

import java.io.File;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class MethodUtils {

    /**
     * Method scans all open Maven or Dynamic web projects form workspace
     * and returns name of project who is using specific key.
     * @return
     */
    public static String getModuleNameAsPerKey(Project project, String keyToRemove) {
        String name = "";
        try {
            Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module module : modules) {
                if (module != null && module.isLoaded()
                        && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
                    String aiXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
                    String webXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("xmlPath"));
                    AILibraryHandler handler = new AILibraryHandler();
                    if (new File(aiXMLPath).exists() && new File(webXMLPath).exists()) {
                        handler.parseWebXmlPath(webXMLPath);
                        handler.parseAIConfXmlPath(aiXMLPath);
                        // if application insights configuration is enabled.
                        if (handler.isAIWebFilterConfigured()) {
                            String key = handler.getAIInstrumentationKey();
                            if (key != null && !key.isEmpty() && key.equals(keyToRemove)) {
                                return module.getName();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
        return name;
    }
}
