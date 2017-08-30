/**
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

package com.microsoft.azuretools.container.handlers;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerProgressHandler;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.ui.wizard.publish.PublishWizard;
import com.microsoft.azuretools.container.ui.wizard.publish.PublishWizardDialog;
import com.microsoft.azuretools.container.utils.ConfigFileUtil;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.container.utils.WarUtil;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.spotify.docker.client.DockerClient;

import rx.Observable;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class PublishHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        IProject project = PluginUtil.getSelectedProject();
        if (project == null || !SignInCommandHandler.doSignIn(window.getShell())) {
            return null;
        }
        String basePath = project.getLocation().toString();
        Properties props = ConfigFileUtil.loadConfig(project);
        DockerRuntime.getInstance().loadFromProps(props);

        ConsoleLogger.info(Constant.MESSAGE_BUILDING_IMAGE);
        Observable.fromCallable(() -> {
            buildImage(project);
            return null;
        }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(ret -> {
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                PublishWizard pw = new PublishWizard();
                WizardDialog pwd = new PublishWizardDialog(window.getShell(), pw);
                if (pwd.open() == Window.OK) {
                    ConsoleLogger.info(String.format("URL: http://%s.azurewebsites.net/",
                            DockerRuntime.getInstance().getLatestWebAppName()));
                    ConfigFileUtil.saveConfig(project, DockerRuntime.getInstance().saveToProps(props));
                }
            });
        }, err -> {
            String dockerHost = DockerRuntime.getInstance().getDockerBuilder().uri().toString();
            String dockerFileRelativePath = Paths.get(basePath).getParent().toUri()
                    .relativize(Paths.get(basePath, Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME).toUri())
                    .toString();
            DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                MessageDialog.openError(window.getShell(), "Error on building image", String
                        .format(Constant.ERROR_BUILDING_IMAGE, dockerHost, dockerFileRelativePath, err.getMessage()));
            });
        });

        return null;
    }

    private void buildImage(IProject project) throws Exception {
        if (project == null) {
            throw new Exception("Project is null.");
        }
        String basePath = project.getLocation().toString();
        DockerClient docker = DockerRuntime.getInstance().getDockerBuilder().build();
        // export WAR file

        String destinationPath = null;
        if (MavenUtils.isMavenProject(project)) {
            destinationPath = MavenUtils.getTargetPath(project);
            MavenUtils.executePackageGoal(project);
        } else {
            destinationPath = Paths.get(basePath, Constant.DOCKERFILE_FOLDER, project.getName() + ".war").normalize()
                    .toString();
            WarUtil.export(project, destinationPath);
        }

        // validate dockerfile
        Path targetDockerfile = Paths.get(basePath, Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME);
        ConsoleLogger.info(String.format("Validating dockerfile ... [%s]", targetDockerfile));
        if (!targetDockerfile.toFile().exists()) {
            throw new FileNotFoundException("Dockerfile not found.");
        }
        // replace placeholder if exists
        String content = new String(Files.readAllBytes(targetDockerfile));
        content = content.replaceAll(Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER,
                Paths.get(basePath).toUri().relativize(Paths.get(destinationPath).toUri()).getPath());
        Files.write(targetDockerfile, content.getBytes());

        // build image based on WAR file
        ConsoleLogger.info(Constant.MESSAGE_BUILDING_IMAGE);
        String imageNameWithTag = DockerUtil.buildImage(docker, Constant.DEFAULT_IMAGE_NAME_WITH_TAG,
                Paths.get(project.getLocation().toString(), Constant.DOCKERFILE_FOLDER), new DockerProgressHandler());
        ConsoleLogger.info(String.format(Constant.MESSAGE_IMAGE_INFO, imageNameWithTag));

        DockerRuntime.getInstance().setLatestImageName(imageNameWithTag);
    }

}
