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

package com.microsoft.intellij.helpers.functionapp

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.HideableTitledPanel
import com.intellij.ui.HyperlinkLabel
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azuretools.core.mvp.ui.functionapp.FunctionAppProperty
import com.microsoft.intellij.helpers.base.BaseEditor
import com.microsoft.intellij.ui.components.AppSettingsComponent
import com.microsoft.intellij.ui.components.AzureActionListenerWrapper
import com.microsoft.intellij.ui.util.UIUtils
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppPropertyMvpView
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.base.FunctionAppBasePropertyViewPresenter
import net.miginfocom.swing.MigLayout
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

abstract class FunctionAppBasePropertyView(val project: Project,
                                           val subscriptionId: String,
                                           val resId: String,
                                           val slotName: String) : BaseEditor(), FunctionAppPropertyMvpView {

    companion object {
        private const val HEADER_OVERVIEW = "Overview"
        private const val HEADER_APP_SETTINGS = "App Settings"

        private const val TXT_NA = "N/A"
        private const val TABLE_EMPTY_MESSAGE = "No available settings."
        private const val FILE_SELECTOR_TITLE = "Choose Where You Want to Save the Publish Profile."
        private const val NOTIFY_PROPERTY_UPDATE_SUCCESS = "Properties updated."
        private const val NOTIFY_PROFILE_GET_SUCCESS = "Publish Profile saved."
        private const val NOTIFY_PROFILE_GET_FAIL = "Failed to get Publish Profile."
        private const val TEXT_LOADING = "Loading..."

        private const val INSIGHT_NAME = "AzurePlugin.IntelliJ.Editor.FunctionAppBasePropertyView"
    }

    private val pnlMain = JPanel(MigLayout("novisualpadding, ins 5, fillx, wrap 1"))
    private val pnlControlButtons = JPanel(MigLayout("novisualpadding, ins 0, wrap 3"))
    private val btnGetPublishFile = JButton("Get Publish Profile", IconLoader.getIcon("actions/download.svg"))
    private val btnSave = JButton("Save")
    private val btnDiscard = JButton("Discard")

    private val pnlOverview = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 4", "[10%][40%][10%][40%]"))
    private val pnlOverviewHolder = HideableTitledPanel(HEADER_OVERVIEW, false, pnlOverview, true)

    private val lblResourceGroup = JLabel("Resource Group:")
    private val txtResourceGroup = JTextField(TEXT_LOADING)
    private val lblStatus = JLabel("Status:")
    private val txtStatus = JTextField(TEXT_LOADING)
    private val lblLocation = JLabel("Location:")
    private val txtLocation = JTextField(TEXT_LOADING)
    private val lblSubscription = JLabel("Subscription ID:")
    private val txtSubscription = JTextField(TEXT_LOADING)
    private val lblAppServicePlan = JLabel("App Service Plan:")
    private val txtAppServicePlan = JTextField(TEXT_LOADING)
    private val lblUrl = JLabel("URL:")
    private val lnkUrl = HyperlinkLabel()
    private val lblPricingTier = JLabel("Pricing Tier:")
    private val txtPricingTier = JTextField(TEXT_LOADING)
    private val lblOperatingSystem = JLabel("OS:")
    private val txtOperatingSystem = JTextField(TEXT_LOADING)

    private val pnlAppSettings = AppSettingsComponent()
    private val pnlAppSettingsHolder = HideableTitledPanel(HEADER_APP_SETTINGS, false, pnlAppSettings, true)

    val appId: String
    protected val presenter: FunctionAppBasePropertyViewPresenter<FunctionAppPropertyMvpView>
    private val statusBar: StatusBar

    init {

        initButtons()
        initAppSettingTable()

        pnlControlButtons.apply {
            add(btnGetPublishFile)
            add(btnSave)
            add(btnDiscard)
        }

        setBoldFont(
                lblResourceGroup,
                lblStatus,
                lblLocation,
                lblUrl,
                lblSubscription,
                lblAppServicePlan,
                lblPricingTier,
                lblOperatingSystem)

        pnlOverview.apply {
            add(lblResourceGroup)
            add(txtResourceGroup, "growx")
            add(lblAppServicePlan)
            add(txtAppServicePlan, "growx")
            add(lblStatus)
            add(txtStatus, "growx")
            add(lblUrl)
            add(lnkUrl, "growx")
            add(lblLocation)
            add(txtLocation, "growx")
            add(lblPricingTier)
            add(txtPricingTier, "growx")
            add(lblSubscription)
            add(txtSubscription, "growx")
            add(lblOperatingSystem)
            add(txtOperatingSystem, "growx")
        }

        pnlMain.apply {
            add(pnlControlButtons, "growx")
            add(pnlOverviewHolder, "growx")
            add(pnlAppSettingsHolder, "growx")
        }

        appId = getId()
        presenter = createPresenter()
        presenter.onAttachView(this)
        statusBar = WindowManager.getInstance().getStatusBar(project)

        lnkUrl.setHyperlinkText("<Loading...>")
        setTextFieldStyle()
    }

    protected abstract fun getId(): String

    protected abstract fun createPresenter(): FunctionAppBasePropertyViewPresenter<FunctionAppPropertyMvpView>

    override fun onLoadFunctionAppProperty(sid: String, appId: String, slotName: String) = presenter.onLoadFunctionAppProperty(sid, appId)
    override fun getComponent() = pnlMain
    override fun getName() = appId

    override fun dispose() {
        presenter.onDetachView()
    }

    override fun showProperty(property: FunctionAppProperty) {
        txtResourceGroup.text  = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_RESOURCE_GROUP) as? String ?: TXT_NA
        txtStatus.text         = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_STATUS) as? String ?: TXT_NA
        txtLocation.text       = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_LOCATION) as? String ?: TXT_NA
        txtSubscription.text   = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_SUBSCRIPTION_ID) as? String ?: TXT_NA
        txtAppServicePlan.text = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_APP_SERVICE_PLAN) as? String ?: TXT_NA

        val url = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_URL) as? String
        if (url == null) {
            lnkUrl.setHyperlinkText(TXT_NA)
        } else {
            val linkText = "http://$url"
            lnkUrl.setHyperlinkText(linkText)
            lnkUrl.setHyperlinkTarget(linkText)
        }

        txtPricingTier.text     = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_PRICING_TIER) as? String ?: TXT_NA
        txtOperatingSystem.text = (property.getValue(FunctionAppBasePropertyViewPresenter.KEY_OPERATING_SYSTEM) as? OperatingSystem)?.name?.toLowerCase()?.capitalize() ?: TXT_NA

        val tableModel = pnlAppSettings.table.model as DefaultTableModel
        tableModel.dataVector.removeAllElements()
        pnlAppSettings.cachedAppSettings.clear()
        pnlAppSettings.table.emptyText.text = TABLE_EMPTY_MESSAGE
        val appSettings = property.getValue(FunctionAppBasePropertyViewPresenter.KEY_APP_SETTING) as? Map<*, *>
        if (appSettings != null) {
            for (key in appSettings.keys) {
                if (key !is String) continue
                val value = appSettings[key] as? String ?: continue
                tableModel.addRow(arrayOf(key, value))
                pnlAppSettings.cachedAppSettings[key] = value
            }
        }
        updateMapStatus(pnlAppSettings.editedAppSettings, pnlAppSettings.cachedAppSettings)
        pnlOverview.revalidate()
        pnlAppSettings.revalidate()
    }

    override fun showPropertyUpdateResult(isSuccess: Boolean) {
        setBtnEnableStatus(true)
        if (isSuccess) {
            updateMapStatus(pnlAppSettings.cachedAppSettings, pnlAppSettings.editedAppSettings)
            UIUtils.showNotification(statusBar, NOTIFY_PROPERTY_UPDATE_SUCCESS, MessageType.INFO)
        }
    }

    override fun showGetPublishingProfileResult(isSuccess: Boolean) {
        if (isSuccess) {
            UIUtils.showNotification(statusBar, NOTIFY_PROFILE_GET_SUCCESS, MessageType.INFO)
        } else {
            UIUtils.showNotification(statusBar, NOTIFY_PROFILE_GET_FAIL, MessageType.ERROR)
        }
    }

    private fun updateMapStatus(to: MutableMap<String, String>, from: Map<String, String>) {
        to.clear()
        to.putAll(from)
        updateSaveAndDiscardBtnStatus()
    }

    private fun setBtnEnableStatus(enabled: Boolean) {
        btnSave.isEnabled = enabled
        btnDiscard.isEnabled = enabled
        pnlAppSettings.btnAdd.isEnabled = enabled
        pnlAppSettings.btnDelete.isEnabled = enabled
        pnlAppSettings.btnEdit.isEnabled = enabled
        pnlAppSettings.table.isEnabled = enabled
    }

    private fun updateSaveAndDiscardBtnStatus() {
        if (Comparing.equal(pnlAppSettings.editedAppSettings, pnlAppSettings.cachedAppSettings)) {
            btnDiscard.isEnabled = false
            btnSave.isEnabled = false
        } else {
            btnDiscard.isEnabled = true
            btnSave.isEnabled = true
        }
    }

    private fun setTextFieldStyle() {
        val controls = arrayOf(
                txtResourceGroup,
                txtStatus,
                txtLocation,
                txtSubscription,
                txtAppServicePlan,
                txtPricingTier,
                txtOperatingSystem)

        controls.forEach { control ->
            control.border = BorderFactory.createEmptyBorder()
            control.background = null
        }
    }

    private fun initButtons() {

        btnGetPublishFile.addActionListener(object : AzureActionListenerWrapper(INSIGHT_NAME, "btnGetPublishFile", null) {
            override fun actionPerformedFunc(event: ActionEvent) {
                val fileChooserDescriptor = FileChooserDescriptor(
                        false /*chooseFiles*/,
                        true /*chooseFolders*/,
                        false /*chooseJars*/,
                        false /*chooseJarsAsFiles*/,
                        false /*chooseJarContents*/,
                        false /*chooseMultiple*/
                )
                fileChooserDescriptor.title = FILE_SELECTOR_TITLE
                val file = FileChooser.chooseFile(fileChooserDescriptor, null, null)
                        ?: return

                presenter.onGetPublishingProfileXmlWithSecrets(subscriptionId, resId, file.path)
            }
        })

        btnDiscard.addActionListener(object : AzureActionListenerWrapper(INSIGHT_NAME, "btnDiscard", null) {
            override fun actionPerformedFunc(event: ActionEvent) {
                val tableModel = pnlAppSettings.table.model as DefaultTableModel

                updateMapStatus(pnlAppSettings.editedAppSettings, pnlAppSettings.cachedAppSettings)
                tableModel.dataVector.removeAllElements()
                for (key in pnlAppSettings.editedAppSettings.keys) {
                    tableModel.addRow(arrayOf(key, pnlAppSettings.editedAppSettings[key]))
                }
                tableModel.fireTableDataChanged()
            }
        })

        btnSave.addActionListener(object : AzureActionListenerWrapper(INSIGHT_NAME, "btnSave", null) {
            override fun actionPerformedFunc(event: ActionEvent) {
                setBtnEnableStatus(false)
                presenter.onUpdateFunctionAppProperty(subscriptionId, resId, slotName, pnlAppSettings.cachedAppSettings, pnlAppSettings.editedAppSettings)
            }
        })
    }

    private fun initAppSettingTable() {
        pnlAppSettings.table.addPropertyChangeListener { event ->
            if ("tableCellEditor" != event.propertyName || pnlAppSettings.table.isEditing)
                return@addPropertyChangeListener

            updateSaveAndDiscardBtnStatus()
        }

        pnlAppSettings.deleteButtonAction = { updateSaveAndDiscardBtnStatus() }
    }

    private fun setBoldFont(vararg components: JLabel) {
        components.forEach { component ->
            val font = component.font
            component.font = Font(font.name, Font.BOLD, font.size)
        }
    }
}
