/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.google.common.base.Joiner;
import com.jcraft.jsch.*;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.utils.WebSocketSSLProxy;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class TunnelProxy {
    public static final String DEFAULT_SSH_USERNAME = "root";
    public static final String DEFAULT_SSH_PASSWORD = "Docker!";
    private static final Logger logger = Logger.getLogger(TunnelProxy.class.getName());
    private static final String LOCALHOST = "localhost";
    private WebAppBase webAppBase;
    private WebSocketSSLProxy wssProxy;

    public TunnelProxy(@NotNull WebAppBase webAppBase) {
        this.webAppBase = webAppBase;
        reset();
    }

    public void reset() {
        String host = webAppBase.defaultHostName().toLowerCase().replace("http://", "").replace("https://", "");
        String[] parts = host.split("\\.", 2);
        host = Joiner.on('.').join(parts[0], "scm", parts[1]);
        PublishingProfile publishingProfile = webAppBase.getPublishingProfile();
        wssProxy = new WebSocketSSLProxy(String.format("wss://%s/AppServiceTunnel/Tunnel.ashx", host),
                                         publishingProfile.gitUsername(), publishingProfile.gitPassword());
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
            logger.warning(String.format("Encounter error while ssh into azure app service: %s ", e.getMessage()));
        }
        return null;
    }
}
