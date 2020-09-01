/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component.appservice

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.*
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.AppServicePlanValidator
import com.microsoft.intellij.helpers.validator.LocationValidator
import com.microsoft.intellij.helpers.validator.PricingTierValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.awt.event.ActionListener
import javax.swing.*

class AppServicePlanSelector(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]", "[sg a]")),
        AzureComponent {

    companion object {
        private val indentionSize = JBUI.scale(17)
        private const val WEB_APP_PRICING_URI = "https://azure.microsoft.com/en-us/pricing/details/app-service/"
    }

    val rdoUseExisting = JRadioButton(message("run_config.publish.form.service_plan.use_existing"), true)
    val cbAppServicePlan = ComboBox<AppServicePlan>()
    private val lblExistingLocationName = JLabel(message("run_config.publish.form.service_plan.location.label"))
    private val lblLocationValue = JLabel(message("common.na_capitalized"))
    private val lblExistingPricingTierName = JLabel(message("run_config.publish.form.service_plan.pricing_tier.label"))
    private val lblExistingPricingTierValue = JLabel(message("common.na_capitalized"))

    val rdoCreateNew = JRadioButton(message("run_config.publish.form.service_plan.create_new"))
    val txtName = JTextField("")
    private val lblCreateLocationName = JLabel(message("run_config.publish.form.service_plan.location.label"))
    val cbLocation = ComboBox<Location>()
    private val pnlCreatePricingTier = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[][min!]"))
    private val lblCreatePricingTierName = JLabel(message("run_config.publish.form.service_plan.pricing_tier.label"))
    val cbPricingTier = ComboBox<PricingTier>()
    private val lblPricingLink = LinkLabel(message("run_config.publish.form.service_plan.pricing.label"), null, { _, link -> BrowserUtil.browse(link) }, WEB_APP_PRICING_URI)

    var cachedAppServicePlan: List<AppServicePlan> = emptyList()
    var cachedPricingTier: List<PricingTier> = emptyList()

    var lastSelectedAppServicePlan: AppServicePlan? = null

    val isCreatingNew
        get() = rdoCreateNew.isSelected

    val servicePlanName: String
        get() = txtName.text

    val pricingTier: PricingTier?
        get() = cbPricingTier.getSelectedValue()

    val location: Region?
        get() = cbLocation.getSelectedValue()?.region()

    init {
        initAppServicePlanComboBox()
        initLocationComboBox()
        initPricingTierComboBox()
        initAppServicePlanButtonsGroup()

        pnlCreatePricingTier.apply {
            add(cbPricingTier, "growx")
            add(lblPricingLink)
        }

        add(rdoUseExisting)
        add(cbAppServicePlan, "growx")
        add(lblExistingLocationName, "gapbefore $indentionSize")
        add(lblLocationValue, "growx")
        add(lblExistingPricingTierName, "gapbefore $indentionSize")
        add(lblExistingPricingTierValue, "growx")

        add(rdoCreateNew)
        add(txtName, "growx")
        add(lblCreateLocationName, "gapbefore $indentionSize")
        add(cbLocation, "growx")
        add(lblCreatePricingTierName, "gapbefore $indentionSize")
        add(pnlCreatePricingTier, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (!isEnabled) return emptyList()

        if (rdoUseExisting.isSelected) {
            return listOfNotNull(
                    AppServicePlanValidator.checkAppServicePlanIsSet(cbAppServicePlan.getSelectedValue())
                            .toValidationInfo(cbAppServicePlan)
            )
        }

        return listOfNotNull(
                AppServicePlanValidator.validateAppServicePlanName(txtName.text).toValidationInfo(txtName),
                LocationValidator.checkLocationIsSet(cbLocation.getSelectedValue()).toValidationInfo(cbLocation),
                PricingTierValidator.checkPricingTierIsSet(cbPricingTier.getSelectedValue()).toValidationInfo(cbPricingTier)
        )
    }

    override fun initComponentValidation() {
        txtName.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    AppServicePlanValidator.checkAppServicePlanNameMaxLength(txtName.text)
                            .merge(AppServicePlanValidator.checkInvalidCharacters(txtName.text)) },
                focusLostValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    if (txtName.text.isEmpty()) return@initValidationWithResult ValidationResult()
                    AppServicePlanValidator.checkAppServicePlanNameMinLength(txtName.text) })
    }

    fun fillAppServicePlanComboBox(appServicePlans: List<AppServicePlan>, defaultComparator: (AppServicePlan) -> Boolean = { false }) {
        cachedAppServicePlan = appServicePlans

        cbAppServicePlan.fillComboBox(
                appServicePlans.sortedWith(compareBy({ it.operatingSystem() }, { it.name() })),
                defaultComparator)

        if (appServicePlans.isEmpty()) {
            rdoCreateNew.doClick()
        }
    }

    fun fillLocationComboBox(locations: List<Location>, defaultLocation: Region? = AzureDefaults.location) {
        cbLocation.fillComboBox<Location>(
                elements = locations,
                defaultComparator = { location -> location.region() == defaultLocation }
        )
    }

    fun fillPricingTier(pricingTiers: List<PricingTier>, defaultPricingTier: PricingTier? = AzureDefaults.pricingTier) {
        cachedPricingTier = pricingTiers
        cbPricingTier.fillComboBox(pricingTiers, defaultPricingTier)
    }

    fun toggleAppServicePlanPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtName, cbLocation, cbPricingTier)
        setComponentsEnabled(!isCreatingNew, cbAppServicePlan, lblLocationValue, lblExistingPricingTierValue)
    }

    private fun initAppServicePlanComboBox() {
        cbAppServicePlan.setDefaultRenderer(message("run_config.publish.form.service_plan.no_existing_values")) { it.name() }

        cbAppServicePlan.addActionListener {
            val plan = cbAppServicePlan.getSelectedValue() ?: return@addActionListener
            if (plan == lastSelectedAppServicePlan) return@addActionListener

            lblLocationValue.text = plan.regionName()
            val pricingTier = plan.pricingTier()
            val skuDescription = pricingTier.toSkuDescription()
            lblExistingPricingTierValue.text = "${skuDescription.name()} (${skuDescription.tier()})"

            lastSelectedAppServicePlan = plan
        }
    }

    private fun initLocationComboBox() {
        cbLocation.setDefaultRenderer(message("run_config.publish.form.service_plan.location.empty_message")) { it.displayName() }
    }

    private fun initPricingTierComboBox() {
        cbPricingTier.setDefaultRenderer(message("run_config.publish.form.service_plan.pricing_tier.empty_message")) {
            val skuDescription = it.toSkuDescription()
            "${skuDescription.name()} (${skuDescription.tier()})"
        }
    }

    private fun initAppServicePlanButtonsGroup() {
        initButtonsGroup(hashMapOf(
                rdoUseExisting to ActionListener { toggleAppServicePlanPanel(false) },
                rdoCreateNew to ActionListener { toggleAppServicePlanPanel(true) }))

        toggleAppServicePlanPanel(false)
    }
}