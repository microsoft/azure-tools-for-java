/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud.portforwarder;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.azure.toolkit.ide.common.portforwarder.AbstractPortForwarder;
import com.microsoft.azure.toolkit.ide.common.portforwarder.PortForwarderWebSocketListener;
import com.microsoft.azure.toolkit.ide.common.portforwarder.WebSocketBuilder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import okhttp3.OkHttpClient;

import javax.annotation.Nonnull;
import java.nio.channels.SocketChannel;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SpringPortForwarder extends AbstractPortForwarder {
    private static final String REMOTE_URL_TEMPLATE = "%s?port=%s";

    private final SpringCloudAppInstance appInstance;
    public SpringPortForwarder(@Nonnull final SpringCloudAppInstance appInstance) {
        super();
        this.appInstance = appInstance;
    }

    @Override
    protected WebSocketBuilder createSocketBuilder(final OkHttpClient httpClient) {
        final Account account = az(AzureAccount.class).account();
        final String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = new TokenRequestContext().addScopes(scopes);
        final String accessToken = account.getTokenCredential(appInstance.getSubscriptionId()).getToken(request).block().getToken();
        final String url = String.format(REMOTE_URL_TEMPLATE, appInstance.getRemoteUrl(), appInstance.getParent().getParent().getRemoteDebuggingPort());
        return new WebSocketBuilder(httpClient).uri(url).header("Authorization", String.format("Bearer %s", accessToken));
    }

    @Override
    protected PortForwarderWebSocketListener createWebSocketListener(SocketChannel socketChannel) {
        return new SpringPortForwarderWebSocketListener(socketChannel, socketChannel, this);
    }
}
