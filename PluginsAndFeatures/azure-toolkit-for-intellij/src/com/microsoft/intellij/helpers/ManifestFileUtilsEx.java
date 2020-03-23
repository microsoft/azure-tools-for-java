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

package com.microsoft.intellij.helpers;

import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.DefaultLibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiMethodUtil;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ManifestFileUtilsEx implements ILogger {
    private static class MainClassFilter implements ClassFilter {
        final private String filePath;
        public MainClassFilter(@NotNull String filePath) {
            this.filePath = filePath;
        }
        public boolean isAccepted(final PsiClass aClass) {
            return ReadAction.compute(() ->
                    aClass.getContainingFile().getVirtualFile().getPath().startsWith(filePath) &&
                            PsiMethodUtil.MAIN_CLASS.value(aClass) &&
                            !aClass.getName().endsWith("$") &&
                            aClass.findMethodsByName("main", true).length > 0);
        }
    }

    private final static String LOCAL_ARTIFACT_MODULE_NAME = "SparkAppLocalArtifact";
    @NotNull
    private final Project myProject;

    public ManifestFileUtilsEx(final @NotNull Project project) {
        this.myProject = project;
    }

    @Nullable
    public PsiClass selectMainClass(@Nullable VirtualFile jarFile) {
        if (jarFile == null) {
            return null;
        }

        final TreeClassChooserFactory chooserFactory = TreeClassChooserFactory.getInstance(myProject);
        final GlobalSearchScope searchScope = GlobalSearchScope.everythingScope(myProject);
        // TODO: the following code is used to find initialClassName in the jar. When user specified initialClassName
        // in the main-class-selection textfield and clicked the main-class-selection button, the filter result in the dialog
        // should be the initialClass. Currently we don't enable this method since exception happens with the following code.
        // final PsiClass aClass = initialClassName != null ? JavaPsiFacade.getInstance(project).findClass(initialClassName, searchScope) : null;
        final TreeClassChooser chooser =
                chooserFactory.createWithInnerClassesScopeChooser("Select Main Class", searchScope, new MainClassFilter(jarFile.getPath()), null);

        ((TreeJavaClassChooserDialog) chooser).getWindow().addWindowListener(new WindowAdapter() {
            // These fields are recorded to help remove the artifact and the module.
            @Nullable
            private String localArtifactLibraryName;
            @Nullable
            private String localArtifactModuleName;

            @Override
            public void windowOpened(WindowEvent e) {
                // remove old jar and add new jar to project dependency
                WriteAction.run(() -> addJarToModuleDependency(jarFile));
                super.windowOpened(e);
            }
            @Override
            public void windowClosed(WindowEvent e) {
                WriteAction.run(() -> removeModuleAndJar());
                super.windowClosed(e);
            }

            private void addJarToModuleDependency(@NotNull VirtualFile jarFile) {
                try {
                    final Module module = createJarPackingModule();
                    final List<OrderRoot> myRoots = RootDetectionUtil.detectRoots(
                            Arrays.asList(jarFile), null, myProject, new DefaultLibraryRootsComponentDescriptor());
                    final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
                    // find a unique library name in the module
                    final String libraryName = LibraryEditingUtil.suggestNewLibraryName(
                            modifiableModel.getModuleLibraryTable().getModifiableModel(), jarFile.getName());
                    // add library to the model of the new-created module
                    LibrariesContainerFactory.createContainer(modifiableModel).createLibrary(
                            libraryName, LibrariesContainer.LibraryLevel.MODULE, myRoots);
                    modifiableModel.commit();

                    localArtifactModuleName = module.getName();
                    localArtifactLibraryName = libraryName;
                } catch (Exception ex) {
                    log().warn(String.format("Failed to add the user selected jar(%s) into module dependency: %s", jarFile.getPath(), ex.toString()));
                }
            }

            private void removeModuleAndJar() {
                assert(localArtifactLibraryName != null && localArtifactModuleName != null) :
                        String.format("Can't get module name or library name. module:%s, library:%s",
                                localArtifactModuleName, localArtifactLibraryName);

                try {
                    final Module module = ModuleManager.getInstance(myProject).findModuleByName(localArtifactModuleName);

                    // remove library from the model of the module
                    final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
                    modifiableModel.getModuleLibraryTable().removeLibrary(
                            modifiableModel.getModuleLibraryTable().getLibraryByName(localArtifactLibraryName));
                    modifiableModel.commit();

                    // remove module from project
                    ModuleManager.getInstance(myProject).disposeModule(module);

                    localArtifactModuleName = null;
                    localArtifactLibraryName = null;
                } catch (Exception ex) {
                    log().warn(String.format("Failed to remove jar(%s) from module(%s): %s", localArtifactLibraryName, localArtifactModuleName, ex.toString()));
                }
            }
        });
        chooser.showDialog();
        return chooser.getSelected();
    }

    @NotNull
    private String suggestModuleName(@NotNull String baseName) {
        String candidate = baseName;
        int idx = 1;
        while (ModuleManager.getInstance(myProject).findModuleByName(candidate) != null) {
            candidate = baseName + (idx++);
        }
        return candidate;
    }

    @NotNull
    private Module createJarPackingModule() {
        String moduleName = suggestModuleName(LOCAL_ARTIFACT_MODULE_NAME);
        File moduleFile = new File(myProject.getBasePath(), String.format("%s/%s.iml", moduleName, moduleName));
        Module module = ModuleManager.getInstance(myProject).newModule(moduleFile.getPath(), ModuleTypeId.JAVA_MODULE);
        return module;
    }
}
