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
package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.util.PlatformUtils;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.ijidea.ui.ArtifactValidationWindow;
import com.microsoft.azuretools.ijidea.ui.ErrorWindow;
import com.microsoft.azuretools.ijidea.ui.WarSelectDialog;
import com.microsoft.azuretools.ijidea.ui.WebAppDeployDialog;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class AzureWebDeployAction extends AzureAnAction {
    private static final Logger LOGGER = Logger.getInstance(AzureWebDeployAction.class);

    public void onActionPerformed(AnActionEvent e) {
//        Module module = LangDataKeys.MODULE.getData(e.getDataContext());
//        Module module1 = e.getData(LangDataKeys.MODULE);
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        JFrame frame = WindowManager.getInstance().getFrame(project);

        try {
            if (!AzureSignInAction.doSignIn( AuthMethodManager.getInstance(), project)) return;

            //Project project = module.getProject();
            ModifiableArtifactModel artifactModel =
                    ProjectStructureConfigurable.getInstance(project).getArtifactsStructureConfigurable().getModifiableArtifactModel();

            Artifact artifactToDeploy = null;
            Collection<? extends Artifact> artifacts = artifactModel.getArtifactsByType(ArtifactType.findById("war"));

            List<String> issues = new LinkedList<>();
            if (artifacts.size() == 0 ) {
                issues.add("A web archive (WAR) Artifact has not been configured yet. The artifact configurations are managed in the <b>Project Structure</b> dialog (<b>File | Project Structure | Artifacts</b>).");
                ArtifactValidationWindow.go(project, issues);
                return;
            } else if (artifacts.size() > 1 ) {
                WarSelectDialog d = WarSelectDialog.go(project, new ArrayList<>(artifacts));
                if (d == null) {
                    return;
                }
                artifactToDeploy = d.getSelectedArtifact();
            } else {
                artifactToDeploy = (Artifact)artifacts.toArray()[0];
            }


            // check artifact name is valid
            String name = artifactToDeploy.getName();

            if (!name.matches("^[A-Za-z0-9]*[A-Za-z0-9]$")) {
                issues.add("The artifact name <b>'" + name + "'</b> is invalid. An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive),\nand the digits '0' through '9'.");
            }

            boolean exists = Files.exists(Paths.get(artifactToDeploy.getOutputFilePath()));
            if (!exists)  {
                issues.add("The Artifact has not been built yet. You can initiate building an artifact using <b>Build | Build Artifacts...</b> menu.");
            }

            if (issues.size() > 0) {
                ArtifactValidationWindow.go(project, issues);
                return;
            }

            WebAppDeployDialog d = WebAppDeployDialog.go(project, artifactToDeploy);

        } catch (Exception ex) {
            ex.printStackTrace();
            //LOGGER.error("actionPerformed", ex);
            ErrorWindow.show(project, ex.getMessage(), "Azure Web Deploy Action Error");
        }

    }

    @Override
    public void update(AnActionEvent e) {
        final Module module = e.getData(LangDataKeys.MODULE);
        e.getPresentation().setVisible(PlatformUtils.isIdeaUltimate());
        if (!PlatformUtils.isIdeaUltimate()) {
            return;
        }
    }
}