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

package com.microsoft.azuretools.core.applicationinsights;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;


public class ApplicationInsightsResourceRegistryEclipse {

    /**
     * Method updates application insights registry by adding, removing or updating resources.
     * @param client
     * @throws IOException
     * @throws RestOperationException
     * @throws AzureCmdException
     */
    public static void updateApplicationInsightsResourceRegistry(List<SubscriptionDetail> subList) throws Exception {
        for (SubscriptionDetail sub : subList) {
            if (sub.isSelected()) {
                try {
                    // fetch resources available for particular subscription
                    List<Resource> resourceList = AzureSDKManager.getApplicationInsightsResources(sub);

                    // Removal logic
                    List<ApplicationInsightsResource> registryList = ApplicationInsightsResourceRegistry
                            .getResourceListAsPerSub(sub.getSubscriptionId());
                    List<ApplicationInsightsResource> importedList = ApplicationInsightsResourceRegistry
                            .prepareAppResListFromRes(resourceList, sub);
                    List<String> inUsekeyList = getInUseInstrumentationKeys();
                    for (ApplicationInsightsResource registryRes : registryList) {
                        if (!importedList.contains(registryRes)) {
                            String key = registryRes.getInstrumentationKey();
                            int index = ApplicationInsightsResourceRegistry.getResourceIndexAsPerKey(key);
                            if (inUsekeyList.contains(key)) {
                                // key is used by project but not present in
                                // cloud,
                                // so make it as manually added resource and not
                                // imported.
                                ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(key, key,
                                        Messages.unknown, Messages.unknown, Messages.unknown, Messages.unknown, false);
                                ApplicationInsightsResourceRegistry.getAppInsightsResrcList().set(index, resourceToAdd);
                            } else {
                                // key is not used by any project then delete
                                // it.
                                ApplicationInsightsResourceRegistry.getAppInsightsResrcList().remove(index);
                            }
                        }
                    }

                    // Addition logic
                    List<ApplicationInsightsResource> list = ApplicationInsightsResourceRegistry
                            .getAppInsightsResrcList();
                    for (Resource resource : resourceList) {
                        ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(resource.getName(),
                                resource.getInstrumentationKey(), sub.getSubscriptionName(), sub.getSubscriptionId(),
                                resource.getLocation(), resource.getResourceGroup(), true);
                        if (list.contains(resourceToAdd)) {
                            int index = ApplicationInsightsResourceRegistry
                                    .getResourceIndexAsPerKey(resource.getInstrumentationKey());
                            ApplicationInsightsResource objectFromRegistry = list.get(index);
                            if (!objectFromRegistry.isImported()) {
                                ApplicationInsightsResourceRegistry.getAppInsightsResrcList().set(index, resourceToAdd);
                            }
                        } else {
                            ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resourceToAdd);
                        }
                    }
                } catch (Exception e) {
                    Activator.getDefault().log(String.format(Messages.aiListErr, sub.getSubscriptionName()), e);
                }
            }
        }
        ApplicationInsightsPreferences.save();
        ApplicationInsightsPreferences.setLoaded(true);
    }

    public static void keeepManuallyAddedList() {
        List<ApplicationInsightsResource> addedList = ApplicationInsightsResourceRegistry.getAddedResources();
        List<String> addedKeyList = new ArrayList<String>();
        for (ApplicationInsightsResource res : addedList) {
            addedKeyList.add(res.getInstrumentationKey());
        }
        List<String> inUsekeyList = getInUseInstrumentationKeys();
        for (String inUsekey : inUsekeyList) {
            if (!addedKeyList.contains(inUsekey)) {
                ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(
                        inUsekey, inUsekey, Messages.unknown, Messages.unknown,
                        Messages.unknown, Messages.unknown, false);
                addedList.add(resourceToAdd);
            }
        }
        ApplicationInsightsResourceRegistry.setAppInsightsResrcList(addedList);
        ApplicationInsightsPreferences.save();
        ApplicationInsightsPreferences.setLoaded(true);
    }

    /**
     * Method scans all open Maven or Dynamic web projects form workspace
     * and prepare a list of instrumentation keys which are in use.
     * @return
     */
    public static List<String> getInUseInstrumentationKeys() {
        List<String> keyList = new ArrayList<String>();
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            for (IProject iProject : root.getProjects()) {
                if (iProject.isOpen() && WebPropertyTester.isWebProj(iProject)) {
                    String aiXMLPath;
                    if (iProject.hasNature(Messages.natMaven)) {
                        aiXMLPath = Messages.aiXMLPathMaven;
                    } else {
                        aiXMLPath = Messages.aiXMLPath;
                    }
                    AILibraryHandler handler = new AILibraryHandler();
                    IFile file = iProject.getFile(aiXMLPath);
                    if (file.exists()) {
                        handler.parseAIConfXmlPath(file.getLocation().toOSString());
                        String key = handler.getAIInstrumentationKey();
                        if (key != null && !key.isEmpty()) {
                            keyList.add(key);
                        }
                    }
                }
            }
        } catch(Exception ex) {
            Activator.getDefault().log(ex.getMessage(), ex);
        }
        return keyList;
    }
}
