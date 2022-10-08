package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.intellij.execution.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.util.remotedebugging.PortForwarder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentInstanceEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class AppInstanceDebuggingAction {
    private static final int DEFAULT_PORT = 5005;
    private static final String REMOTE_URL_TEMPLATE = "%s?port=%s";
    private static final String DIALOG_TITLE = "Remote Debug";
    private static PortForwarder portForwarder;
    public static void startDebugging(@Nonnull SpringCloudDeploymentInstanceEntity appInstance, @Nullable Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final RemoteConfiguration remoteConfiguration = generateRemoteConfiguration(project, appInstance);
            final RunManagerImpl runManager = new RunManagerImpl(project);
            final RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(runManager, remoteConfiguration, false);
            if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
                System.out.println("editConfiguration success");
                runManager.addConfiguration(settings);
                runManager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }

    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }

    private static RemoteConfiguration generateRemoteConfiguration(Project project, SpringCloudDeploymentInstanceEntity appInstance) {
        final RemoteConfiguration remoteConfig = (RemoteConfiguration) RemoteConfigurationType.getInstance().createTemplateConfiguration(project);
        remoteConfig.PORT = String.valueOf(DEFAULT_PORT);
        remoteConfig.HOST = "localhost";
        remoteConfig.USE_SOCKET_TRANSPORT = true;
        remoteConfig.SERVER_MODE = false;
        remoteConfig.setName("spring remote debug");
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

    private static PortForwardingTaskProvider.MyBeforeRunTask createPortForwardingTask(RemoteConfiguration runConfiguration, SpringCloudDeploymentInstanceEntity appInstance) {
        final PortForwardingTaskProvider provider = new PortForwardingTaskProvider();
        final PortForwardingTaskProvider.MyBeforeRunTask runTask = provider.createTask(runConfiguration);
        final Account account = az(AzureAccount.class).account();
        final String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = new TokenRequestContext().addScopes(scopes);
        final String accessToken = account.getTokenCredential(appInstance.getDeployment().getSubscriptionId()).getToken(request).block().getToken();
        runTask.setRemoteUrl(String.format(REMOTE_URL_TEMPLATE, appInstance.getRemoteUrl(), runConfiguration.PORT));
        runTask.setAccessToken(accessToken);
        return runTask;
    }
}
