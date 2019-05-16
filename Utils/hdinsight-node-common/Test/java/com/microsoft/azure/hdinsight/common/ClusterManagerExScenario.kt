/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.azure.hdinsight.common

import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail
import com.microsoft.azuretools.authmanage.SubscriptionManager
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail
import com.microsoft.azuretools.sdkmanage.AzureManager
import cucumber.api.DataTable
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.util.*

class ClusterManagerExScenario {
    data class SimpleCluster(val name: String,
                             val storageAccount: String,
                             val storageKey: String,
                             val username: String,
                             val password: String,
                             val subscription: String)

    data class SimpleSubscription(val name: String,
                                  val isSelected: Boolean)

    private var clusterMagr: ClusterManagerEx? = null
    private var additionalClusters: List<HDInsightAdditionalClusterDetail> = ArrayList()
    private var emulatedClusters: List<EmulatorClusterDetail> = ArrayList()
    private var subscriptionClusters: List<ClusterDetail> = ArrayList()
    private var selectedSubscriptions = mapOf<String, SubscriptionDetail>()

    @Before
    fun setUp() {
        clusterMagr = mock(ClusterManagerEx::class.java, CALLS_REAL_METHODS)
    }

    @Given("^Linked HDInsight clusters are:$")
    fun initLinkedClusters(clusterDetails: DataTable) {
        additionalClusters = clusterDetails.asList(SimpleCluster::class.java)
                .map {
                    val clusterMock = mock(HDInsightAdditionalClusterDetail::class.java, CALLS_REAL_METHODS)
                    doReturn(it.name).`when`(clusterMock).name
                    doReturn(it.username).`when`(clusterMock).httpUserName
                    doReturn(it.password).`when`(clusterMock).httpPassword

                    clusterMock
                }

        doReturn(additionalClusters).`when`(clusterMagr!!).loadAdditionalClusters()
    }

    @Given("^emulated HDInsight clusters are:$")
    fun initEmulatedLinkedClusters(clusterDetails: DataTable) {
        emulatedClusters = clusterDetails.asList(SimpleCluster::class.java)
                .map {
                    val clusterMock = mock(EmulatorClusterDetail::class.java, CALLS_REAL_METHODS)
                    doReturn(it.name).`when`(clusterMock).name
                    doReturn(it.username).`when`(clusterMock).httpUserName
                    doReturn(it.password).`when`(clusterMock).httpPassword

                    clusterMock
                }

        doReturn(emulatedClusters).`when`(clusterMagr!!).emulatorClusters
    }


    @Given("^in subscription HDInsight clusters are:$")
    fun initSubscriptionClusters(clusterDetails: DataTable) {
        // create AzureManager mock instance
        val azureMgrMock = mock(AzureManager::class.java)
        doReturn(azureMgrMock).`when`(clusterMagr!!).azureManager

        // create SubscriptionManager mock instance
        val subscriptionManagerMock = mock(SubscriptionManager::class.java)
        Mockito.`when`(azureMgrMock.subscriptionManager).thenReturn(subscriptionManagerMock)

        Mockito.`when`(subscriptionManagerMock.selectedSubscriptionDetails).thenReturn(selectedSubscriptions.values.toList())

        subscriptionClusters = clusterDetails.asList(SimpleCluster::class.java)
                .map {
                    val clusterMock = mock(ClusterDetail::class.java, CALLS_REAL_METHODS)
                    doReturn(it.name).`when`(clusterMock).name
                    doReturn(it.username).`when`(clusterMock).httpUserName
                    doReturn(it.password).`when`(clusterMock).httpPassword
                    // FIXME: Hardcoded for spark version
                    doReturn("2.2").`when`(clusterMock).sparkVersion
                    doReturn(selectedSubscriptions[it.subscription]).`when`(clusterMock).subscription
                    doReturn(false).`when`(clusterMock).isRoleTypeReader
                    doReturn("Running").`when`(clusterMock).state

                    clusterMock
                }

        doReturn(Optional.of(subscriptionClusters)).`when`(clusterMagr!!)
                .getSubscriptionHDInsightClustersOfType(selectedSubscriptions.values.toList())
    }

    @Given("^subscriptions mocked are:$")
    fun mockSubscriptions(subscriptionsMock: DataTable) {
        selectedSubscriptions = subscriptionsMock.asList(SimpleSubscription::class.java)
                .map {
                    val subMock = mock(SubscriptionDetail::class.java)
                    Mockito.`when`(subMock.subscriptionName).thenReturn(it.name)
                    Mockito.`when`(subMock.isSelected).thenReturn(it.isSelected)

                    it.name to subMock
                }
                .filter { it.second.isSelected }
                .toMap()

    }

    @Given("^check get all Cluster details should be:$")
    fun checkGetClusterDetails(clusterDetailsExpect: List<String>) {
        assertThat(clusterMagr!!.clusterDetails).extracting("title")
                .containsAll(clusterDetailsExpect)
    }
}