package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

enum ApplicationTemplateType {
    ApplicationProperties("/code-templates/application.properties"),
    ApplicationMain("/code-templates/application.java"),
    ApplicationController("/code-templates/spring-controller.java");

    @NotNull
    private final String resourcePath;

    ApplicationTemplateType(@NotNull String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @NotNull
    String getResourcePath() {
        return resourcePath;
    }

    @NotNull
    String getFilename() {
        return PathUtil.getFileName(resourcePath);
    }
}
