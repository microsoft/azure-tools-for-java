/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.FirewallRuleInner;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MySQLProperty {

    private Server server;
    private String subscriptionId;
    private List<FirewallRuleInner> firewallRules;

}
