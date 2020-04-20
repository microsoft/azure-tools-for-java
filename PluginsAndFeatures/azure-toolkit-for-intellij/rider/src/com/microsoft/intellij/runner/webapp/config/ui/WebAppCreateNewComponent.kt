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

package com.microsoft.intellij.runner.webapp.config.ui

import com.intellij.ui.HideableTitledPanel
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.AzureResourceGroupSelector
import com.microsoft.intellij.ui.component.AzureSubscriptionsSelector
import com.microsoft.intellij.ui.component.appservice.AppNameComponent
import com.microsoft.intellij.ui.component.appservice.AppServicePlanSelector
import com.microsoft.intellij.ui.component.appservice.OperatingSystemSelector
import com.microsoft.intellij.ui.extension.fillComboBox
import com.microsoft.intellij.ui.extension.setComponentsEnabled
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JPanel

class WebAppCreateNewComponent(lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    val pnlAppName = AppNameComponent(lifetime.createNested())
    val pnlSubscription = AzureSubscriptionsSelector()

    val pnlResourceGroup = AzureResourceGroupSelector(lifetime.createNested())
    private val pnlResourceGroupHolder = HideableTitledPanel(
            message("run_config.publish.form.resource_group.header"), pnlResourceGroup, true)

    val pnlOperatingSystem = OperatingSystemSelector()
    private val pnlOperatingSystemHolder = HideableTitledPanel(
            message("run_config.publish.form.operating_system.header"), pnlOperatingSystem, true)

    val pnlAppServicePlan = AppServicePlanSelector(lifetime.createNested())
    private val pnlAppServicePlanHolder = HideableTitledPanel(
            message("run_config.publish.form.service_plan.header"), pnlAppServicePlan, true)

    init {
        initButtonGroupsState()

        add(pnlAppName, "growx")
        add(pnlSubscription, "growx")
        add(pnlResourceGroupHolder, "growx")
        add(pnlOperatingSystemHolder, "growx")
        add(pnlAppServicePlanHolder, "growx")
    }

    fun setOperatingSystemRadioButtons(isDotNetCore: Boolean) {
        setComponentsEnabled(isDotNetCore, pnlOperatingSystem.rdoOperatingSystemLinux)

        if (!isDotNetCore) {
            pnlOperatingSystem.rdoOperatingSystemWindows.doClick()
            toggleOperatingSystem(OperatingSystem.WINDOWS)
        }
    }

    //region Button Group

    private fun initButtonGroupsState() {
        initOperatingSystemButtonGroup()
    }

    private fun initOperatingSystemButtonGroup() {
        pnlOperatingSystem.rdoOperatingSystemWindows.addActionListener { toggleOperatingSystem(OperatingSystem.WINDOWS) }
        pnlOperatingSystem.rdoOperatingSystemLinux.addActionListener { toggleOperatingSystem(OperatingSystem.LINUX) }
    }

    fun toggleOperatingSystem(operatingSystem: OperatingSystem) {
        pnlAppServicePlan.cbAppServicePlan.fillComboBox(
                filterAppServicePlans(operatingSystem, pnlAppServicePlan.cachedAppServicePlan),
                pnlAppServicePlan.lastSelectedAppServicePlan
        )

        pnlAppServicePlan.cbPricingTier.fillComboBox(
                filterPricingTiers(operatingSystem, pnlAppServicePlan.cachedPricingTier),
                pnlAppServicePlan.lastSelectedAppServicePlan?.pricingTier()
        )
    }

    //endregion Button Group

    //region Fill Values

    fun fillSubscription(subscriptions: List<Subscription>, defaultSubscription: Subscription? = null) {
        pnlSubscription.fillSubscriptionComboBox(subscriptions, defaultSubscription)
    }

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>, defaultResourceGroupName: String? = null) {
        pnlResourceGroup.fillResourceGroupComboBox(resourceGroups) {
            resourceGroup -> resourceGroup.name() == defaultResourceGroupName
        }
    }

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>, defaultAppServicePlanId: String? = null) {
        pnlAppServicePlan.fillAppServicePlanComboBox(
                filterAppServicePlans(pnlOperatingSystem.deployOperatingSystem, appServicePlans)) {
            appServicePlan -> appServicePlan.id() == defaultAppServicePlanId
        }
    }

    fun fillLocation(locations: List<Location>, defaultLocation: Region? = null) {
        pnlAppServicePlan.fillLocationComboBox(locations, defaultLocation)
    }

    fun fillPricingTier(pricingTiers: List<PricingTier>, defaultPricingTier: PricingTier? = null) {
        pnlAppServicePlan.fillPricingTier(
                filterPricingTiers(pnlOperatingSystem.deployOperatingSystem, pricingTiers),
                defaultPricingTier
        )
    }

    //endregion Fill Values

    /**
     * Filter App Service Plans to Operating System related values
     */
    private fun filterAppServicePlans(operatingSystem: OperatingSystem,
                                      appServicePlans: List<AppServicePlan>): List<AppServicePlan> {
        return appServicePlans
                .filter { it.operatingSystem() == operatingSystem }
                .sortedWith(compareBy({ it.operatingSystem() }, { it.name() }))
    }

    /**
     * Filter Pricing Tier based on Operating System
     *
     * Note: We should hide "Free" and "Shared" Pricing Tiers for Linux OS
     */
    private fun filterPricingTiers(operatingSystem: OperatingSystem,
                                   prices: List<PricingTier>): List<PricingTier> {
        if (operatingSystem == OperatingSystem.WINDOWS) return prices
        return prices.filter { it != PricingTier.FREE_F1 && it != PricingTier.SHARED_D1 }
    }
}
