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
