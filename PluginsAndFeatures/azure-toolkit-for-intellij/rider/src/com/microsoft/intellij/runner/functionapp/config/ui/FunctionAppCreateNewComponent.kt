/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.functionapp.config.ui

import com.intellij.ui.HideableTitledPanel
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.intellij.component.AzureComponent
import com.microsoft.intellij.component.AzureResourceGroupSelector
import com.microsoft.intellij.component.AzureSubscriptionsSelector
import com.microsoft.intellij.component.StorageAccountSelector
import com.microsoft.intellij.component.appservice.AppNameComponent
import com.microsoft.intellij.component.appservice.HostingPlanSelector
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel

class FunctionAppCreateNewComponent(lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {
        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_HOSTING_PLAN = "Hosting Plan"
        private const val HEADER_STORAGE_ACCOUNT = "Storage Account"
    }

    val pnlAppName = AppNameComponent(lifetime.createNested())

    val pnlSubscription = AzureSubscriptionsSelector()

    val pnlResourceGroup = AzureResourceGroupSelector(lifetime.createNested())
    private val pnlResourceGroupHolder = HideableTitledPanel(HEADER_RESOURCE_GROUP, pnlResourceGroup, true)

    val pnlHostingPlan = HostingPlanSelector(lifetime.createNested())
    private val pnlHostingPlanHolder = HideableTitledPanel(HEADER_HOSTING_PLAN, pnlHostingPlan, true)

    val pnlStorageAccount = StorageAccountSelector(lifetime.createNested())
    private val pnlStorageAccountHolder = HideableTitledPanel(HEADER_STORAGE_ACCOUNT, pnlStorageAccount, true)

    init {
        add(pnlAppName, "growx")
        add(pnlSubscription, "growx")
        add(pnlResourceGroupHolder, "growx")
        add(pnlHostingPlanHolder, "growx")
        add(pnlStorageAccountHolder, "growx")
    }

    fun fillSubscription(subscriptions: List<Subscription>, defaultSubscription: Subscription? = null) =
            pnlSubscription.fillSubscriptionComboBox(subscriptions, defaultSubscription)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>, defaultResourceGroupName: String? = null) {
        pnlResourceGroup.fillResourceGroupComboBox(resourceGroups) {
            resourceGroup -> resourceGroup.name() == defaultResourceGroupName
        }
    }

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>, defaultAppServicePlanId: String? = null) =
            pnlHostingPlan.fillAppServicePlan(filterAppServicePlans(appServicePlans), defaultAppServicePlanId)

    fun fillLocation(locations: List<Location>, defaultLocation: Region? = null) =
            pnlHostingPlan.fillLocationComboBox(locations, defaultLocation)

    fun fillPricingTier(pricingTiers: List<PricingTier>, defaultPricingTier: PricingTier? = null) =
            pnlHostingPlan.fillPricingTier(updatePricingTiers(pricingTiers), defaultPricingTier)

    fun fillStorageAccount(storageAccounts: List<StorageAccount>, defaultStorageAccountId: String? = null) =
            pnlStorageAccount.fillStorageAccount(storageAccounts, defaultStorageAccountId)

    fun fillStorageAccountType(storageAccountType: List<StorageAccountSkuType>, defaultType: StorageAccountSkuType? = null) =
            pnlStorageAccount.fillStorageAccountType(storageAccountType, defaultType)

    /**
     * Filter App Service Plans to Operating System related values
     */
    private fun filterAppServicePlans(appServicePlans: List<AppServicePlan>): List<AppServicePlan> {
        return appServicePlans
                .filter { it.operatingSystem() == OperatingSystem.WINDOWS }
                .sortedWith(compareBy({ it.operatingSystem() }, { it.name() }))
    }

    /**
     * Update Pricing Tiers for Functions App
     *
     * Note: Function Apps cannot use "Free" and "Shared" Pricing Tiers.
     *       Add Consumption plan for pricing on demand
     */
    private fun updatePricingTiers(prices: List<PricingTier>) =
            prices.filter { it != PricingTier.FREE_F1 && it != PricingTier.SHARED_D1 } +
                    FunctionAppPublishModel.consumptionPricingTier
}
