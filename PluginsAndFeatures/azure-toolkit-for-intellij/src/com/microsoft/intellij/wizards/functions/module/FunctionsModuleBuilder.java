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
package com.microsoft.intellij.wizards.functions.module;

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.util.DisposeAwareRunnable;
import com.microsoft.intellij.runner.functions.AzureFunctionSupportConfigurationType;
import com.microsoft.intellij.util.AzureFunctionsUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.functions.AzureFunctionsConstants;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

public class FunctionsModuleBuilder extends JavaModuleBuilder {
    private WizardContext wizardContext;

    @Nullable
    @Override
    public String getBuilderId() {
        return "azurefunctions";
    }

    @Override
    public String getDescription() {
        return "Azure Functions module";
    }

    @Override
    public String getPresentableName() {
        return "Azure Functions";
    }

    @Override
    public Icon getNodeIcon() {
        return IconLoader.getIcon(AzureFunctionSupportConfigurationType.ICON_PATH, AzureFunctionSupportConfigurationType.class);
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull final WizardContext wizardContext,
            @NotNull final ModulesProvider modulesProvider) {
        return new ModuleWizardStep[] { new FunctionsModuleInfoStep(wizardContext) };
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull final SettingsStep settingsStep) {
        final ModuleNameLocationSettings moduleNameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (moduleNameLocationSettings != null) {
            moduleNameLocationSettings.setModuleName(
                    settingsStep.getContext().getUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY));
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Override
    public ModuleWizardStep[] createFinishingSteps(@NotNull final WizardContext wizardContext,
            @NotNull final ModulesProvider modulesProvider) {
        this.wizardContext = wizardContext;
        return super.createFinishingSteps(wizardContext, modulesProvider);
    }

    @Override
    public void setupRootModel(@NotNull final ModifiableRootModel rootModel) {
        final VirtualFile root = createAndGetContentEntry();
        rootModel.addContentEntry(root);

        // todo this should be moved to generic ModuleBuilder
        if (myJdk != null) {
            rootModel.setSdk(myJdk);
        } else {
            rootModel.inheritSdk();
        }
        final Project project = rootModel.getProject();
        runWhenInitialized(rootModel.getProject(), () -> {
            final String tool = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_TOOL_KEY);
            final String groupId = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_GROUPID_KEY);
            final String artifactId = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY);
            final String version = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_VERSION_KEY);
            final String packageName = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_PACKAGE_NAME_KEY);
            final String[] triggers = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_TRIGGERS_KEY);
            File tempProjectFolder = null;
            try {
                tempProjectFolder = AzureFunctionsUtils.createFunctionProjectToTempFolder(groupId, artifactId, version, tool);
                if (tempProjectFolder != null) {
                    if (tempProjectFolder.exists() && tempProjectFolder.isDirectory()) {
                        final File moduleFile = new File(getContentEntryPath());
                        final File srcFolder = Paths.get(tempProjectFolder.getAbsolutePath(), "src/main/java").toFile();

                        for (final String trigger : triggers) {
                            // class name like HttpTriggerFunction
                            final String className = trigger + "Function";
                            final String fileContent = AzureFunctionsUtils.generateFunctionClassByTrigger(trigger, packageName, className);
                            final File targetFile = Paths.get(srcFolder.getAbsolutePath(), String.format("%s/%s.java",
                                    packageName.replace('.', '/'), className)).toFile();
                            targetFile.getParentFile().mkdirs();
                            FileUtils.write(targetFile,
                                    fileContent, "utf-8");
                        }
                        FileUtils.copyDirectory(tempProjectFolder, moduleFile);

                        final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleFile);
                        RefreshQueue.getInstance().refresh(true, true, null, new VirtualFile[] { vf });
                        final VirtualFile pomFile = vf.findChild("pom.xml");
                        if (pomFile != null) {
                            final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
                            mavenProjectsManager.addManagedFiles(Collections.singletonList(pomFile));
                        }
                    }
                }
            } catch (final Exception e) {
                PluginUtil.displayErrorDialogAndLog("Error", "Cannot create Azure Function Project in Java.", e);
            } finally {
                if (tempProjectFolder != null && tempProjectFolder.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(tempProjectFolder);
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(final WizardContext context, final Disposable parentDisposable) {
        return new FunctionTriggerChooserStep(context);
    }

    private static void runWhenInitialized(@NotNull final Project project, @NotNull final Runnable runnable) {
        if (project.isDisposed()) {
            return;
        }

        if (isNoBackgroundMode()) {
            runnable.run();
            return;
        }

        if (!project.isInitialized()) {
            StartupManager.getInstance(project).registerStartupActivity(runnable);
            return;
        }

        runDumbAware(project, runnable);
    }

    private static void runDumbAware(@NotNull final Project project, @NotNull final Runnable r) {
        if (DumbService.isDumbAware(r)) {
            r.run();
        } else {
            DumbService.getInstance(project).runWhenSmart(DisposeAwareRunnable.create(r, project));
        }
    }

    private static boolean isNoBackgroundMode() {
        return (ApplicationManager.getApplication().isUnitTestMode() ||
            ApplicationManager.getApplication().isHeadlessEnvironment());
    }

    private VirtualFile createAndGetContentEntry() {
        final String path = FileUtil.toSystemIndependentName(getContentEntryPath());
        new File(path).mkdirs();
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }
}
