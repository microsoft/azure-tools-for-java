/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.springcloud.portforwarder.PortForwarder;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

import java.util.Objects;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class PortForwardingTaskProvider extends BeforeRunTaskProvider<PortForwardingTaskProvider.PortForwarderBeforeRunTask> {
    private static final String NAME_TEMPLATE = "Attach to %s";
    private static final Key<PortForwarderBeforeRunTask> ID = Key.create("PortForwardingTaskProviderId");
    private static final Icon ICON = IntelliJAzureIcons.getIcon(AzureIcons.Action.ATTACH);
    @Getter
    public Key<PortForwarderBeforeRunTask> id = ID;
    @Getter
    public String name = String.format(NAME_TEMPLATE, "spring app instance");
    @Getter
    public Icon icon = ICON;

    @Override
    public @Nullable
    Icon getTaskIcon(PortForwarderBeforeRunTask task) {
        return ICON;
    }

    @Nullable
    @Override
    public PortForwarderBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
        return new PortForwarderBeforeRunTask(runConfiguration);
    }

    @Override
    public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration, @NotNull ExecutionEnvironment environment, @Nonnull PortForwarderBeforeRunTask task) {
        if (configuration instanceof RemoteConfiguration) {
            return task.startPortForwarding(Integer.parseInt(((RemoteConfiguration) configuration).PORT));
        }
        return false;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription(PortForwarderBeforeRunTask task) {
        return Objects.isNull(task.appInstance) ? name : String.format(NAME_TEMPLATE, task.appInstance.getName());
    }

    @Getter
    @Setter
    public static class PortForwarderBeforeRunTask extends BeforeRunTask<PortForwarderBeforeRunTask> {
        private static final String REMOTE_URL_TEMPLATE = "%s?port=%s";
        private final RunConfiguration config;
        private String remoteUrl;
        private String accessToken;
        private PortForwarder forwarder;
        private SpringCloudAppInstance appInstance;

        protected PortForwarderBeforeRunTask(RunConfiguration config) {
            super(ID);
            this.config = config;
        }

        public boolean startPortForwarding(int localPort) {
            if (this.config instanceof RemoteConfiguration) {
                this.forwarder = new PortForwarder();
                AzureTaskManager.getInstance().runOnPooledThread(() ->  this.forwarder.startForward(remoteUrl, accessToken, localPort));
                return true;
            }
            return false;
        }

        public void setAppInstance(SpringCloudAppInstance appInstance) {
            this.appInstance = appInstance;
            this.remoteUrl = String.format(REMOTE_URL_TEMPLATE, appInstance.getRemoteDebuggingUrl(), SpringCloudAppInstanceDebuggingAction.getDefaultPort());
            final Account account = az(AzureAccount.class).account();
            final String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
            final TokenRequestContext request = new TokenRequestContext().addScopes(scopes);
            this.accessToken = account.getTokenCredential(appInstance.getSubscriptionId()).getToken(request).block().getToken();
        }

    }
}
