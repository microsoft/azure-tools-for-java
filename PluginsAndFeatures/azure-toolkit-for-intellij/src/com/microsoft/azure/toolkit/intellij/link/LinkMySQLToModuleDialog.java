/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.link.mysql.BasicLinkMySQLPanel;
import com.microsoft.azure.toolkit.intellij.link.mysql.JdbcUrl;
import com.microsoft.azure.toolkit.intellij.link.mysql.MySQLResourceConfig;
import com.microsoft.azure.toolkit.intellij.link.po.MySQLResourcePO;
import com.microsoft.azure.toolkit.intellij.mysql.action.LinkMySQLAction;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.link.AzureLinkService;
import com.microsoft.intellij.AzureMySQLStorage;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class LinkMySQLToModuleDialog extends AzureDialog<LinkConfig<MySQLResourceConfig, ModuleResourceConfig>> {

    private static final String PROMPT_TITLE = "Azure Explorer";
    private static final String[] PROMPT_OPTIONS = new String[]{"Yes", "No"};
    private static final String PROMPT_MESSAGE = "This resource already existed in your local environment. Do you want to override it?";
    private JPanel rootPanel;
    private BasicLinkMySQLPanel basicPanel;

    private MySQLNode node;
    private Project project;
    private Module module;

    public LinkMySQLToModuleDialog(Project project, MySQLNode node, Module module) {
        super(project);
        this.project = project;
        this.node = node;
        this.module = module;
        this.init();
        // listener
        this.setOkActionListener((data) -> doLink(data, project, this));
    }

    @Override
    public AzureForm<LinkConfig<MySQLResourceConfig, ModuleResourceConfig>> getForm() {
        return basicPanel;
    }

    public String showAndGetEnvPrefix() {
        boolean exitWithOkButton = this.showAndGet();
        return exitWithOkButton ? basicPanel.getData().getEnvPrefix() : StringUtils.EMPTY;
    }

    @Override
    protected String getDialogTitle() {
        return "Connect Azure Database for MySQL";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        MySQLResourceConfig source = MySQLResourceConfig.getDefaultConfig(node);
        ModuleResourceConfig target = ModuleResourceConfig.getDefaultConfig(module);
        LinkConfig<MySQLResourceConfig, ModuleResourceConfig> linkConfig = new LinkConfig<>(source, target);
        basicPanel = new BasicLinkMySQLPanel(project, () -> linkConfig);
    }

    private void doLink(LinkConfig<MySQLResourceConfig, ModuleResourceConfig> linkConfig, Project project, LinkMySQLToModuleDialog dialog) {
        dialog.close(0);
        // check to prompt override existing resource or not
        MySQLResourceConfig resourceConfig = linkConfig.getResource();
        JdbcUrl jdbcUrl = JdbcUrl.from(resourceConfig.getUrl());
        String businessUniqueKey = MySQLResourcePO.getBusinessUniqueKey(resourceConfig.getServer().id(), jdbcUrl.getDatabase());
        MySQLResourcePO existedResourcePO = AzureMySQLStorage.getStorage().getResourceByBusinessUniqueKey(businessUniqueKey);
        boolean storageResource = true;
        if (Objects.nonNull(existedResourcePO)) {
            if (!StringUtils.equals(resourceConfig.getUrl(), existedResourcePO.getUrl()) ||
                !StringUtils.equals(resourceConfig.getUsername(), existedResourcePO.getUsername()) ||
                resourceConfig.getPasswordConfig().getPasswordSaveType() != existedResourcePO.getPasswordSave()) {
                storageResource = DefaultLoader.getUIHelper().showConfirmation(PROMPT_MESSAGE, PROMPT_TITLE, PROMPT_OPTIONS, null);
            }
        }
        // link in background
        boolean finalStorageResource = storageResource;
        final Runnable runnable = () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            AzureLinkService.getInstance().link(project, linkConfig, finalStorageResource);
            final String message = String.format("The connection between database (%s/%s) and project (%s) has been successfully created.",
                                                 resourceConfig.getServer().name(), resourceConfig.getDatabase().name(), project.getName());
            DefaultLoader.getUIHelper().showInfoNotification(LinkMySQLAction.ACTION_NAME, message);
        };
        final IAzureOperationTitle title = AzureOperationBundle.title("azure-mysql.azure-mysql-link-to-module");
        final AzureTask task = new AzureTask(null, title, false, runnable);
        AzureTaskManager.getInstance().runInBackground(task);
    }

}
