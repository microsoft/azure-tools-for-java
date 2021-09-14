/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class SpringDatasourceLineMarkerProvider implements LineMarkerProvider {

    @Override
    @Nullable
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@Nonnull PsiElement element) {
        // Do not show azure line marker if not signed in
        if (!AuthMethodManager.getInstance().isSignedIn() || !(element instanceof PropertyImpl)) {
            return null;
        }
        final PropertyImpl property = (PropertyImpl) element;
        final String propKey = property.getKey();
        final String propVal = property.getValue();
        final String envPrefix = extractEnvPrefix(propVal);
        final Module module = ModuleUtil.findModuleForFile(element.getContainingFile().getVirtualFile(), element.getProject());
        if (!StringUtils.equals(propKey, "spring.datasource.url") || StringUtils.isBlank(envPrefix) || Objects.isNull(module)) {
            return null;
        }
        final Project project = module.getProject();
        return project.getService(ConnectionManager.class)
                .getConnectionsByConsumerId(module.getName()).stream()
                .filter(c -> DatabaseResource.Definition.AZURE_MYSQL.getName().equals(c.getResource().getDefinition().getName())
                        || DatabaseResource.Definition.SQL_SERVER.getName().equals(c.getResource().getDefinition().getName()))
                .map(c -> ((DatabaseResourceConnection) c))
                .filter(c -> StringUtils.equals(envPrefix, c.getResource().getData().getEnvPrefix()))
                .findAny()
                .map(DatabaseResourceConnection::getResource)
                .map(r -> new LineMarkerInfo<>(
                        element, element.getTextRange(),
                        AzureIcons.getIcon("/icons/connector/connect.svg"),
                        element2 -> String.format("Connect to %s (%s)", r.getDefinition().getTitle(), r.getName()),
                        new SpringDatasourceNavigationHandler(r),
                        GutterIconRenderer.Alignment.LEFT, () -> "")).orElse(null);
    }

    private String extractEnvPrefix(String value) {
        if (StringUtils.startsWith(value, "${") && StringUtils.endsWith(value, "_URL}")) {
            return value.substring(2, value.length() - 4);
        }
        return StringUtils.EMPTY;
    }

    @RequiredArgsConstructor
    public static class SpringDatasourceNavigationHandler implements GutterIconNavigationHandler<PsiElement> {
        private final Resource<Database> resource;

        @Override
        public void navigate(MouseEvent mouseEvent, PsiElement psiElement) {
            if (!AuthMethodManager.getInstance().isSignedIn()) {
                final String message = String.format("Failed to connect (%s) , please sign in Azure first.", resource.getName());
                AzureMessager.getMessager().error(message, "Connect to " + resource.getName());
                return;
            }
            if (DatabaseResource.Definition.AZURE_MYSQL == resource.getDefinition()) {
                DefaultLoader.getUIHelper().openMySQLPropertyView(resource.getData().getServerId().id(), psiElement.getProject());
            } else if (DatabaseResource.Definition.SQL_SERVER == resource.getDefinition()) {
                DefaultLoader.getUIHelper().openSqlServerPropertyView(resource.getData().getServerId().id(), psiElement.getProject());
            }
        }
    }

}
