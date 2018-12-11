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
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterModel
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewClusterForm
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule
import org.apache.commons.lang3.StringUtils
import java.awt.CardLayout
import java.net.URI
import javax.swing.DefaultComboBoxModel

class AddNewSqlBigDataClusterForm(project: Project, module: SqlBigDataClusterModule): AddNewClusterForm(project, module) {
    init {
        title = "Link SQL Big Data Cluster"

        val livyServiceTitle = "Livy Service"
        clusterComboBox.model = DefaultComboBoxModel(arrayOf(livyServiceTitle))
        clusterComboBox.isEnabled = true
        val clusterLayout = clusterCardsPanel.layout as CardLayout
        clusterLayout.show(clusterCardsPanel, livyServiceTitle)

        val basicAuthTitle = "Basic Authentication"
        authComboBox.model = DefaultComboBoxModel(arrayOf(basicAuthTitle))
        authComboBox.isEnabled = true
        val authLayout = authCardsPanel.layout as CardLayout
        authLayout.show(authCardsPanel, basicAuthTitle)
    }

    override fun getData(data: AddNewClusterModel) {
        data.apply {
            sparkClusterType = SparkClusterType.SQL_BIG_DATA_CLUSTER
            livyEndpoint = URI.create(livyEndpointField.text.trim())
            yarnEndpoint = if (StringUtils.isBlank(yarnEndpointField.text.trim())) null else URI.create(yarnEndpointField.text.trim())
            clusterName = livyClusterNameField.text.trim()
            clusterNameLabelTitle = livyClusterNameLabel.text
            userNameLabelTitle = userNameLabel.text
            passwordLabelTitle = passwordLabel.text
            userName = userNameField.text.trim()
            password = passwordField.text.trim()
        }
    }

    override fun getSparkClusterType(): SparkClusterType {
        return SparkClusterType.SQL_BIG_DATA_CLUSTER
    }

    override fun basicValidate() {
        errorMessageField.text =
                if (StringUtils.isBlank(livyEndpointField.text) || StringUtils.isBlank(livyClusterNameField.text)) {
                    "Livy Endpoint and cluster name can't be empty"
                } else if (!ctrlProvider.isURLValid(livyEndpointField.text)) {
                    "Livy Endpoint is not a valid URL"
                } else if (ctrlProvider.doesClusterLivyEndpointExistInSqlBigDataClusters(livyEndpointField.text)) {
                    "The same name Livy Endpoint already exists in clusters"
                } else if (ctrlProvider.doesClusterNameExistInSqlBigDataClusters(livyClusterNameField.text)) {
                    "Cluster Name already exists in clusters"
                } else if (!StringUtils.isEmpty(yarnEndpointField.text) && !ctrlProvider.isURLValid(yarnEndpointField.text)) {
                    "Yarn Endpoint is not a valid URL"
                } else if (StringUtils.isBlank(userNameField.text) || StringUtils.isBlank(passwordField.text)) {
                    "Username and password can't be empty in Basic Authentication"
                } else {
                    null
                }

        okAction.isEnabled = StringUtils.isEmpty(errorMessageField.text)
    }
}