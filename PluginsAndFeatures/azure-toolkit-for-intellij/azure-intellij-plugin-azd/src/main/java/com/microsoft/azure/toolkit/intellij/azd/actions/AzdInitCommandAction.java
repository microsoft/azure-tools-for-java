package com.microsoft.azure.toolkit.intellij.azd.actions;

import java.util.Set;

public class AzdInitCommandAction extends AbstractAzdCommandAction {

    @Override
    public Set<String> getSupportedFiles() {
        return Set.of("pom.xml");
    }
}
