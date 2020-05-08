/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package com.microsoft.azuretools.core.mvp.model.storage

import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

object AzureStorageAccountMvpModel {

    private val logger = Logger.getLogger(AzureStorageAccountMvpModel::class.java.name)

    private val subscriptionIdToStorageAccountMap = ConcurrentHashMap<String, List<StorageAccount>>()

    fun listAllStorageAccounts(force: Boolean = false): List<ResourceEx<StorageAccount>> {
        if (!force && subscriptionIdToStorageAccountMap.isNotEmpty())
            return subscriptionIdToStorageAccountMap.flatMap { entry -> entry.value.map { account -> ResourceEx(account, entry.key) } }

        val storageAccountRes = ArrayList<ResourceEx<StorageAccount>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        for (subscription in subscriptions) {

            val subscriptionId = subscription.subscriptionId()

            val storageAccounts = listStorageAccountsBySubscriptionId(subscriptionId, force)
            subscriptionIdToStorageAccountMap[subscriptionId] = storageAccounts

            storageAccounts.forEach { account -> storageAccountRes.add(ResourceEx(account, subscriptionId)) }
        }

        return storageAccountRes
    }

    fun listStorageAccountsBySubscriptionId(subscriptionId: String, force: Boolean = false): List<StorageAccount> {
        logger.info("List Azure storage accounts (force: $force).")

        if (!force && subscriptionIdToStorageAccountMap.containsKey(subscriptionId)) {
            val storageAccounts = subscriptionIdToStorageAccountMap[subscriptionId] ?: emptyList()
            logger.info("Found existing subscription with ID '$subscriptionId' in a map. Find ${storageAccounts.size} storage account(s).")
            return storageAccounts
        }

        logger.info("Collecting storage accounts for subscription '$subscriptionId'.")
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        val storageAccounts = azure.storageAccounts().list()
        subscriptionIdToStorageAccountMap[subscriptionId] = storageAccounts

        logger.info("Found ${storageAccounts.size} storage accounts.")
        return storageAccounts
    }

    fun getStorageAccountById(subscriptionId: String, storageAccountId: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).storageAccounts().getById(storageAccountId)

    fun checkNameAvailability(subscriptionId: String, name: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).storageAccounts()
                    .checkNameAvailability(name).isAvailable

    fun isStorageAccountNameExist(subscriptionId: String, name: String, force: Boolean = false): Boolean {
        logger.info("Checking name '$name' availability for subscription '$subscriptionId' (force: $force)")
        if (!force && subscriptionIdToStorageAccountMap.containsKey(subscriptionId)) {
            logger.info("Found existing subscription with ID '$subscriptionId' in a map.")
            return subscriptionIdToStorageAccountMap[subscriptionId]?.any { it.name() == name } ?: false
        }

        logger.info("Check storage account name availability: '$name'")
        return checkNameAvailability(subscriptionId, name)
    }

    /**
     * List all Storage Account Types supported by SDK.
     *
     * @return List of [StorageAccountSkuType] instances.
     */
    fun listStorageAccountType(): List<StorageAccountSkuType> {
        val types = ArrayList<StorageAccountSkuType>()
        for (field in StorageAccountSkuType::class.java.declaredFields) {
            val modifier = field.modifiers
            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier) && Modifier.isFinal(modifier)) {
                val type = field.get(null) as StorageAccountSkuType
                types.add(type)
            }
        }
        return types
    }

    fun refreshStorageAccountsMap() {
        listAllStorageAccounts(true)
    }

    fun clearStorageAccountMap() {
        subscriptionIdToStorageAccountMap.clear()
    }

    @TestOnly
    fun setSubscriptionIdToStorageAccountMap(map: Map<String, List<StorageAccount>>) {
        subscriptionIdToStorageAccountMap.clear()
        map.forEach { (subscriptionId, storageAccountList) ->
            subscriptionIdToStorageAccountMap[subscriptionId] = storageAccountList
        }
    }
}
