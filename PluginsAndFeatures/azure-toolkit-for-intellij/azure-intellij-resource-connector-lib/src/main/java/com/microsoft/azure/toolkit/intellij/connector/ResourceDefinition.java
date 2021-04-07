package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import org.jdom.Element;

import javax.annotation.Nonnull;

public interface ResourceDefinition<T extends Resource> {
    int RESOURCE = 1;
    int CONSUMER = 2;
    int BOTH = RESOURCE | CONSUMER;

    default int isResourceOrConsumer() {
        return RESOURCE;
    }

    default String getName() {
        return this.getType();
    }

    String getType();

    AzureFormJPanel<T> getResourcesPanel(@Nonnull final String type, final Project project);

    /**
     * write {@param value} to {@param element} for persistence
     *
     * @return true if to persist, false otherwise
     */
    boolean write(@Nonnull final Element element, @Nonnull final T value);

    T read(@Nonnull final Element element);
}
