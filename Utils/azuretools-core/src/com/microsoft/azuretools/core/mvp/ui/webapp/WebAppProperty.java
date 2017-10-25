package com.microsoft.azuretools.core.mvp.ui.webapp;

import java.util.Map;

public class WebAppProperty {

    private Map<String, Object> propertyMap;

    public WebAppProperty(Map<String, Object> propertyMap) {
        this.propertyMap = propertyMap;
    }

    public Object getValue(String key) {
        return this.propertyMap.get(key);
    }

}
