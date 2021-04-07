/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class SpringDatasourceLineMarkerProvider implements LineMarkerProvider {

    @Override
    @Nullable
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@NotNull PsiElement element) {
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
            .filter(c -> MySQLDatabaseResource.Definition.AZURE_MYSQL == c.getConsumer().getDefinition())
            .map(c -> ((MySQLDatabaseResourceConnection) c))
            .filter(c -> StringUtils.equals(envPrefix, c.getResource().getEnvPrefix()))
            .findAny()
            .map(MySQLDatabaseResourceConnection::getResource)
            .map(r -> new LineMarkerInfo<>(
                element, element.getTextRange(),
                AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.BIND_INTO),
                element2 -> String.format("Connect to Azure Database for MySQL (%s)", r.getJdbcUrl().getHost()),
                new SpringDatasourceNavigationHandler(r.getBizId()),
                GutterIconRenderer.Alignment.LEFT, () -> "")).orElse(null);
    }

    private String extractEnvPrefix(String value) {
        if (StringUtils.startsWith(value, "${") && StringUtils.endsWith(value, "_URL}")) {
            return value.substring(2, value.length() - 4);
        }
        return StringUtils.EMPTY;
    }

    public static class SpringDatasourceNavigationHandler implements GutterIconNavigationHandler<PsiElement> {

        private final String resourceId;

        SpringDatasourceNavigationHandler(String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public void navigate(MouseEvent mouseEvent, PsiElement psiElement) {
            if (!AuthMethodManager.getInstance().isSignedIn()) {
                final String resourceName = ResourceId.fromString(resourceId).name();
                final String message = String.format("Failed to connect Azure Database for MySQL (%s) , please sign in Azure first.", resourceName);
                DefaultLoader.getUIHelper().showError(message, "Connect to Azure Datasource for MySQL");
                return;
            }
            final ResourceId resourceIdObject = ResourceId.fromString(resourceId);
            final Server server = MySQLMvpModel.findServer(resourceIdObject.subscriptionId(), resourceIdObject.resourceGroupName(), resourceIdObject.name());
            if (Objects.nonNull(server)) {
                final MySQLNode node = new MySQLNode(null, resourceIdObject.subscriptionId(), server) {
                    @Override
                    public Object getProject() {
                        return psiElement.getProject();
                    }
                };
                DefaultLoader.getUIHelper().openMySQLPropertyView(node);
            }
        }
    }

}
