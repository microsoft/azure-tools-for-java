/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.buildimage;

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
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.IDockerConfiguration;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                AzureMessager.getMessager().error(t);
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
            if (Objects.isNull(image) || Objects.isNull(configuration.getDockerHostConfiguration()) || !FileUtil.exists(image.getDockerFile())) {
                return false;
            }
            final AzureDockerClient dockerClient = AzureDockerClient.from(configuration.getDockerHostConfiguration());
            final ConsoleView consoleView = AzureTaskManager.getInstance().runAndWait(() ->
                createConsoleView(configuration.getProject(), image.getImageName())).join();
            final BuildImageResultCallback callback = createBuildImageResultCallback(consoleView);
            final File baseDir = Optional.ofNullable(image.getBaseDirectory()).map(File::new).orElse(null);
            dockerClient.buildImage(image.getImageName(), new File(image.getDockerFile()), baseDir, callback);
            return true;
        }

        private BuildImageResultCallback createBuildImageResultCallback(@Nonnull final ConsoleView consoleView) {
            return new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    super.onNext(item);
                    final String stream = item.getStream();
                    final String status = item.getStatus();
                    final String id = item.getId();
                    final String progress = item.getProgress();
                    final String message = Stream.of(stream, status, id, progress).filter(StringUtils::isNoneBlank).collect(Collectors.joining(" "));
                    if (StringUtils.isNoneBlank(message)) {
                        consoleView.print(message + System.lineSeparator(), ConsoleViewContentType.SYSTEM_OUTPUT);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    super.onError(throwable);
                    consoleView.print(ExceptionUtils.getStackTrace(throwable) + System.lineSeparator(), ConsoleViewContentType.LOG_ERROR_OUTPUT);
                }
            };
        }

        private ConsoleView createConsoleView(final Project project, final String imageName) {
            final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
            final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, "azure.guidance.summary", context);
            ActionManager.getInstance().getAction("ActivateRunToolWindow").actionPerformed(event);
            final ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
            ((ConsoleViewImpl) console).setVisible(true);
            final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Run");
            if (toolWindow == null) {
                return console;
            }
            final String displayName = "Build docker image: " + imageName;
            final Content result = Arrays.stream(toolWindow.getContentManager().getContents())
                    .filter(content -> StringUtils.equalsIgnoreCase(content.getDisplayName(), displayName))
                    .findFirst().orElseGet(() -> {
                        final Content content = ContentFactory.getInstance().createContent(console.getComponent(), displayName, false);
                        toolWindow.getContentManager().addContent(content);
                        return content;
                    });
            result.setDisposer(console);
            result.setComponent(console.getComponent());
            return console;
        }

        private String getDescription() {
            return "Build Docker Image";
        }
    }
}
