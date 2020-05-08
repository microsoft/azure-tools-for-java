/**
 * Copyright (c) 2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.mock

import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.*
import com.microsoft.azure.management.sql.implementation.ServerInner
import com.microsoft.azure.management.sql.implementation.SqlServerManager
import rx.Observable

class SqlServerMock(
        private val id: String = "test-sql-server-id",
        private val name: String = "test-sql-server"
) : SqlServer {

    override fun databases(): SqlDatabaseOperations.SqlDatabaseActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun inner(): ServerInner {
        TODO("Not yet implemented")
    }

    override fun removeAccessFromAzureServices() {
        TODO("Not yet implemented")
    }

    override fun version(): String {
        TODO("Not yet implemented")
    }

    override fun systemAssignedManagedServiceIdentityPrincipalId(): String {
        TODO("Not yet implemented")
    }

    override fun fullyQualifiedDomainName(): String {
        TODO("Not yet implemented")
    }

    override fun tags(): MutableMap<String, String> {
        TODO("Not yet implemented")
    }

    override fun refresh(): SqlServer {
        TODO("Not yet implemented")
    }

    override fun type(): String {
        TODO("Not yet implemented")
    }

    override fun removeActiveDirectoryAdministrator() {
        TODO("Not yet implemented")
    }

    override fun enableAccessFromAzureServices(): SqlFirewallRule {
        TODO("Not yet implemented")
    }

    override fun virtualNetworkRules(): SqlVirtualNetworkRuleOperations.SqlVirtualNetworkRuleActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun serverKeys(): SqlServerKeyOperations.SqlServerKeyActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun dnsAliases(): SqlServerDnsAliasOperations.SqlServerDnsAliasActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun listServiceObjectives(): MutableList<ServiceObjective> {
        TODO("Not yet implemented")
    }

    override fun manager(): SqlServerManager {
        TODO("Not yet implemented")
    }

    override fun managedServiceIdentityType(): IdentityType {
        TODO("Not yet implemented")
    }

    override fun region(): Region {
        TODO("Not yet implemented")
    }

    override fun listRestorableDroppedDatabasesAsync(): Observable<SqlRestorableDroppedDatabase> {
        TODO("Not yet implemented")
    }

    override fun key(): String {
        TODO("Not yet implemented")
    }

    override fun listRecommendedElasticPools(): MutableMap<String, RecommendedElasticPool> {
        TODO("Not yet implemented")
    }

    override fun firewallRules(): SqlFirewallRuleOperations.SqlFirewallRuleActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun elasticPools(): SqlElasticPoolOperations.SqlElasticPoolActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun encryptionProtectors(): SqlEncryptionProtectorOperations.SqlEncryptionProtectorActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun state(): String {
        TODO("Not yet implemented")
    }

    override fun id(): String = id

    override fun administratorLogin(): String {
        TODO("Not yet implemented")
    }

    override fun listUsages(): MutableList<ServerMetric> {
        TODO("Not yet implemented")
    }

    override fun getServerAutomaticTuning(): SqlServerAutomaticTuning {
        TODO("Not yet implemented")
    }

    override fun serverSecurityAlertPolicies(): SqlServerSecurityAlertPolicyOperations.SqlServerSecurityAlertPolicyActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun getActiveDirectoryAdministrator(): SqlActiveDirectoryAdministrator {
        TODO("Not yet implemented")
    }

    override fun update(): SqlServer.Update {
        TODO("Not yet implemented")
    }

    override fun refreshAsync(): Observable<SqlServer> {
        TODO("Not yet implemented")
    }

    override fun resourceGroupName(): String {
        TODO("Not yet implemented")
    }

    override fun getServiceObjective(p0: String?): ServiceObjective {
        TODO("Not yet implemented")
    }

    override fun systemAssignedManagedServiceIdentityTenantId(): String {
        TODO("Not yet implemented")
    }

    override fun listRestorableDroppedDatabases(): MutableList<SqlRestorableDroppedDatabase> {
        TODO("Not yet implemented")
    }

    override fun listUsageMetrics(): MutableList<ServerMetric> {
        TODO("Not yet implemented")
    }

    override fun isManagedServiceIdentityEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun name(): String = name

    override fun setActiveDirectoryAdministrator(p0: String?, p1: String?): SqlActiveDirectoryAdministrator {
        TODO("Not yet implemented")
    }

    override fun regionName(): String {
        TODO("Not yet implemented")
    }

    override fun kind(): String {
        TODO("Not yet implemented")
    }

    override fun failoverGroups(): SqlFailoverGroupOperations.SqlFailoverGroupActionsDefinition {
        TODO("Not yet implemented")
    }
}
