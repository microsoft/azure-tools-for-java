/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.buildimage;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.DockerUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class DockerBuildTaskProvider extends BeforeRunTaskProvider<DockerBuildTaskProvider.DockerBuildBeforeRunTask> {
    private static final Key<DockerBuildBeforeRunTask> ID = Key.create("DockerBuildBeforeRunTaskProviderId");
    private static final Icon ICON = IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE);
    @Getter
    public Key<DockerBuildBeforeRunTask> id = ID;
    @Getter
    public Icon icon = ICON;

    @Override
    public @Nullable
    Icon getTaskIcon(DockerBuildBeforeRunTask task) {
        return ICON;
    }

    @Nullable
    @Override
    public DockerBuildBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
        return new DockerBuildBeforeRunTask();
    }

    @Override
    public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration, @NotNull ExecutionEnvironment environment, @Nonnull DockerBuildBeforeRunTask task) {
        if (configuration instanceof IDockerConfiguration) {
            try {
                return task.buildImage((IDockerConfiguration) configuration);
            } catch (final Throwable t) {
                AzureMessager.getMessager().error(t.getMessage());
            }
        }
        return false;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getName() {
        return "Build Docker Image";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription(DockerBuildBeforeRunTask task) {
        return task.getDescription();
    }

    @Getter
    @Setter
    public static class DockerBuildBeforeRunTask extends BeforeRunTask<DockerBuildBeforeRunTask> {
        protected DockerBuildBeforeRunTask() {
            super(ID);
        }

        public boolean buildImage(@Nonnull final IDockerConfiguration configuration) {
            final DockerImage image = configuration.getDockerImageConfiguration();
            if (Objects.isNull(image) || Objects.isNull(configuration.getDockerHostConfiguration())) {
                return false;
            }
            final DockerClient dockerClient = DockerUtil.getDockerClient(configuration.getDockerHostConfiguration());
            final ConsoleView consoleView = AzureTaskManager.getInstance().runAndWaitAsObservable(new AzureTask<>(() ->
                    createConsoleView(configuration.getProject(), image.getImageName()))).toBlocking().first();
            final BuildImageResultCallback callback = createBuildImageResultCallback(consoleView);
            DockerUtil.buildImage(dockerClient, image, callback);
            return true;
        }

        private BuildImageResultCallback createBuildImageResultCallback(@Nonnull final ConsoleView consoleView) {
            return new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem object) {
                    super.onNext(object);
                    consoleView.print(object.getStream() + System.lineSeparator(), ConsoleViewContentType.LOG_INFO_OUTPUT);
                }

                @Override
                public void onError(Throwable throwable) {
                    super.onError(throwable);
                    consoleView.print(ExceptionUtils.getStackTrace(throwable), ConsoleViewContentType.LOG_ERROR_OUTPUT);
                }
            };
        }

        private ConsoleView createConsoleView(final Project project, final String imageName) {
            final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
            final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, "azure.guidance.summary", context);
            ActionManager.getInstance().getAction("ActivateRunToolWindow").actionPerformed(event);
            final ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
            final Content result = ContentFactory.getInstance().createContent(console.getComponent(), "Build docker image: " + imageName, false);
            result.setDisposer(console);
            ((ConsoleViewImpl) console).setVisible(true);
            Optional.ofNullable(ToolWindowManager.getInstance(project).getToolWindow("Run")).ifPresent(toolWindow ->
                    toolWindow.getContentManager().addContent(result));
            return console;
        }

        private String getDescription() {
            return "Build Docker Image";
        }
    }
}
