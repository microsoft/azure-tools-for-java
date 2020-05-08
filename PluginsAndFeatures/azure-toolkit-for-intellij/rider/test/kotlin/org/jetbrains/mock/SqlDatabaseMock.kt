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
import com.microsoft.azure.management.resources.fluentcore.model.Creatable
import com.microsoft.azure.management.sql.*
import com.microsoft.azure.management.sql.implementation.DatabaseInner
import com.microsoft.azure.management.storage.StorageAccount
import org.joda.time.DateTime
import rx.Completable
import rx.Observable
import java.util.*

class SqlDatabaseMock(
        private val id: String = "test-sql-database-id",
        private val name: String = "test-sql-database",
        private val sqlServerName: String = "test-sql-server",
        private val resourceGroupName: String = "test-resource-group-name"
) : SqlDatabase {

    override fun inner(): DatabaseInner {
        TODO("Not yet implemented")
    }

    override fun edition(): DatabaseEdition {
        TODO("Not yet implemented")
    }

    override fun sqlServerName(): String = sqlServerName

    override fun maxSizeBytes(): Long {
        TODO("Not yet implemented")
    }

    override fun currentServiceObjectiveId(): UUID {
        TODO("Not yet implemented")
    }

    override fun getDatabaseAutomaticTuning(): SqlDatabaseAutomaticTuning {
        TODO("Not yet implemented")
    }

    override fun getThreatDetectionPolicy(): SqlDatabaseThreatDetectionPolicy {
        TODO("Not yet implemented")
    }

    override fun listServiceTierAdvisors(): MutableMap<String, ServiceTierAdvisor> {
        TODO("Not yet implemented")
    }

    override fun elasticPoolName(): String {
        TODO("Not yet implemented")
    }

    override fun requestedServiceObjectiveId(): UUID {
        TODO("Not yet implemented")
    }

    override fun requestedServiceObjectiveName(): ServiceObjectiveName {
        TODO("Not yet implemented")
    }

    override fun listRestorePoints(): MutableList<RestorePoint> {
        TODO("Not yet implemented")
    }

    override fun rename(p0: String?): SqlDatabase {
        TODO("Not yet implemented")
    }

    override fun refresh(): SqlDatabase {
        TODO("Not yet implemented")
    }

    override fun importBacpac(p0: String?): SqlDatabaseImportRequest.DefinitionStages.WithStorageTypeAndKey {
        TODO("Not yet implemented")
    }

    override fun importBacpac(p0: StorageAccount?, p1: String?, p2: String?): SqlDatabaseImportRequest.DefinitionStages.WithAuthenticationTypeAndLoginPassword {
        TODO("Not yet implemented")
    }

    override fun listServiceTierAdvisorsAsync(): Observable<ServiceTierAdvisor> {
        TODO("Not yet implemented")
    }

    override fun parent(): SqlServer {
        TODO("Not yet implemented")
    }

    override fun status(): String {
        TODO("Not yet implemented")
    }

    override fun listReplicationLinksAsync(): Observable<ReplicationLink> {
        TODO("Not yet implemented")
    }

    override fun defineThreatDetectionPolicy(p0: String?): SqlDatabaseThreatDetectionPolicy.DefinitionStages.Blank {
        TODO("Not yet implemented")
    }

    override fun serviceLevelObjective(): ServiceObjectiveName {
        TODO("Not yet implemented")
    }

    override fun region(): Region {
        TODO("Not yet implemented")
    }

    override fun listMetricDefinitionsAsync(): Observable<SqlDatabaseMetricDefinition> {
        TODO("Not yet implemented")
    }

    override fun key(): String {
        TODO("Not yet implemented")
    }

    override fun syncGroups(): SqlSyncGroupOperations.SqlSyncGroupActionsDefinition {
        TODO("Not yet implemented")
    }

    override fun asWarehouse(): SqlWarehouse {
        TODO("Not yet implemented")
    }

    override fun exportTo(p0: String?): SqlDatabaseExportRequest.DefinitionStages.WithStorageTypeAndKey {
        TODO("Not yet implemented")
    }

    override fun exportTo(p0: StorageAccount?, p1: String?, p2: String?): SqlDatabaseExportRequest.DefinitionStages.WithAuthenticationTypeAndLoginPassword {
        TODO("Not yet implemented")
    }

    override fun exportTo(p0: Creatable<StorageAccount>?, p1: String?, p2: String?): SqlDatabaseExportRequest.DefinitionStages.WithAuthenticationTypeAndLoginPassword {
        TODO("Not yet implemented")
    }

    override fun id(): String = id

    override fun listMetricsAsync(p0: String?): Observable<SqlDatabaseMetric> {
        TODO("Not yet implemented")
    }

    override fun listUsages(): MutableList<DatabaseMetric> {
        TODO("Not yet implemented")
    }

    override fun listMetricDefinitions(): MutableList<SqlDatabaseMetricDefinition> {
        TODO("Not yet implemented")
    }

    override fun getTransparentDataEncryption(): TransparentDataEncryption {
        TODO("Not yet implemented")
    }

    override fun update(): SqlDatabase.Update {
        TODO("Not yet implemented")
    }

    override fun isDataWarehouse(): Boolean {
        TODO("Not yet implemented")
    }

    override fun refreshAsync(): Observable<SqlDatabase> {
        TODO("Not yet implemented")
    }

    override fun resourceGroupName(): String = resourceGroupName

    override fun collation(): String {
        TODO("Not yet implemented")
    }

    override fun creationDate(): DateTime {
        TODO("Not yet implemented")
    }

    override fun parentId(): String {
        TODO("Not yet implemented")
    }

    override fun delete() {
        TODO("Not yet implemented")
    }

    override fun databaseId(): String {
        TODO("Not yet implemented")
    }

    override fun defaultSecondaryLocation(): String {
        TODO("Not yet implemented")
    }

    override fun renameAsync(p0: String?): Observable<SqlDatabase> {
        TODO("Not yet implemented")
    }

    override fun listRestorePointsAsync(): Observable<RestorePoint> {
        TODO("Not yet implemented")
    }

    override fun listMetrics(p0: String?): MutableList<SqlDatabaseMetric> {
        TODO("Not yet implemented")
    }

    override fun earliestRestoreDate(): DateTime {
        TODO("Not yet implemented")
    }

    override fun listUsageMetrics(): MutableList<SqlDatabaseUsageMetric> {
        TODO("Not yet implemented")
    }

    override fun listReplicationLinks(): MutableMap<String, ReplicationLink> {
        TODO("Not yet implemented")
    }

    override fun name(): String = name

    override fun regionName(): String {
        TODO("Not yet implemented")
    }

    override fun getTransparentDataEncryptionAsync(): Observable<TransparentDataEncryption> {
        TODO("Not yet implemented")
    }

    override fun listUsageMetricsAsync(): Observable<SqlDatabaseUsageMetric> {
        TODO("Not yet implemented")
    }

    override fun deleteAsync(): Completable {
        TODO("Not yet implemented")
    }
}
