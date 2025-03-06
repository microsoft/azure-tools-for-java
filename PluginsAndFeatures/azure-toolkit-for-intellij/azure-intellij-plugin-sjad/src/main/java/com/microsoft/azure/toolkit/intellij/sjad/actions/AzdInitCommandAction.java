package com.microsoft.azure.toolkit.intellij.sjad.actions;

import java.util.Set;

public class AzdInitCommandAction extends AbstractAzdCommandAction {

    @Override
    public Set<String> getSupportedFiles() {
        return Set.of("pom.xml");
    }
}
