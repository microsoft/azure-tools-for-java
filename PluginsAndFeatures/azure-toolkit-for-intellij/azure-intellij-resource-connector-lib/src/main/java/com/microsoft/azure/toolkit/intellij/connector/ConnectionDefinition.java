package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import org.jdom.Element;

import javax.annotation.Nullable;

public interface ConnectionDefinition<R extends Resource, C extends Resource> {
    Connection<R, C> create(String type, R resource, C consumer);

    Connection<R, C> read(Element element);

    void write(Element element, Connection<? extends R, ? extends C> value);

    boolean validate(Connection<R, C> connection, Project project);

    @Nullable
    default AzureDialog<Connection<R, C>> getConnectorDialog() {
        return null;
    }
}
