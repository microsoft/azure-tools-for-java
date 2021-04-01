/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.toolkit.intellij.link.base.LinkType;
import com.microsoft.azure.toolkit.intellij.link.mysql.JdbcUrl;
import com.microsoft.azure.toolkit.intellij.link.po.LinkPO;
import com.microsoft.azure.toolkit.intellij.link.po.MySQLResourcePO;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import com.microsoft.intellij.AzureLinkStorage;
import com.microsoft.intellij.AzureMySQLStorage;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.util.Objects;

public class SpringDatasourceLineMarkerProvider implements LineMarkerProvider {

    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        // Do not show azure line marker if not signed in
        if (!AuthMethodManager.getInstance().isSignedIn()) {
            return null;
        }
        if (element instanceof PropertyImpl) {
            PropertyImpl property = (PropertyImpl) element;
            String value = property.getValue();
            if (StringUtils.equals(property.getKey(), "spring.datasource.url")) {
                String envPrefix = extractEnvPrefix(property.getValue());
                if (StringUtils.isBlank(envPrefix)) {
                    return null;
                }
                Module module = ModuleUtil.findModuleForFile(element.getContainingFile().getVirtualFile(), element.getProject());
                LinkPO link = AzureLinkStorage.getProjectStorage(element.getProject()).getLinkByModuleId(module.getName())
                        .stream()
                        .filter(e -> LinkType.SERVICE_WITH_MODULE == e.getType() && StringUtils.equals(envPrefix, e.getEnvPrefix()))
                        .findFirst().orElse(null);
                if (Objects.isNull(link)) {
                    return null;
                }
                MySQLResourcePO service = AzureMySQLStorage.getStorage().getResourceById(link.getResourceId());
                if (Objects.isNull(service)) {
                    return null;
                }
                JdbcUrl url = JdbcUrl.from(service.getUrl());
                LineMarkerInfo lineMarkerInfo = new LineMarkerInfo<>(element, element.getTextRange(),
                        AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.BIND_INTO),
                        element2 -> String.format("Connect to Azure Database for MySQL (%s)", url.getHostname()),
                        new SpringDatasourceNavigationHandler(service.getResourceId()),
                        GutterIconRenderer.Alignment.LEFT);
                return lineMarkerInfo;
            }
        }
        return null;
    }

    private String extractEnvPrefix(String value) {
        if (StringUtils.startsWith(value, "${") && StringUtils.endsWith(value, "_URL}")) {
            return value.substring(2, value.length() - 4);
        }
        return StringUtils.EMPTY;
    }

    public class SpringDatasourceNavigationHandler implements GutterIconNavigationHandler {

        private String resourceId;

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
            ResourceId resourceIdObject = ResourceId.fromString(resourceId);
            Server server = MySQLMvpModel.findServer(resourceIdObject.subscriptionId(), resourceIdObject.resourceGroupName(), resourceIdObject.name());
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
