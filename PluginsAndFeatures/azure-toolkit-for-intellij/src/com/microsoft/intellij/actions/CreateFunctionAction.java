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
 *
 */
package com.microsoft.intellij.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.template.FunctionTemplate;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.eventhub.EventHubNamespace;
import com.microsoft.azure.management.eventhub.EventHubNamespaceAuthorizationRule;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.forms.function.CreateFunctionForm;
import com.microsoft.intellij.runner.functions.AzureFunctionSupportConfigurationType;
import com.microsoft.intellij.util.AzureFunctionsUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateFunctionAction extends CreateElementActionBase {
    public CreateFunctionAction() {
        super("Azure Function Class",
                "newPage.dialog.prompt", IconLoader.getIcon(AzureFunctionSupportConfigurationType.ICON_PATH,
                        AzureFunctionSupportConfigurationType.class));
    }

    @Override
    protected PsiElement[] invokeDialog(Project project, PsiDirectory psiDirectory) {
        PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(psiDirectory);
        // get existing package from current directoy
        String hintPackageName = pkg == null ? "" : pkg.getQualifiedName();

        CreateFunctionForm form = new CreateFunctionForm(project, hintPackageName);
        List<PsiElement> psiElements = new ArrayList<>();
        if (form.showAndGet()) {
            Map<String, String> parameters = form.getTemplateParameters();
            String triggerType = form.getTriggerType();
            String packageName = parameters.get("packageName");
            String className = parameters.get("className");

            PsiDirectory directory = com.intellij.psi.util.ClassUtil.sourceRoot(psiDirectory);
            String newName = packageName.replace('.', '/');
            final FunctionTemplate bindingTemplate;
            try {
                bindingTemplate = form.getFunctionTemplate(triggerType);
                final String functionClassContent = AzureFunctionsUtils.substituteParametersInTemplate(bindingTemplate, parameters);
                Application application = ApplicationManager.getApplication();
                application.runWriteAction(() -> {
                    CreateFileAction.MkDirs mkDirs = ApplicationManager.getApplication().runWriteAction((Computable<CreateFileAction.MkDirs>) () -> new CreateFileAction.MkDirs(newName + '/' + className, directory));
                    PsiFileFactory factory = PsiFileFactory.getInstance(project);
                    CommandProcessor.getInstance().executeCommand(project, () -> {
                        PsiFile psiFile = factory.createFileFromText(className
                                + ".java", JavaFileType.INSTANCE, functionClassContent);

                        psiElements.add(mkDirs.directory.add(psiFile));
                    }, null, null);

                    if (StringUtils.equalsIgnoreCase(triggerType, CreateFunctionForm.EVENT_HUB_TRIGGER)) {
                        String connectionString = getEventHubNamespaceConnectionString(form.getEventHubNamespace());
                        saveLocalSetting(project, parameters.get("connection"), connectionString);
                    }
                });


            } catch (AzureExecutionException e) {
                PluginUtil.displayErrorDialogAndLog("Create Azure Function Class error", e.getMessage(), e);
            }
        }
        if (!psiElements.isEmpty()) {
            FileEditorManager.getInstance(project).openFile(psiElements.get(0).getContainingFile().getVirtualFile(), false);
        }
        return psiElements.toArray(new PsiElement[0]);
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
    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{\"IsEncrypted\":false,\"Values\":{\"AzureWebJobsStorage\":\"\",\"FUNCTIONS_WORKER_RUNTIME\":\"java\"}}";

    public void saveLocalSetting(Project project, String key, String value) {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        ;
        JsonObject localSettings = null;
        final Path localSettingPath = Paths.get(project.getBasePath(), "local.settings.json");
        synchronized (this) {
            try (FileInputStream fis = new FileInputStream(localSettingPath.toFile());
                 InputStreamReader isr = new InputStreamReader(fis)) {
                localSettings = gson.fromJson(isr, JsonObject.class);
            } catch (IOException | JsonParseException e) {
                localSettings = gson.fromJson(DEFAULT_LOCAL_SETTINGS_JSON, JsonObject.class);
            }
        }
        JsonObject appSettings = localSettings.getAsJsonObject("Values");
        appSettings.addProperty(key, value);
        try (Writer writer = new FileWriter(localSettingPath.toFile())) {
            gson.toJson(localSettings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    @Override
    protected PsiElement[] create(@NotNull String s, PsiDirectory psiDirectory) throws Exception {
        return new PsiElement[0];
    }

    private static boolean doCheckPackageExists(PsiDirectory directory) {
        PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(directory);
        if (pkg == null) {
            return false;
        }

        String name = pkg.getQualifiedName();
        return StringUtil.isEmpty(name) || PsiNameHelper.getInstance(directory.getProject()).isQualifiedName(name);
    }


    @Override
    protected boolean isAvailable(final DataContext dataContext) {
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        for (PsiDirectory dir : view.getDirectories()) {
            if (projectFileIndex.isUnderSourceRootOfType(dir.getVirtualFile(), JavaModuleSourceRootTypes.SOURCES) && doCheckPackageExists(dir)) {
                return true;
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
    protected String getActionName(PsiDirectory directory, String newName) {
        return "";
    }


}
