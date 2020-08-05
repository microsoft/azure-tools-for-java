/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.ide.actions.CreateFileAction;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.template.FunctionTemplate;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.eventhub.EventHubNamespace;
import com.microsoft.azure.management.eventhub.EventHubNamespaceAuthorizationRule;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.forms.function.CreateFunctionForm;
import com.microsoft.intellij.runner.functions.AzureFunctionSupportConfigurationType;
import com.microsoft.intellij.util.AzureFunctionsUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_FUNCTION_TRIGGER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;

public class CreateFunctionAction extends CreateElementActionBase {
    private static final String DEFAULT_EVENT_HUB_CONNECTION_STRING = "Endpoint=sb://<your-envent-hub-namespace>.servicebus.windows.net/;" +
            "SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<your-SAS-key>;";

    public CreateFunctionAction() {
        super("Azure Function Class",
                "newPage.dialog.prompt", IconLoader.getIcon(AzureFunctionSupportConfigurationType.ICON_PATH,
                        AzureFunctionSupportConfigurationType.class));
    }

    @Override
    protected PsiElement[] invokeDialog(Project project, PsiDirectory psiDirectory) {
        final Operation operation = TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.CREATE_FUNCTION_TRIGGER);
        try {
            operation.start();
            PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(psiDirectory);
            // get existing package from current directory
            String hintPackageName = pkg == null ? "" : pkg.getQualifiedName();

            CreateFunctionForm form = new CreateFunctionForm(project, hintPackageName);
            List<PsiElement> psiElements = new ArrayList<>();
            if (form.showAndGet()) {
                final FunctionTemplate bindingTemplate;
                try {
                    Map<String, String> parameters = form.getTemplateParameters();
                    final String connectionName = parameters.get("connection");
                    String triggerType = form.getTriggerType();
                    String packageName = parameters.get("packageName");
                    String className = parameters.get("className");
                    PsiDirectory directory = ClassUtil.sourceRoot(psiDirectory);
                    String newName = packageName.replace('.', '/');
                    bindingTemplate = AzureFunctionsUtils.getFunctionTemplate(triggerType);
                    EventUtil.logEvent(EventType.info, FUNCTION, CREATE_FUNCTION_TRIGGER, new HashMap<String, String>() {{
                            put("triggerType", triggerType);
                        }});
                    if (StringUtils.equalsIgnoreCase(triggerType, CreateFunctionForm.EVENT_HUB_TRIGGER)) {
                        if (StringUtils.isBlank(connectionName)) {
                            throw new AzureExecutionException("Required property 'connection' is missing");
                        }
                        parameters.putIfAbsent("eventHubName", "myeventhub");
                        parameters.putIfAbsent("consumerGroup", "$Default");
                    }

                    final String functionClassContent = AzureFunctionsUtils.substituteParametersInTemplate(bindingTemplate, parameters);
                    if (StringUtils.isNotEmpty(functionClassContent)) {
                        Application application = ApplicationManager.getApplication();
                        application.runWriteAction(() -> {

                            CreateFileAction.MkDirs mkDirs = ApplicationManager.getApplication().runWriteAction(
                                    (Computable<CreateFileAction.MkDirs>) () ->
                                            new CreateFileAction.MkDirs(newName + '/' + className, directory));
                            PsiFileFactory factory = PsiFileFactory.getInstance(project);
                            try {
                                mkDirs.directory.checkCreateFile(className + ".java");
                            } catch (IncorrectOperationException e) {
                                PluginUtil.displayErrorDialog("Create Azure Function Class error", e.getMessage());
                                return;
                            }
                            CommandProcessor.getInstance().executeCommand(project, () -> {
                                PsiFile psiFile = factory.createFileFromText(className + ".java", JavaFileType.INSTANCE, functionClassContent);
                                psiElements.add(mkDirs.directory.add(psiFile));
                            }, null, null);

                            if (StringUtils.equalsIgnoreCase(triggerType, CreateFunctionForm.EVENT_HUB_TRIGGER)) {
                                try {
                                    String connectionString = form.getEventHubNamespace() == null ? DEFAULT_EVENT_HUB_CONNECTION_STRING :
                                            getEventHubNamespaceConnectionString(form.getEventHubNamespace());

                                    AzureFunctionsUtils.applyKeyValueToLocalSettingFile(new File(project.getBasePath(), "local.settings.json"),
                                            parameters.get("connection"), connectionString);
                                } catch (IOException e) {
                                    PluginUtil.displayErrorDialogAndLog("Create Azure Function Class error", e.getMessage(), e);
                                    EventUtil.logError(operation, ErrorType.systemError, e, null, null);
                                }
                            }
                        });
                    }
                } catch (AzureExecutionException e) {
                    PluginUtil.displayErrorDialogAndLog("Create Azure Function Class error", e.getMessage(), e);
                    EventUtil.logError(operation, ErrorType.systemError, e, null, null);
                }
            }
            if (!psiElements.isEmpty()) {
                FileEditorManager.getInstance(project).openFile(psiElements.get(0).getContainingFile().getVirtualFile(), false);
            }
            return psiElements.toArray(new PsiElement[0]);
        } finally {
            operation.complete();
        }
    }

    @NotNull
    @Override
    protected PsiElement[] create(@NotNull String s, PsiDirectory psiDirectory) throws Exception {
        return new PsiElement[0];
    }

    @Override
    protected boolean isAvailable(final DataContext dataContext) {
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return false;
        }
        final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        if (view != null) {
            for (PsiDirectory dir : view.getDirectories()) {
                if (projectFileIndex.isUnderSourceRootOfType(dir.getVirtualFile(), JavaModuleSourceRootTypes.SOURCES) && doCheckPackageExists(dir)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String getErrorTitle() {
        return "Cannot create Function Class";
    }

    @Override
    protected String getCommandName() {
        return "";
    }

    @Override
    protected String getActionName(PsiDirectory psiDirectory, String s) {
        return "";
    }

    private static boolean doCheckPackageExists(PsiDirectory directory) {
        PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(directory);
        if (pkg == null) {
            return false;
        }

        String name = pkg.getQualifiedName();
        return StringUtil.isEmpty(name) || PsiNameHelper.getInstance(directory.getProject()).isQualifiedName(name);
    }

    private String getEventHubNamespaceConnectionString(EventHubNamespace eventHubNamespace) {
        try {
            Azure azure = AuthMethodManager.getInstance().getAzureClient(eventHubNamespace.id().split("/")[2]);
            EventHubNamespaceAuthorizationRule eventHubNamespaceAuthorizationRule = azure.eventHubNamespaces().
                    authorizationRules().getByName(eventHubNamespace.resourceGroupName(), eventHubNamespace.name(),
                    "RootManageSharedAccessKey");
            return eventHubNamespaceAuthorizationRule.getKeys().primaryConnectionString();
        } catch (IOException e) {
            return null;
        }
    }
}
