/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.GradleProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.MavenProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.ProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.components.ModuleDependencyComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;

public class AzureSdkProjectDependencyPanel {
    public static final String DEPENDENCY_ADDED = "Dependency Added";
    public static final String DEPENDENCY_UPDATED = "Dependency Updated";
    public static final String ADD_DEPENDENCY = "Add Dependency";
    public static final String UPDATE_DEPENDENCY = "Update Dependency";

    private ModuleDependencyComboBox cbModule;
    private AzureActionButton<Void> btnAddDependency;
    private JTextPane paneMessage;
    private JPanel pnlRoot;
    private JLabel lblMessageIcon;
    private JPanel pnlMessage;
    @Getter
    private String version;
    @Getter
    private AzureSdkArtifactEntity pkg;

    private final Project project;
    private final DependencyNotificationMessager messager;

    public AzureSdkProjectDependencyPanel(@Nonnull final Project project) {
        this.project = project;
        this.messager = new DependencyNotificationMessager();
        $$$setupUI$$$();
        init();
    }

    public void setVisible(final boolean visible) {
        this.pnlRoot.setVisible(visible);
    }

    public void setPkg(AzureSdkArtifactEntity pkg) {
        this.pkg = pkg;
        this.cbModule.setArtifact(pkg, this.version);
    }

    public void setVersion(String version) {
        this.version = version;
        this.cbModule.setArtifact(this.pkg, version);
    }

    private void init() {
        cbModule.addItemListener(e -> onSelectModule());
        final Action<Void> addDependency = new Action<Void>(Action.Id.of("user/sdk.add_dependency"))
                .withAuthRequired(false)
                .withHandler(ignore -> onAddDependency());
        btnAddDependency.setAction(addDependency);
        lblMessageIcon.setIcon(AllIcons.General.BalloonInformation);
    }

    @AzureOperation(name = "user/sdk.refresh_dependency")
    public void onSelectModule() {
        messager.clean();
        final ProjectModule module = cbModule.getValue();
        if (module == null) {
            btnAddDependency.setEnabled(false);
            btnAddDependency.setText(ADD_DEPENDENCY);
            return;
        }
        final boolean exists = module.isDependencyExists(pkg);
        final boolean upToDate = module.isDependencyUpToDate(pkg, version);
        btnAddDependency.setEnabled(!(exists && upToDate));
        btnAddDependency.setText(exists ? UPDATE_DEPENDENCY : ADD_DEPENDENCY);
    }

    private void onAddDependency() {
        OperationContext.action().setTelemetryProperty("artifact", pkg.getArtifactId());
        messager.clean();
        OperationContext.action().setMessager(messager);
        btnAddDependency.setText("Running...");
        btnAddDependency.setEnabled(false);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>("Add dependency", this::addDependency));
    }

    private void addDependency() {
        final ProjectModule module = cbModule.getValue();
        try {
            final String buttonLabel = module.isDependencyExists(pkg) ? DEPENDENCY_UPDATED : DEPENDENCY_ADDED;
            if (module instanceof MavenProjectModule) {
                DependencyUtils.addOrUpdateMavenDependency((MavenProjectModule) module, pkg, version);
            } else if (module instanceof GradleProjectModule) {
                DependencyUtils.addOrUpdateGradleDependency((GradleProjectModule) module, pkg, version);
            }
            btnAddDependency.setText(buttonLabel);
            btnAddDependency.setEnabled(false);
        } catch (final Throwable t) {
            AzureMessager.getMessager().error(t);
            onSelectModule();
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbModule = new ModuleDependencyComboBox(this.project);
        cbModule.reloadItems();
    }

    private class DependencyNotificationMessager implements IAzureMessager {
        @Override
        public synchronized boolean show(IAzureMessage message) {
            pnlMessage.setVisible(true);
            paneMessage.setDocument(paneMessage.getEditorKit().createDefaultDocument());
            paneMessage.setText(message.getMessage().toString());
            return true;
        }

        public synchronized void clean() {
            paneMessage.setDocument(paneMessage.getEditorKit().createDefaultDocument());
            paneMessage.setText(StringUtils.EMPTY);
            pnlMessage.setVisible(false);
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
