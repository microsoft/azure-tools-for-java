package com.microsoft.azure.toolkit.intellij.azd.actions;

import java.util.Set;

public class AzdUpCommandAction extends AzdCommandAction {

    @Override
    public Set<String> getSupportedFiles() {
        return Set.of("azure.yaml");
    }
}
