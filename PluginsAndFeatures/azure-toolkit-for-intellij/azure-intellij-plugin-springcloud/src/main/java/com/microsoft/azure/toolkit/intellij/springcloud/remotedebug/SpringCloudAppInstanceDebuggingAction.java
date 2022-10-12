/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.execution.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringCloudAppInstanceDebuggingAction {
    private static final int DEFAULT_PORT = 5005;
    private static final String REMOTE_URL_TEMPLATE = "%s?port=%s";
    private static final String FAILED_TO_START_REMOTE_DEBUGGING = "Failed to start remote debugging";
    @AzureOperation(name = "springcloud.attach_debugger.instance", params = {"appInstance.getName()"}, type = AzureOperation.Type.ACTION)
    public static void startDebugging(@Nonnull SpringCloudAppInstance appInstance, Project project) {
        if (!appInstance.getParent().getParent().isRemoteDebuggingEnabled()) {
            final Action<SpringCloudApp> enableDebuggingAction = AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.ENABLE_REMOTE_DEBUGGING);
            AzureMessager.getMessager().warning("Failed to attach debugger because remote debugging is not enabled.", FAILED_TO_START_REMOTE_DEBUGGING, new Action<>(Action.Id.of("springcloud.enable_remote_debugging"), new ActionView.Builder("Enable Remote Debugging")) {
                @Override
                public void handle(Object source, Object e) {
                    enableDebuggingAction.handle(appInstance.getParent().getParent(), e);
                }
            });
            return;
        }
        final RemoteConfiguration remoteConfiguration = generateRemoteConfiguration(project, appInstance);
        final RunManagerImpl managerImpl = new RunManagerImpl(project);
        final RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(managerImpl, remoteConfiguration, false);
        final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        addExecutionListener(project);
        settings.storeInLocalWorkspace();
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance());
    }

    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }

    private static void addExecutionListener(Project project) {
        final MessageBusConnection messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
                Optional.ofNullable(env.getRunnerAndConfigurationSettings()).ifPresent(settings -> {
                    if (settings.getConfiguration() instanceof RemoteConfiguration) {
                        final List<BeforeRunTask<?>> beforeRunTaskList = settings.getConfiguration().getBeforeRunTasks();
                        beforeRunTaskList.forEach(beforeRunTask -> {
                            if (beforeRunTask instanceof PortForwardingTaskProvider.PortForwarderBeforeRunTask) {
                                ((PortForwardingTaskProvider.PortForwarderBeforeRunTask) beforeRunTask).getForwarder().stopForward();
                            }
                        });
                    }
                });
                messageBusConnection.disconnect();
            }
        });
    }

    private static RemoteConfiguration generateRemoteConfiguration(Project project, SpringCloudAppInstance appInstance) {
        final RemoteConfiguration remoteConfig = (RemoteConfiguration) RemoteConfigurationType.getInstance().createTemplateConfiguration(project);
        remoteConfig.PORT = String.valueOf(DEFAULT_PORT);
        remoteConfig.HOST = "localhost";
        remoteConfig.USE_SOCKET_TRANSPORT = true;
        remoteConfig.SERVER_MODE = false;
        remoteConfig.setName(String.format("remote-debug-%s", appInstance.getName()));
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length > 0) {
            remoteConfig.setModule(modules[0]);
        }
        final List<BeforeRunTask<?>> beforeRunTasks = new ArrayList<>();
        beforeRunTasks.add(createPortForwardingTask(remoteConfig, appInstance));
        beforeRunTasks.addAll(remoteConfig.getBeforeRunTasks());
        remoteConfig.setBeforeRunTasks(beforeRunTasks);
        return remoteConfig;
    }

    private static PortForwardingTaskProvider.PortForwarderBeforeRunTask createPortForwardingTask(RemoteConfiguration runConfiguration, SpringCloudAppInstance appInstance) {
        final PortForwardingTaskProvider provider = new PortForwardingTaskProvider();
        final PortForwardingTaskProvider.PortForwarderBeforeRunTask runTask = provider.createTask(runConfiguration);
        Optional.ofNullable(runTask).ifPresent(task -> task.setAppInstance(appInstance));
        return runTask;
    }
}
