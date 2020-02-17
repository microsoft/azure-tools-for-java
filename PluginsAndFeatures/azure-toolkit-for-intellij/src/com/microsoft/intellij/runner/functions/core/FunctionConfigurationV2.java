package com.microsoft.intellij.runner.functions.core;

public class FunctionConfigurationV2 {
    private String scriptFile;
    private String entryPoint;
    private BindingV2[] bindings;

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public void setBindings(BindingV2[] bindings) {
        this.bindings = bindings;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public BindingV2[] getBindings() {
        return bindings;
    }
}
