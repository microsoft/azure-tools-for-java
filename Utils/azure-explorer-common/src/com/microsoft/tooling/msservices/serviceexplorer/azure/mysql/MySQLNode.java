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
package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azure.management.mysql.v2017_12_01.Server;
import com.microsoft.azure.management.mysql.v2017_12_01.implementation.FirewallRuleInner;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ENABLE_MY_PUBLIC_IP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.MYSQL;
import static com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule.ICON_FILE;


public class MySQLNode extends Node implements TelemetryProperties {
    private static final InetAddressValidator ipValidator = InetAddressValidator.getInstance();
    private static final String ACTION_ADD_MY_IP = "Add My IP";
    private final String subscriptionId;
    private final Server server;

    public MySQLNode(AzureRefreshableNode parent, String subscriptionId, Server mysqlServer) {
        super(mysqlServer.id(), mysqlServer.name(), parent, ICON_FILE, true);
        this.subscriptionId = subscriptionId;
        this.server = mysqlServer;
        loadActions();
    }

    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        // todo: track region name
        return properties;
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_ADD_MY_IP, new WrappedTelemetryNodeActionListener(MYSQL, ENABLE_MY_PUBLIC_IP,
                                                                           createBackgroundActionListener("Adding local machine IP to allow list",
                                                                               () -> enableAccessFromMyMachine())));
    }

    private void enableAccessFromMyMachine() {
        String myIp = getIpAddress();
        if (StringUtils.isBlank(myIp)) {
            DefaultLoader.getUIHelper().showErrorNotification("Cannot get public ip",
                    "Cannot get public ip address, please make sure you are online and can access http://whatismyip.akamai.com/");
        }
        AtomicBoolean alreadyDone = new AtomicBoolean(false);
        server.manager().firewallRules().listByServerAsync(server.resourceGroupName(), server.name()).toBlocking().forEach(t -> {
            if (StringUtils.equalsIgnoreCase(t.startIpAddress(), myIp)) {
                alreadyDone.set(true);
            }
        });
        if (alreadyDone.get()) {
            DefaultLoader.getUIHelper().showInfoNotification("Public IP is already added.",
                    String.format("Your public IP(%s) is already listed in the firewall rules.", myIp));
            return;
        }
        FirewallRuleInner rule = new FirewallRuleInner();
        rule.withStartIpAddress(myIp);
        rule.withEndIpAddress(myIp);
        FirewallRuleInner result = server.manager().firewallRules().inner()
                .createOrUpdate(server.resourceGroupName(), server.name(), "allow-" + myIp.replaceAll("\\.", "-"), rule);

        DefaultLoader.getUIHelper().showInfoNotification("Public IP added.",
                String.format("Your public IP(%s) is added in the firewall rules with name(%s).", myIp, result.name()));
    }

    private static String getIpAddress() {
        try {
            URL myIP = new URL("http://whatismyip.akamai.com/");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(myIP.openStream()))) {
                final String line = in.readLine();
                if (ipValidator.isValid(line)) {
                    return line;
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return "";
    }
}
