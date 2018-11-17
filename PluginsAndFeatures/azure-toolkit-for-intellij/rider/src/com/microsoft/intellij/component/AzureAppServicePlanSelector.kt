/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij.component

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.labels.LinkLabel
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.intellij.component.extension.createDefaultRenderer
import com.microsoft.intellij.component.extension.getSelectedValue
import com.microsoft.intellij.component.extension.setComponentsEnabled
import net.miginfocom.swing.MigLayout
import javax.swing.*

class AzureAppServicePlanSelector : JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")) {

    companion object {
        private const val EMPTY_APP_SERVICE_PLAN_MESSAGE = "No existing Azure App Service Plans"
        private const val WEB_APP_PRICING_URI = "https://azure.microsoft.com/en-us/pricing/details/app-service/"
    }

    private val rdoExistingAppServicePlan = JRadioButton("Use Existing", true)
    private val cbAppServicePlan = ComboBox<AppServicePlan>()
    private val lblExistingLocationName = JLabel("Location")
    private val lblLocationValue = JLabel("N/A")
    private val lblExistingPricingTierName = JLabel("Pricing Tier")
    private val lblExistingPricingTierValue = JLabel("N/A")

    private val rdoCreateAppServicePlan = JRadioButton("Create New")
    private val txtAppServicePlanName = JTextField("")
    private val lblCreateLocationName = JLabel("Location")
    private val cbLocation = ComboBox<Location>()
    private val pnlCreatePricingTier = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[][min!]"))
    private val lblCreatePricingTierName = JLabel("Pricing Tier")
    private val cbPricingTier = ComboBox<PricingTier>()
    private val lblPricingLink = LinkLabel("Pricing", null, { _, link -> BrowserUtil.browse(link) }, WEB_APP_PRICING_URI)

    var lastSelectedAppServicePlan: AppServicePlan? = null
    var listenerAction: () -> Unit = {}

    init {
        initAppServicePlanComboBox()
        initAppServicePlanButtonsGroup()

        pnlCreatePricingTier.apply {
            add(cbPricingTier, "growx")
            add(lblPricingLink)
        }

        add(rdoExistingAppServicePlan)
        add(cbAppServicePlan, "growx")
        add(lblExistingLocationName)
        add(lblLocationValue, "growx")
        add(lblExistingPricingTierName)
        add(lblExistingPricingTierValue, "growx")

        add(rdoCreateAppServicePlan)
        add(txtAppServicePlanName, "growx")
        add(lblCreateLocationName)
        add(cbLocation, "growx")
        add(lblCreatePricingTierName)
        add(pnlCreatePricingTier, "growx")
    }

    private fun initAppServicePlanComboBox() {
        cbAppServicePlan.renderer = cbAppServicePlan.createDefaultRenderer(EMPTY_APP_SERVICE_PLAN_MESSAGE) { it.name() }

        cbAppServicePlan.addActionListener {
            val plan = cbAppServicePlan.getSelectedValue() ?: return@addActionListener
            if (plan == lastSelectedAppServicePlan) return@addActionListener

            lblLocationValue.text = plan.regionName()
            val pricingTier = plan.pricingTier()
            val skuDescription = pricingTier.toSkuDescription()
            lblExistingPricingTierValue.text = "${skuDescription.name()} (${skuDescription.tier()})"

            listenerAction()
            lastSelectedAppServicePlan = plan
        }
    }

    private fun initAppServicePlanButtonsGroup() {
        val appServicePlanButtons = ButtonGroup()
        appServicePlanButtons.add(rdoExistingAppServicePlan)
        appServicePlanButtons.add(rdoCreateAppServicePlan)

        rdoCreateAppServicePlan.addActionListener { toggleAppServicePlanPanel(true) }
        rdoExistingAppServicePlan.addActionListener { toggleAppServicePlanPanel(false) }

        toggleAppServicePlanPanel(false)
    }

    private fun toggleAppServicePlanPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtAppServicePlanName, cbLocation, cbPricingTier)
        setComponentsEnabled(!isCreatingNew, cbAppServicePlan, lblLocationValue, lblExistingPricingTierValue)
    }
}