package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
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
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentInstanceEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class AppInstanceDebuggingAction {
    private static final int DEFAULT_PORT = 5005;
    private static final String REMOTE_URL_TEMPLATE = "%s?port=%s";
    private static final String DIALOG_TITLE = "Remote Debug";
    public static void startDebugging(@Nonnull SpringCloudDeploymentInstanceEntity appInstance, Project project) {
        addExecutionListener(project);
        final RemoteConfiguration remoteConfiguration = generateRemoteConfiguration(project, appInstance);
        final RunManagerImpl managerImpl = new RunManagerImpl(project);
        final RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(managerImpl, remoteConfiguration, false);
        final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        settings.storeInLocalWorkspace();
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance());
    }

    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }

    private static void addExecutionListener(Project project) {
        project.getMessageBus().connect().subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
                ExecutionListener.super.processTerminated(executorId, env, handler, exitCode);
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
            }
        });
    }

    private static RemoteConfiguration generateRemoteConfiguration(Project project, SpringCloudDeploymentInstanceEntity appInstance) {
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

    private static PortForwardingTaskProvider.PortForwarderBeforeRunTask createPortForwardingTask(RemoteConfiguration runConfiguration, SpringCloudDeploymentInstanceEntity appInstance) {
        final PortForwardingTaskProvider provider = new PortForwardingTaskProvider();
        final PortForwardingTaskProvider.PortForwarderBeforeRunTask runTask = provider.createTask(runConfiguration);
        final Account account = az(AzureAccount.class).account();
        final String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = new TokenRequestContext().addScopes(scopes);
        final String accessToken = account.getTokenCredential(appInstance.getDeployment().getSubscriptionId()).getToken(request).block().getToken();
        runTask.setRemoteUrl(String.format(REMOTE_URL_TEMPLATE, appInstance.getRemoteUrl(), runConfiguration.PORT));
        runTask.setAccessToken(accessToken);
        runTask.setTaskDescription(String.format("Connect to %s", appInstance.getName()));
        return runTask;
    }
}
