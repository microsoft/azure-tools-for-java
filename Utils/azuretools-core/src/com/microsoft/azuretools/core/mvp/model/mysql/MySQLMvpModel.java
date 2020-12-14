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

package com.microsoft.azuretools.core.mvp.model.mysql;

import com.google.common.base.Preconditions;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerPropertiesForDefaultCreate;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerState;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerUpdateParameters;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerVersion;
import com.microsoft.azure.management.mysql.v2020_01_01.SslEnforcementEnum;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.DatabaseInner;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.FirewallRuleInner;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.MySQLManager;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import lombok.Lombok;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MySQLMvpModel {

    private static final String NAME_ALLOW_ACCESS_TO_AZURE_SERVICES = "AllowAllWindowsAzureIps";
    private static final String IP_ALLOW_ACCESS_TO_AZURE_SERVICES = "0.0.0.0";
    private static final String NAME_PREFIX_ALLOW_ACCESS_TO_LOCAL = "ClientIPAddress_";
    private static final List<String> MYSQL_SUPPORTED_REGIONS = Arrays.asList(
            "australiacentral", "australiacentral2", "australiaeast", "australiasoutheast", "brazilsouth", "canadacentral", "canadaeast", "centralindia",
            "centralus", "eastasia", "eastus2", "eastus", "francecentral", "francesouth", "germanywestcentral", "japaneast", "japanwest", "koreacentral",
            "koreasouth", "northcentralus", "northeurope", "southafricanorth", "southafricawest", "southcentralus", "southindia", "southeastasia",
            "norwayeast", "switzerlandnorth", "uaenorth", "uksouth", "ukwest", "westcentralus", "westeurope", "westindia", "westus", "westus2",
            "centraluseuap", "eastus2euap");

    public static List<Server> listMySQLServers() {
        final List<Server> servers = new ArrayList<>();
        final List<Subscription> subscriptions = AzureMvpModel.getInstance().getSelectedSubscriptions();
        if (CollectionUtils.isEmpty(subscriptions)) {
            return servers;
        }
        subscriptions.parallelStream().forEach(subscription -> {
            try {
                List<Server> subServers = MySQLMvpModel.listMySQLServersBySubscriptionId(subscription.subscriptionId());
                synchronized (servers) {
                    servers.addAll(subServers);
                }
            } catch (IOException e) {
                // swallow exception and skip error subscription
            }
        });
        return servers;
    }

    public static Server findServer(final String subscriptionId, final String resourceGroup, final String name) {
        final MySQLManager manager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
        Server result = manager.servers().getByResourceGroup(resourceGroup, name);
        return result;
    }

    public static Server create(final String subscriptionId, final String resourceGroupName, final String serverName,
                                Region region, final ServerPropertiesForDefaultCreate properties) {
        final MySQLManager manager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
        /*Sku sku = new Sku().withName("GP_Gen5_4");*/
        Server result = manager.servers().define(serverName).withRegion(region.name()).withExistingResourceGroup(resourceGroupName)
                .withProperties(properties)/*.withSku(sku)*/.create();
        return result;
    }

    public static void delete(final String subscriptionId, final String id) {
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
        mySQLManager.servers().deleteByIds(id);
    }

    public static void start(final String subscriptionId, final Server server) {
        Server currentServer = MySQLMvpModel.findServer(subscriptionId, server.resourceGroupName(), server.name());
        Preconditions.checkArgument(
                ServerState.fromString("Stopped").equals(currentServer.userVisibleState()) || ServerState.DISABLED.equals(currentServer.userVisibleState()),
                "Start action is not supported for non-disabled server.");
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
        mySQLManager.servers().inner().start(server.resourceGroupName(), server.name());
    }

    public static void restart(final String subscriptionId, final Server server) {
        Server currentServer = MySQLMvpModel.findServer(subscriptionId, server.resourceGroupName(), server.name());
        Preconditions.checkArgument(ServerState.READY.equals(currentServer.userVisibleState()), "Restart action is not supported for non-ready server.");
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
        mySQLManager.servers().inner().restart(server.resourceGroupName(), server.name());
    }

    public static void stop(final String subscriptionId, final Server server) {
        Server currentServer = MySQLMvpModel.findServer(subscriptionId, server.resourceGroupName(), server.name());
        Preconditions.checkArgument(ServerState.READY.equals(currentServer.userVisibleState()), "Stop action is not supported for non-ready server.");
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
        mySQLManager.servers().inner().stop(server.resourceGroupName(), server.name());
    }

    public static List<String> listSupportedVersions() {
        List<String> resultList = new ArrayList();
        resultList.add(ServerVersion.FIVE_FULL_STOP_SIX.toString());
        resultList.add(ServerVersion.FIVE_FULL_STOP_SEVEN.toString());
        resultList.add(ServerVersion.EIGHT_FULL_STOP_ZERO.toString());
        return resultList;
    }

    public static List<Region> listSupportedRegions() {
        // TODO (qianijn): remove join logic
        return Arrays.asList(com.microsoft.azure.arm.resources.Region.values()).stream()
                .filter(e -> MYSQL_SUPPORTED_REGIONS.contains(e.name()))
                .map(e -> Region.findByLabelOrName(e.name()))
                .sorted(Comparator.comparing(Region::label))
                .collect(Collectors.toList());
    }

    public static boolean updateSSLEnforcement(final String subscriptionId, final Server server, final SslEnforcementEnum sslEnforcement) {
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
        final ServerUpdateParameters parameters = new ServerUpdateParameters();
        parameters.withSslEnforcement(sslEnforcement);
        mySQLManager.servers().inner().update(server.resourceGroupName(), server.name(), parameters);
        return true;
    }

    public static class DatabaseMvpModel {

        public static List<DatabaseInner> listDatabases(final String subscriptionId, final Server server) {
            final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
            final List<DatabaseInner> databases = mySQLManager.databases().inner().listByServer(server.resourceGroupName(), server.name());
            return databases;
        }
    }

    public static class FirewallRuleMvpModel {

        public static List<FirewallRuleInner> listFirewallRules(final String subscriptionId, final Server server) {
            final MySQLManager manager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
            final List<FirewallRuleInner> firewallRules = manager.firewallRules().inner().listByServer(server.resourceGroupName(), server.name());
            return firewallRules;
        }

        public static List<FirewallRuleInner> listFirewallRules(final String subscriptionId, final String resourceGroupName, final String name) {
            final MySQLManager manager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
            final List<FirewallRuleInner> firewallRules = manager.firewallRules().inner().listByServer(resourceGroupName, name);
            return firewallRules;
        }

        public static boolean isAllowAccessFromAzureServices(final String subscriptionId, final Server server) {
            List<FirewallRuleInner> firewallRules = MySQLMvpModel.FirewallRuleMvpModel.listFirewallRules(subscriptionId, server);
            return MySQLMvpModel.FirewallRuleMvpModel.isAllowAccessFromAzureServices(firewallRules);
        }

        public static boolean isAllowAccessFromAzureServices(final List<FirewallRuleInner> firewallRules) {
            return firewallRules.stream().filter(e -> StringUtils.equals(NAME_ALLOW_ACCESS_TO_AZURE_SERVICES, e.name())).count() > 0;
        }

        public static boolean updateAllowAccessFromAzureServices(final String subscriptionId, final Server server, final boolean enable) {
            if (enable) {
                return MySQLMvpModel.FirewallRuleMvpModel.enableAllowAccessFromAzureServices(subscriptionId, server);
            } else {
                return MySQLMvpModel.FirewallRuleMvpModel.disableAllowAccessFromAzureServices(subscriptionId, server);
            }
        }

        public static boolean enableAllowAccessFromAzureServices(final String subscriptionId, final Server server) {
            if (MySQLMvpModel.FirewallRuleMvpModel.isAllowAccessFromAzureServices(subscriptionId, server)) {
                return true;
            }
            final String ruleName = NAME_ALLOW_ACCESS_TO_AZURE_SERVICES;
            final FirewallRuleInner firewallRule = new FirewallRuleInner();
            firewallRule.withStartIpAddress(IP_ALLOW_ACCESS_TO_AZURE_SERVICES);
            firewallRule.withEndIpAddress(IP_ALLOW_ACCESS_TO_AZURE_SERVICES);
            final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
            mySQLManager.firewallRules().inner().createOrUpdate(server.resourceGroupName(), server.name(), ruleName, firewallRule);
            return true;
        }

        public static boolean disableAllowAccessFromAzureServices(final String subscriptionId, final Server server) {
            if (!MySQLMvpModel.FirewallRuleMvpModel.isAllowAccessFromAzureServices(subscriptionId, server)) {
                return true;
            }
            final String ruleName = NAME_ALLOW_ACCESS_TO_AZURE_SERVICES;
            final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
            mySQLManager.firewallRules().inner().delete(server.resourceGroupName(), server.name(), ruleName);
            return true;
        }

        public static boolean isAllowAccessFromLocalMachine(final String subscriptionId, final Server server) {
            final List<FirewallRuleInner> firewallRules = MySQLMvpModel.FirewallRuleMvpModel.listFirewallRules(subscriptionId, server);
            return MySQLMvpModel.FirewallRuleMvpModel.isAllowAccessFromLocalMachine(firewallRules);
        }

        public static boolean isAllowAccessFromLocalMachine(final List<FirewallRuleInner> firewallRules) {
            final List<FirewallRuleInner> localFirewallRules = MySQLMvpModel.FirewallRuleMvpModel.getLocalFirewallRules(firewallRules);
            return CollectionUtils.isNotEmpty(localFirewallRules);
        }

        public static boolean updateAllowAccessToLocalMachine(final String subscriptionId, final Server server, final boolean enable) {
            if (enable) {
                return MySQLMvpModel.FirewallRuleMvpModel.enableAllowAccessFromLocalMachine(subscriptionId, server);
            } else {
                return MySQLMvpModel.FirewallRuleMvpModel.disableAllowAccessFromLocalMachine(subscriptionId, server);
            }
        }

        public static boolean enableAllowAccessFromLocalMachine(final String subscriptionId, final Server server) {
            if (MySQLMvpModel.FirewallRuleMvpModel.isAllowAccessFromLocalMachine(subscriptionId, server)) {
                return true;
            }
            try {
                final String publicIp = MySQLMvpModel.FirewallRuleMvpModel.getPublicIp();
                final String ruleName = NAME_PREFIX_ALLOW_ACCESS_TO_LOCAL + publicIp.replaceAll("\\.", "-");
                final FirewallRuleInner firewallRule = new FirewallRuleInner();
                firewallRule.withStartIpAddress(publicIp);
                firewallRule.withEndIpAddress(publicIp);
                final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
                mySQLManager.firewallRules().inner().createOrUpdate(server.resourceGroupName(), server.name(), ruleName, firewallRule);
            } catch (IOException e) {
                Lombok.sneakyThrow(e);
            }
            return true;
        }

        public static boolean disableAllowAccessFromLocalMachine(final String subscriptionId, final Server server) {
            if (!MySQLMvpModel.FirewallRuleMvpModel.isAllowAccessFromLocalMachine(subscriptionId, server)) {
                return true;
            }
            final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLManager(subscriptionId);
            final List<FirewallRuleInner> firewallRules = MySQLMvpModel.FirewallRuleMvpModel.listFirewallRules(subscriptionId, server);
            final List<FirewallRuleInner> localFirewallRules = MySQLMvpModel.FirewallRuleMvpModel.getLocalFirewallRules(firewallRules);
            localFirewallRules.stream().forEach(e -> {
                mySQLManager.firewallRules().inner().delete(server.resourceGroupName(), server.name(), e.name());
            });
            return true;
        }

        private static List<FirewallRuleInner> getLocalFirewallRules(final List<FirewallRuleInner> firewallRules) {
            try {
                final String publicIp = getPublicIp();
                if (StringUtils.isBlank(publicIp)) {
                    return new ArrayList<>();
                }
                return firewallRules.stream().filter(e -> StringUtils.equals(publicIp, e.startIpAddress()) && StringUtils.equals(publicIp, e.endIpAddress()))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                Lombok.sneakyThrow(e);
            }
            return new ArrayList<>();
        }

        private static String getPublicIp() throws IOException {
            final URL url = new URL("http://whatismyip.akamai.com");
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            String ip;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
                while ((ip = in.readLine()) != null) {
                    if (StringUtils.isNotBlank(ip)) {
                        break;
                    }
                }
            }
            return ip;
        }
    }

    private static List<Server> listMySQLServersBySubscriptionId(final String subscriptionId) throws IOException {
        return getMySQLManager(subscriptionId).servers().list();
    }

    private static MySQLManager getMySQLManager(String sid) throws IOException {
        return AuthMethodManager.getInstance().getMySQLManager(sid);
    }
}
