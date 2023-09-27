/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.action;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.ide.actions.CreateFileAction;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
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
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionClassCreationDialog;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class CreateFunctionAction extends CreateElementActionBase {
    private static final String DEFAULT_EVENT_HUB_CONNECTION_STRING = "Endpoint=sb://<your-envent-hub-namespace>.servicebus.windows.net/;" +
            "SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<your-SAS-key>;";

    public CreateFunctionAction() {
        super(message("function.createFunction.action.title"), "newPage.dialog.prompt", IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.MODULE));
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/function.create_function_class")
    protected PsiElement[] invokeDialog(Project project, PsiDirectory psiDirectory) {
        final Operation operation = TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.CREATE_FUNCTION_TRIGGER);
        try {
            operation.start();
            final PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(psiDirectory);
            // get existing package from current directory
            final String hintPackageName = pkg == null ? "" : pkg.getQualifiedName();
            final Module module = ModuleUtil.findModuleForPsiElement(psiDirectory);
            final FunctionClassCreationDialog form = new FunctionClassCreationDialog(module);
            form.setPackage(hintPackageName);

            final List<PsiElement> psiElements = new ArrayList<>();
            form.setOkActionListener(result -> {
                form.close();
                final FunctionTemplate bindingTemplate = result.getTemplate();
                final Map<String, String> parameters = result.getParameters();
                final String connectionName = parameters.get("connection");
                final String triggerType = result.getTemplate().getTriggerType();
                final String packageName = parameters.get("packageName");
                final String className = parameters.get("className");
                final PsiDirectory directory = ClassUtil.sourceRoot(psiDirectory);
                final String newName = packageName.replace('.', '/');
                operation.trackProperty(TelemetryConstants.TRIGGER_TYPE, triggerType);

                final String functionClassContent = bindingTemplate.generateContent(parameters);
                if (StringUtils.isNotEmpty(functionClassContent)) {
                    AzureTaskManager.getInstance().write(() -> {
                        final CreateFileAction.MkDirs mkDirs = ApplicationManager.getApplication().runWriteAction(
                                (Computable<CreateFileAction.MkDirs>) () ->
                                        new CreateFileAction.MkDirs(newName + '/' + className, directory));
                        final PsiFileFactory factory = PsiFileFactory.getInstance(project);
                        try {
                            mkDirs.directory.checkCreateFile(className + ".java");
                        } catch (final IncorrectOperationException e) {
                            final String dir = mkDirs.directory.getName();
                            final String error = String.format("failed to create function class[%s] in directory[%s]", className, dir);
                            throw new AzureToolkitRuntimeException(error, e);
                        }
                        CommandProcessor.getInstance().executeCommand(project, () -> {
                            final PsiFile psiFile = factory.createFileFromText(className + ".java", JavaFileType.INSTANCE, functionClassContent);
                            psiElements.add(mkDirs.directory.add(psiFile));
                        }, null, null);
                    });
                }
            });
            form.show();
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
        if (project == null || project.isDisposed()) {
            return false;
        }
        try {
            final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
            final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            if (view != null) {
                final List<PsiDirectory> dirs = Arrays.stream(view.getDirectories()).filter(Objects::nonNull).collect(Collectors.toList());
                for (final PsiDirectory dir : dirs) {
                    if (projectFileIndex.isUnderSourceRootOfType(dir.getVirtualFile(), JavaModuleSourceRootTypes.SOURCES) && doCheckPackageExists(dir)) {
                        return true;
                    }
                }
            }
        } catch (final RuntimeException e) {
            // swallow exception when check action availability
        }
        return false;
    }

    @Override
    protected String getErrorTitle() {
        return "Cannot Create Function Class";
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
        final PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(directory);
        if (pkg == null) {
            return false;
        }

        final String name = pkg.getQualifiedName();
        return StringUtil.isEmpty(name) || PsiNameHelper.getInstance(directory.getProject()).isQualifiedName(name);
    }
}
