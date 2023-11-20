/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @see "org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider"
 */
@Slf4j
public class DotEnvBeforeRunTaskProvider extends BeforeRunTaskProvider<DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask> {
    private static final String NAME = "Load .env";
    private static final String DESCRIPTION = "Load .env";
    private static final Icon ICON = AllIcons.General.Gear;
    private static final Key<LoadDotEnvBeforeRunTask> ID = Key.create("ConnectionRunnerForConfigurationId");

    @Getter
    public String name = NAME;
    @Getter
    private final boolean configurable = true;
    @Getter
    public Key<LoadDotEnvBeforeRunTask> id = ID;
    @Getter
    public Icon icon = ICON;

    @Override
    public @Nullable
    Icon getTaskIcon(LoadDotEnvBeforeRunTask task) {
        return ICON;
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/connector.get_before_task_description")
    public String getDescription(LoadDotEnvBeforeRunTask task) {
        return Optional.ofNullable(task.getConfig().getProject()).map(ProjectUtil::guessProjectDir)
            .flatMap(v -> Optional.ofNullable(task.getFile()).map(f -> v.toNioPath().relativize(f.toNioPath())))
            .map(path -> String.format("Load '%s'", path.toString().replaceAll("\\\\", "/")))
            .orElse("Load .env");
    }

    @Nullable
    @Override
    public LoadDotEnvBeforeRunTask createTask(@Nonnull RunConfiguration config) {
        return new LoadDotEnvBeforeRunTask(config);
    }

    @Override
    @AzureOperation(name = "user/connector.config_before_task")
    public Promise<Boolean> configureTask(@Nonnull DataContext context, @Nonnull RunConfiguration configuration, @Nonnull LoadDotEnvBeforeRunTask task) {
        final AsyncPromise<Boolean> result = new AsyncPromise<>();
        AzureTaskManager.getInstance().runLater(() -> {
            final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("env");
            descriptor.withTitle("Choose .env File");
            final VirtualFile defaultDir = AzureModule.createIfSupport(configuration)
                .flatMap(AzureModule::getModuleDir).orElse(null);
            final VirtualFile file = FileChooser.chooseFile(descriptor, configuration.getProject(), defaultDir);
            if (Objects.nonNull(file)) {
                task.setFile(file);
                result.setResult(true);
            } else {
                result.setResult(false);
            }
        });
        return result;
    }

    @Override
    public boolean canExecuteTask(@Nonnull RunConfiguration configuration, @Nonnull LoadDotEnvBeforeRunTask task) {
        return Objects.nonNull(task.getFile());
    }

    @Override
    @ExceptionNotification
    public boolean executeTask(@Nonnull DataContext context, @Nonnull RunConfiguration config, @Nonnull ExecutionEnvironment env, @Nonnull LoadDotEnvBeforeRunTask task) {
        return true;
    }

    @Getter
    @Setter
    public static class LoadDotEnvBeforeRunTask extends BeforeRunTask<LoadDotEnvBeforeRunTask> implements PersistentStateComponent<LoadDotEnvBeforeRunTask.State> {
        private final RunConfiguration config;
        @Nullable
        private VirtualFile file;

        public LoadDotEnvBeforeRunTask(RunConfiguration configuration) {
            super(ID);
            this.config = configuration;
            this.file = AzureModule.createIfSupport(this.config).map(AzureModule::getDefaultProfile).map(Profile::getDotEnvFile).orElse(null);
        }

        @AzureOperation("platform/connector.load_env_beforeruntask")
        public List<Pair<String, String>> loadEnv() {
            final AzureModule azureModule = AzureModule.createIfSupport(this.config).orElse(null);
            final Project project = Optional.ofNullable(azureModule).map(AzureModule::getProject).orElse(null);
            final List<Pair<String, String>> exists = Optional.ofNullable(this.file)
                    .or(() -> AzureModule.createIfSupport(this.config).map(AzureModule::getDefaultProfile).map(Profile::getDotEnvFile))
                    .map(Profile::load)
                    .orElse(Collections.emptyList());
            final List<Pair<String, String>> result = new ArrayList<>(exists);
            Optional.ofNullable(azureModule).map(AzureModule::getDefaultProfile)
                    .map(Profile::getConnections)
                    .stream().flatMap(List::stream)
                    .filter(c -> !c.isValidConnection())
                    .map(c -> ResourceConnectionActionsContributor.fixResourceConnection(c, () -> project))
                    .filter(Objects::nonNull)
                    .forEach(connection -> connection.getEnvironmentVariables(project)
                            .forEach((key, value) -> result.add(Pair.of(key, value))));
            return result;
        }

        @Override
        public @Nullable LoadDotEnvBeforeRunTask.State getState() {
            return Optional.ofNullable(this.file).map(f -> f.toNioPath().toString()).map(State::new).orElse(null);
        }

        @Override
        public void loadState(@Nonnull LoadDotEnvBeforeRunTask.State state) {
            this.file = Optional.ofNullable(state.getEnvFile())
                .filter(StringUtils::isNotBlank).map(Path::of)
                .map(p -> VfsUtil.findFile(p, true))
                .orElse(null);
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        static class State {
            private String envFile;
        }
    }
}
