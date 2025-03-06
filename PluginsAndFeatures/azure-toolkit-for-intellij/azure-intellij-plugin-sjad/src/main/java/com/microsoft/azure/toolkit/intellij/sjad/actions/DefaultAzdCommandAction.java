package com.microsoft.azure.toolkit.intellij.sjad.actions;

import java.util.Set;

public class DefaultAzdCommandAction extends AbstractAzdCommandAction {

    @Override
    public Set<String> getSupportedFiles() {
        return Set.of("azure.yaml");
    }
}
