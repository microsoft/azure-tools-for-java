/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.sqlbigdata.serverexplore.ui

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.sdk.cluster.SparkClusterType
import com.microsoft.azure.hdinsight.sdk.common.AuthType
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterModel
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewClusterForm
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule
import org.apache.commons.lang3.StringUtils
import java.awt.CardLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class AddNewSqlBigDataClusterForm(project: Project, module: SqlBigDataClusterModule): AddNewClusterForm(project, module) {
    private val IPV4_ADDRESS_PATTERN = "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"

    init {
        title = "Link SQL Server Big Data Cluster"

        linkResourceTypeLabel.isVisible = false
        clusterComboBox.isVisible = false

        val livyServiceTitle = "Livy Service"
        clusterComboBox.model = DefaultComboBoxModel(arrayOf(livyServiceTitle))
        clusterComboBox.isEnabled = true
        val clusterLayout = clusterCardsPanel.layout as CardLayout
        clusterLayout.show(clusterCardsPanel, "Aris Livy Service")

        authComboBox.model = DefaultComboBoxModel(arrayOf(AuthType.BasicAuth))
        authComboBox.isEnabled = true
        val authLayout = authCardsPanel.layout as CardLayout
        authLayout.show(authCardsPanel, AuthType.BasicAuth.typeName)
    }

    override fun getData(data: AddNewClusterModel) {
        data.apply {
            sparkClusterType = SparkClusterType.SQL_BIG_DATA_CLUSTER
            host = arisHostField.text.trim()
            knoxPort = arisPortField.text.trim().toInt()
            clusterName = arisClusterNameField.text.trim()
            clusterNameLabelTitle = livyClusterNameLabel.text
            userNameLabelTitle = userNameLabel.text
            passwordLabelTitle = passwordLabel.text
            userName = userNameField.text.trim()
            password = passwordField.text.trim()
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return arisHostField;
    }

    override fun getSparkClusterType(): SparkClusterType {
        return SparkClusterType.SQL_BIG_DATA_CLUSTER
    }

    override fun validateBasicInputs() {
        validationErrorMessageField.text =
                if (StringUtils.isBlank(arisHostField.text)) {
                    "Server can't be empty"
                } else if (!IPV4_ADDRESS_PATTERN.toRegex().matches(arisHostField.text)) {
                    "Server format is not valid"
                } else if (ctrlProvider.doeshostExistInSqlBigDataClusters(arisHostField.text)) {
                    "Server already exists in linked clusters"
                } else if (StringUtils.isNotBlank(arisClusterNameField.text) &&
                    ctrlProvider.doesClusterNameExistInSqlBigDataClusters(arisClusterNameField.text)) {
                    "Cluster name already exists in linked clusters"
                } else if (StringUtils.isBlank(userNameField.text) || StringUtils.isBlank(passwordField.text)) {
                    "Username and password can't be empty in Basic Authentication"
                } else {
                    null
                }

        okAction.isEnabled = StringUtils.isEmpty(validationErrorMessageField.text)
    }
}