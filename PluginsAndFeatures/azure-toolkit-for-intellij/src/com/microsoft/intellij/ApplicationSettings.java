package com.microsoft.intellij;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(
        name = "HDInsightSettings",
        storages = {
                @Storage(file = "$APP_CONFIG$/azure.application.settings.xml")
        })
public class ApplicationSettings implements PersistentStateComponent<ApplicationSettings.State> {
    private State myState = new State();

    public static ApplicationSettings getInstance() {
        return ServiceManager.getService(ApplicationSettings.class);
    }

    public void setProperty(String name, String value) {
        myState.properties.put(name, value);
    }

    public String getProperty(String name) {
        return myState.properties.get(name);
    }

    public void unsetProperty(String name) {
        myState.properties.remove(name);
        myState.array_properties.remove(name);
    }

    public boolean isPropertySet(String name) {
        return myState.properties.containsKey(name) || myState.array_properties.containsKey(name);
    }

    public String[] getProperties(String name) {
        return myState.array_properties.get(name);
    }

    public void setProperties(String name, String[] value) {
        myState.array_properties.put(name, value);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public static class State {
        public Map<String, String> properties = new HashMap<String, String>();
        public Map<String, String[]> array_properties = new HashMap<String, String[]>();
    }
}
