/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice;

import com.google.common.base.Joiner;
import com.jcraft.jsch.*;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import lombok.CustomLog;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Properties;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

@CustomLog
public class TunnelProxy {
    public static final String DEFAULT_SSH_USERNAME = "root";
    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    // [SuppressMessage("Microsoft.Security", "CS002:SecretInNextLine", Justification="Public credential for app service, refers https://docs.microsoft.com/en-us/azure/app-service/configure-linux-open-ssh-session")]
    public static final String DEFAULT_SSH_PASSWORD = "Docker!";
    private static final String LOCALHOST = "localhost";
    private AppServiceAppBase<?, ?, ?> appService;
    private WebSocketSSLProxy wssProxy;

    public TunnelProxy(@Nonnull AppServiceAppBase<?, ?, ?> webAppBase) {
        this.appService = webAppBase;
        reset();
    }

    public void reset() {
        String host = appService.getHostName().toLowerCase().replace("http://", "").replace("https://", "");
        String[] parts = host.split("\\.", 2);
        host = Joiner.on('.').join(parts[0], "scm", parts[1]);
        PublishingProfile publishingProfile = appService.getPublishingProfile();
        wssProxy = new WebSocketSSLProxy(String.format("wss://%s/AppServiceTunnel/Tunnel.ashx", host),
                                         publishingProfile.getGitUsername(), publishingProfile.getGitPassword());
    }

    public void close() {
        if (Objects.nonNull(wssProxy)) {
            wssProxy.close();
            wssProxy = null;
        }
    }

    public int start() throws IOException {
        if (Objects.isNull(wssProxy)) {
            reset();
        }
        wssProxy.start();
        return wssProxy.getLocalPort();
    }

    public String executeCommandViaSSH(String cmd) throws IOException {
        start();
        String output = executeSSHCommand(cmd, wssProxy.getLocalPort());
        close();
        return output;
    }

    private static String executeSSHCommand(String command, int port) throws IOException {
        try {
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            Session session = jsch.getSession(DEFAULT_SSH_USERNAME, LOCALHOST, port);
            session.setPassword(DEFAULT_SSH_PASSWORD);
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            try (InputStream in = channel.getInputStream()) {
                channel.connect();
                String output = IOUtils.toString(in, Charset.defaultCharset());
                channel.disconnect();
                session.disconnect();
                return output;
            }
        } catch (JSchException e) {
            log.warn(message("appService.tunnel.error.sshFailed", e.getMessage()));
        }
        return null;
    }
}
