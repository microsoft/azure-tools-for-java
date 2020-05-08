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
import com.microsoft.azure.management.storage.*
import com.microsoft.azure.management.storage.implementation.AccountStatuses
import com.microsoft.azure.management.storage.implementation.StorageAccountInner
import com.microsoft.azure.management.storage.implementation.StorageManager
import com.microsoft.rest.ServiceCallback
import com.microsoft.rest.ServiceFuture
import org.joda.time.DateTime
import rx.Observable

class StorageAccountMock(
        private val name: String = "test-storage-account"
) : StorageAccount {

    override fun isAccessAllowedFromAllNetworks(): Boolean {
        TODO("Not yet implemented")
    }

    override fun inner(): StorageAccountInner {
        TODO("Not yet implemented")
    }

    override fun skuType(): StorageAccountSkuType {
        TODO("Not yet implemented")
    }

    override fun systemAssignedManagedServiceIdentityPrincipalId(): String {
        TODO("Not yet implemented")
    }

    override fun endPoints(): PublicEndpoints {
        TODO("Not yet implemented")
    }

    override fun accessTier(): AccessTier {
        TODO("Not yet implemented")
    }

    override fun isAzureFilesAadIntegrationEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun regenerateKey(p0: String?): MutableList<StorageAccountKey> {
        TODO("Not yet implemented")
    }

    override fun tags(): MutableMap<String, String> {
        TODO("Not yet implemented")
    }

    override fun refresh(): StorageAccount {
        TODO("Not yet implemented")
    }

    override fun networkSubnetsWithAccess(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun type(): String {
        TODO("Not yet implemented")
    }

    override fun ipAddressesWithAccess(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun customDomain(): CustomDomain {
        TODO("Not yet implemented")
    }

    override fun manager(): StorageManager {
        TODO("Not yet implemented")
    }

    override fun ipAddressRangesWithAccess(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun region(): Region {
        TODO("Not yet implemented")
    }

    override fun key(): String {
        TODO("Not yet implemented")
    }

    override fun provisioningState(): ProvisioningState {
        TODO("Not yet implemented")
    }

    override fun sku(): Sku {
        TODO("Not yet implemented")
    }

    override fun creationTime(): DateTime {
        TODO("Not yet implemented")
    }

    override fun canReadLogEntriesFromAnyNetwork(): Boolean {
        TODO("Not yet implemented")
    }

    override fun canReadMetricsFromAnyNetwork(): Boolean {
        TODO("Not yet implemented")
    }

    override fun id(): String {
        TODO("Not yet implemented")
    }

    override fun isHnsEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getKeysAsync(): Observable<MutableList<StorageAccountKey>> {
        TODO("Not yet implemented")
    }

    override fun getKeysAsync(p0: ServiceCallback<MutableList<StorageAccountKey>>?): ServiceFuture<MutableList<StorageAccountKey>> {
        TODO("Not yet implemented")
    }

    override fun accountStatuses(): AccountStatuses {
        TODO("Not yet implemented")
    }

    override fun update(): StorageAccount.Update {
        TODO("Not yet implemented")
    }

    override fun encryptionStatuses(): MutableMap<StorageService, StorageAccountEncryptionStatus> {
        TODO("Not yet implemented")
    }

    override fun refreshAsync(): Observable<StorageAccount> {
        TODO("Not yet implemented")
    }

    override fun resourceGroupName(): String {
        TODO("Not yet implemented")
    }

    override fun encryption(): Encryption {
        TODO("Not yet implemented")
    }

    override fun systemAssignedManagedServiceIdentityTenantId(): String {
        TODO("Not yet implemented")
    }

    override fun isLargeFileSharesEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getKeys(): MutableList<StorageAccountKey> {
        TODO("Not yet implemented")
    }

    override fun name(): String = name

    override fun regenerateKeyAsync(p0: String?): Observable<MutableList<StorageAccountKey>> {
        TODO("Not yet implemented")
    }

    override fun regenerateKeyAsync(p0: String?, p1: ServiceCallback<MutableList<StorageAccountKey>>?): ServiceFuture<MutableList<StorageAccountKey>> {
        TODO("Not yet implemented")
    }

    override fun encryptionKeySource(): StorageAccountEncryptionKeySource {
        TODO("Not yet implemented")
    }

    override fun regionName(): String {
        TODO("Not yet implemented")
    }

    override fun kind(): Kind {
        TODO("Not yet implemented")
    }

    override fun canAccessFromAzureServices(): Boolean {
        TODO("Not yet implemented")
    }

    override fun lastGeoFailoverTime(): DateTime {
        TODO("Not yet implemented")
    }
}
