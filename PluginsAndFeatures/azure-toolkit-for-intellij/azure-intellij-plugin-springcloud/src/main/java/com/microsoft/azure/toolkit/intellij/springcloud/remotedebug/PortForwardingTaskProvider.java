package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.common.util.remotedebugging.PortForwarder;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

public class PortForwardingTaskProvider extends BeforeRunTaskProvider<PortForwardingTaskProvider.MyBeforeRunTask> {
    private static final String NAME = "Connect to remote";
    private static final Key<MyBeforeRunTask> ID = Key.create("PortForwardingTaskProviderId");
    @Getter
    public Key<MyBeforeRunTask> id = ID;
    @Getter
    public String name = NAME;
    @Getter
    private static final Icon ICON = IntelliJAzureIcons.getIcon(AzureIcons.Action.REMOTE);

    @Override
    public @Nullable
    Icon getTaskIcon(MyBeforeRunTask task) {
        return ICON;
    }

    @Nullable
    @Override
    public MyBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
        return new MyBeforeRunTask(runConfiguration);
    }

    @Override
    public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration, @NotNull ExecutionEnvironment environment, @Nonnull MyBeforeRunTask task) {
        return task.startPortForwarding();
    }

    @Getter
    @Setter
    public static class MyBeforeRunTask extends BeforeRunTask<MyBeforeRunTask> {
        private final RunConfiguration config;
        private String remoteUrl;
        private String accessToken;

        protected MyBeforeRunTask(RunConfiguration config) {
            super(ID);
            this.config = config;
        }

        public boolean startPortForwarding() {
            if (this.config instanceof RemoteConfiguration) {
                final int localPort = Integer.parseInt(((RemoteConfiguration) this.config).PORT);
                PortForwarder.startForward(remoteUrl, accessToken, localPort);
                return true;
            }
            return false;
        }
    }
}
