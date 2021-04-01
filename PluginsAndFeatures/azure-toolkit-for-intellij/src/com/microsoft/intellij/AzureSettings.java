/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import org.apache.xmlbeans.impl.util.Base64;

import java.io.*;
import java.util.*;

import static com.microsoft.intellij.AzurePlugin.log;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@State(
        name = "AzureSettings",
        storages = {@Storage("azure/azureSettings.xml")}
)
public class AzureSettings implements PersistentStateComponent<AzureSettings.State> {
    private static final String PREFERENCE_DELIMITER = ";";

    private State myState = new State();

    public static AzureSettings getSafeInstance(Project project) {
        AzureSettings settings = ServiceManager.getService(project, AzureSettings.class);
        return settings != null ? settings : new AzureSettings();
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public void loadAppInsights() {
        try {
            if (myState.appInsights != null) {
                byte[] data = Base64.decode(myState.appInsights.getBytes());
                ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                ObjectInput input = new ObjectInputStream(buffer);
                try {
                    ApplicationInsightsResource[] resources = (ApplicationInsightsResource[]) input.readObject();
                    for (ApplicationInsightsResource resource : resources) {
                        if (!ApplicationInsightsResourceRegistry.getAppInsightsResrcList().contains(resource)) {
                            ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resource);
                        }
                    }
                } finally {
                    input.close();
                }
            }
        } catch (ClassNotFoundException ex) {
            // ignore - this happens because class package changed and settings were not updated
        } catch (Exception e) {
            log(message("err"), e);
        }
    }

    public void saveAppInsights() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutput output = new ObjectOutputStream(buffer);
            List<ApplicationInsightsResource> data = ApplicationInsightsResourceRegistry.getAppInsightsResrcList();
            /*
             * Sort list according to application insights resource name.
             * Save only manually added resources
             */
            ApplicationInsightsResource[] dataArray = data.stream().filter(a -> !a.isImported()).sorted().toArray(ApplicationInsightsResource[]::new);
            try {
                output.writeObject(dataArray);
            } finally {
                output.close();
            }
            myState.appInsights = new String(Base64.encode(buffer.toByteArray()));
        } catch (IOException e) {
            log(message("err"), e);
        }
    }

    public void setProperty(String name, String value) {
        myState.properties.put(name, value);
    }

    public String getProperty(String name) {
        return myState.properties.get(name);
    }

    public void unsetProperty(String name) {
        myState.properties.remove(name);
    }

    public Set<String> getPropertyKeys() {
        return myState.properties.keySet();
    }

    public boolean isPropertySet(String name) {
        return myState.properties.containsKey(name);
    }

    public String[] getProperties(String name) {
        String properties = getProperty(name);
        return properties == null ? new String[0] : convertFromProperties(properties);
    }

    public void setProperties(String name, String[] values) {
        setProperty(name, convertToProperties(values));
    }

    private static String convertToProperties(String[] elements) {
        StringBuilder buffer = new StringBuilder();
        for (String element : elements) {
            buffer.append(element);
            buffer.append(PREFERENCE_DELIMITER);
        }
        return buffer.toString();
    }

    private static String[] convertFromProperties(String propertiesValue) {
        StringTokenizer tokenizer = new StringTokenizer(propertiesValue, PREFERENCE_DELIMITER);
        int tokenCount = tokenizer.countTokens();
        String[] elements = new String[tokenCount];
        for (int i = 0; i < tokenCount; i++) {
            elements[i] = tokenizer.nextToken();
        }
        return elements;
    }

    public static class State {
        public String storageAccount;
        public String appInsights;
        public String publishProfile;
        public String webApps;
        public Map<String, String> properties = new HashMap<String, String>();
    }
}
