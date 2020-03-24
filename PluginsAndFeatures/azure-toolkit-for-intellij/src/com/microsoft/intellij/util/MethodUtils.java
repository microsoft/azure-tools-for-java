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

import com.intellij.openapi.module.*;
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
                if (module!= null && module.isLoaded() && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
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
        } catch(Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
        return name;
    }
}
