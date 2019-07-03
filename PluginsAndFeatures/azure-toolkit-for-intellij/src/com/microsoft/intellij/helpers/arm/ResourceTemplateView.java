/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers.arm;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.intellij.language.arm.ARMLanguage;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ARM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.UPDATE_DEPLOYMENT_SHORTCUT;
import static com.microsoft.intellij.serviceexplorer.azure.arm.UpdateDeploymentAction.NOTIFY_UPDATE_DEPLOYMENT_FAIL;
import static com.microsoft.intellij.serviceexplorer.azure.arm.UpdateDeploymentAction.NOTIFY_UPDATE_DEPLOYMENT_SUCCESS;

public class ResourceTemplateView extends BaseEditor {

    public static final String ID = "com.microsoft.intellij.helpers.arm.ResourceTemplateView";
    private JButton saveAsTemplateButton;
    private JButton updateDeploymentButton;
    private JPanel contentPane;
    private JPanel editorPanel;
    private JPanel editorPanels;
    private JPanel parameterPanel;
    private JLabel lblEditorPanel;
    private JLabel lblParametersPanel;
    private DeploymentNode node;
    private Project project;
    private static final String PROMPT_MESSAGE_SAVE_TEMPALTE = "Would you like to save the template file before you exit";
    private static final String PROMPT_MESSAGE_SAVE_PARAMETERS = "Would you like to save the parameters file before you exit";
    private static final String PROMPT_MESSAGE_UPDATE_DEPLOYMENT = "Are you sure you want to update the deployment";
    private FileEditor fileEditor;
    private FileEditor parameterEditor;

    private String originTemplate;
    private String originParameters;

    public void loadTemplate(DeploymentNode node, String template) {
        this.node = node;
        this.project = (Project) node.getProject();
        GridConstraints constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);

        originTemplate = Utils.getPrettyJson(template);
        fileEditor = createEditor(originTemplate);
        editorPanel.add(fileEditor.getComponent(), constraints);

        originParameters = DeploymentUtils.serializeParameters(node.getDeployment());
        parameterEditor = createEditor(originParameters);
        parameterPanel.add(parameterEditor.getComponent(),constraints);


        final MessageBusConnection messageBusConnection = project.getMessageBus().connect(fileEditor);
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {
            @Override
            public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (file.getFileType().getName().equals(ResourceTemplateViewProvider.TYPE) &&
                        file.getName().equals(node.getName())) {
                    try {
                        if(isTemplateUpdate()){
                            if (DefaultLoader.getUIHelper().showConfirmation(PROMPT_MESSAGE_SAVE_TEMPALTE, "Azure Explorer",
                                    new String[]{"Yes", "No"}, null)) {
                                new ExportTemplate(node).doExport(getTemplate());
                            }
                        }
                        if(isPropertiesUpdate()){
                            if (DefaultLoader.getUIHelper().showConfirmation(PROMPT_MESSAGE_SAVE_PARAMETERS, "Azure Explorer",
                                    new String[]{"Yes", "No"}, null)) {
                                new ExportTemplate(node).doExportParameters(getParameters());
                            }
                        }
                    } finally {
                        messageBusConnection.disconnect();
                    }
                }
            }
        });

        saveAsTemplateButton.addActionListener((e) ->{
            ExportTemplate exportTemplate = new ExportTemplate(node);
            exportTemplate.doExport(getTemplate());
            exportTemplate.doExportParameters(getParameters());
        });

        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        updateDeploymentButton.addActionListener((e) -> {
            try {
                if (DefaultLoader.getUIHelper().showConfirmation(PROMPT_MESSAGE_UPDATE_DEPLOYMENT, "Azure Explorer",
                        new String[]{"Yes", "No"}, null)) {
                    ProgressManager.getInstance().run(new Task.Backgroundable(project,
                            "Update your azure resource " + node.getDeployment().name() + "...", false) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            EventUtil.executeWithLog(ARM, UPDATE_DEPLOYMENT_SHORTCUT, (operation -> {
                                node.getDeployment().update()
                                        .withTemplate(getTemplate())
                                        .withParameters(getParameters())
                                        .withMode(DeploymentMode.INCREMENTAL).apply();
                                UIUtils.showNotification(statusBar, NOTIFY_UPDATE_DEPLOYMENT_SUCCESS, MessageType.INFO);
                            }), (e) -> {
                                UIUtils.showNotification(statusBar,
                                        NOTIFY_UPDATE_DEPLOYMENT_FAIL + ", " + e.getMessage(), MessageType.ERROR);
                            });
                        }
                    });
                }
            } catch (Exception ex) {
                UIUtils.showNotification(statusBar, NOTIFY_UPDATE_DEPLOYMENT_FAIL + ", " + ex.getMessage(),
                        MessageType.ERROR);
            }
        });
    }

    private FileEditor createEditor(String template) {
        return PsiAwareTextEditorProvider.getInstance()
                .createEditor(project, new LightVirtualFile(node.getName() + ".json", ARMLanguage.INSTANCE, template));
    }

    public boolean isTemplateUpdate(){
        return !originTemplate.equals(getTemplate());
    }

    public boolean isPropertiesUpdate(){
        return !originParameters.equals(getParameters());
    }

    public String getTemplate(){
        return ((PsiAwareTextEditorImpl) fileEditor).getEditor().getDocument().getText();
    }

    public String getParameters(){
        final String parameters = ((PsiAwareTextEditorImpl) parameterEditor).getEditor().getDocument().getText();
        return DeploymentUtils.parseParameters(parameters);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return contentPane;
    }

    @NotNull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        fileEditor.dispose();
    }
}
