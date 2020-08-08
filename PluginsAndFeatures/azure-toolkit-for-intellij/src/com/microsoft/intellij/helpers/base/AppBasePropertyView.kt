/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.helpers.base

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.HideableTitledPanel
import com.intellij.ui.HyperlinkLabel
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty
import com.microsoft.icons.CommonIcons
import com.microsoft.intellij.ui.components.AppSettingsComponent
import com.microsoft.intellij.ui.components.AzureActionListenerWrapper
import com.microsoft.intellij.ui.util.UIUtils
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter
import net.miginfocom.swing.MigLayout
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

abstract class AppBasePropertyView(val project: Project,
                                   val subscriptionId: String,
                                   val resourceId: String,
                                   val slotName: String?) : BaseEditor(), WebAppBasePropertyMvpView {

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

        private const val INSIGHT_NAME = "AzurePlugin.IntelliJ.Editor.AppBasePropertyView"
    }

    private val pnlMain = JPanel(MigLayout("novisualpadding, ins 5, fillx, wrap 1"))
    private val pnlControlButtons = JPanel(MigLayout("novisualpadding, ins 0, wrap 3"))
    private val btnGetPublishFile = JButton("Get Publish Profile", AllIcons.Actions.Download)
    private val btnSave = JButton("Save", CommonIcons.SaveChanges)
    private val btnDiscard = JButton("Discard", CommonIcons.Discard)

    // TODO: This this layout
    private val pnlOverview = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 4",
            "[10%][shrink 0, shrinkprio 1][10%][40%, shrinkprio 2]"))
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
    private val txtOperatingSystem = JLabel(TEXT_LOADING)

    private val lblRuntime = JLabel("Runtime:")
    private val txtRuntime = JTextField(TEXT_LOADING)

    private val pnlAppSettings = AppSettingsComponent()
    private val pnlAppSettingsHolder = HideableTitledPanel(HEADER_APP_SETTINGS, false, pnlAppSettings, true)

    val appId: String
    protected val presenter: WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView>
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
                lblOperatingSystem,
                lblRuntime
        )

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
            add(lblRuntime)
            add(txtRuntime, "growx")
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

    protected abstract fun createPresenter(): WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView>

    override fun onLoadWebAppProperty(sid: String, appId: String, slotName: String?) =
            presenter.onLoadWebAppProperty(sid, appId, slotName)

    override fun getComponent() = pnlMain

    override fun getName() = appId

    override fun dispose() {
        presenter.onDetachView()
    }

    override fun showProperty(property: WebAppProperty) {
        txtResourceGroup.text  = property.getValue(WebAppBasePropertyViewPresenter.KEY_RESOURCE_GRP) as? String ?: TXT_NA
        txtStatus.text         = property.getValue(WebAppBasePropertyViewPresenter.KEY_STATUS) as? String ?: TXT_NA
        txtLocation.text       = property.getValue(WebAppBasePropertyViewPresenter.KEY_LOCATION) as? String ?: TXT_NA
        txtSubscription.text   = property.getValue(WebAppBasePropertyViewPresenter.KEY_SUB_ID) as? String ?: TXT_NA
        txtAppServicePlan.text = property.getValue(WebAppBasePropertyViewPresenter.KEY_PLAN) as? String ?: TXT_NA

        val url = property.getValue(WebAppBasePropertyViewPresenter.KEY_URL) as? String
        if (url == null) {
            lnkUrl.setHyperlinkText(TXT_NA)
        } else {
            val linkText = "http://$url"
            lnkUrl.setHyperlinkText(linkText)
            lnkUrl.setHyperlinkTarget(linkText)
        }

        txtPricingTier.text = property.getValue(WebAppBasePropertyViewPresenter.KEY_PRICING) as? String ?: TXT_NA

        // Operating System
        val operatingSystem = property.getValue(WebAppBasePropertyViewPresenter.KEY_OPERATING_SYS) as? OperatingSystem
        txtOperatingSystem.text = operatingSystem?.name?.toLowerCase()?.capitalize() ?: TXT_NA
        txtOperatingSystem.icon = when (operatingSystem) {
            OperatingSystem.WINDOWS -> CommonIcons.OS.Windows
            OperatingSystem.LINUX -> CommonIcons.OS.Linux
            else -> null
        }

        // Runtime
        txtRuntime.text =
                when (operatingSystem) {
                    OperatingSystem.WINDOWS -> {
                        val netFrameworkVersionString = property.getValue(WebAppBasePropertyViewPresenter
                                .KEY_NET_FRAMEWORK_VERSION) as? String ?: TXT_NA
                        when {
                            netFrameworkVersionString.startsWith("v4") -> "v4.7"
                            netFrameworkVersionString.startsWith("v2") -> "v3.5"
                            else -> netFrameworkVersionString
                        }
                    }
                    OperatingSystem.LINUX -> {
                        property.getValue(WebAppBasePropertyViewPresenter.KEY_LINUX_FX_VERSION) as? String ?: TXT_NA
                    }
                    else -> TXT_NA
                }

        // App Settings
        val tableModel = pnlAppSettings.table.model as DefaultTableModel
        tableModel.dataVector.removeAllElements()
        pnlAppSettings.cachedAppSettings.clear()
        pnlAppSettings.table.emptyText.text = TABLE_EMPTY_MESSAGE
        val appSettings = property.getValue(WebAppBasePropertyViewPresenter.KEY_APP_SETTING) as? Map<*, *>
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
        val controls = arrayOf<JComponent>(
                txtResourceGroup,
                txtStatus,
                txtLocation,
                txtSubscription,
                txtAppServicePlan,
                txtPricingTier,
                txtOperatingSystem,
                txtRuntime
        )

        controls.forEach { control ->
            control.border = BorderFactory.createEmptyBorder()
            control.background = null
        }
    }

    private fun initButtons() {
        btnGetPublishFile.addActionListener(object : AzureActionListenerWrapper(INSIGHT_NAME, "btnGetPublishFile", null) {
            override fun actionPerformedFunc(event: ActionEvent) {
                val fileChooserDescriptor = FileChooserDescriptor(
                        false,
                        true,
                        false,
                        false,
                        false,
                        false
                )
                fileChooserDescriptor.title = FILE_SELECTOR_TITLE
                val file = FileChooser.chooseFile(fileChooserDescriptor, null, null)
                        ?: return

                presenter.onGetPublishingProfileXmlWithSecrets(subscriptionId, resourceId, slotName, file.path)
            }
        })

        btnDiscard.isEnabled = false
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

        btnSave.isEnabled = false
        btnSave.addActionListener(object : AzureActionListenerWrapper(INSIGHT_NAME, "btnSave", null) {
            override fun actionPerformedFunc(event: ActionEvent) {
                setBtnEnableStatus(false)
                presenter.onUpdateWebAppProperty(subscriptionId, resourceId, slotName, pnlAppSettings.cachedAppSettings, pnlAppSettings.editedAppSettings)
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
