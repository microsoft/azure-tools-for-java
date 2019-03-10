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

package com.microsoft.azuretools.core.mvp.model.functionapp

import com.microsoft.azure.Page
import com.microsoft.azure.PagedList
import com.microsoft.azure.management.Azure
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.appservice.FunctionApps
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.implementation.AppServiceManager
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.authmanage.SubscriptionManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.sdkmanage.AzureManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import kotlin.test.assertEquals

@RunWith(PowerMockRunner::class)
@PrepareForTest(
        AuthMethodManager::class,
        AzureManager::class,
        Azure::class,
        FunctionApps::class,
        SubscriptionManager::class,
        AppServiceManager::class,
        AzureMvpModel::class,
        AzureFunctionAppMvpModel::class
)

class AzureFunctionAppMvpModelTest {

    companion object {
        private const val MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000"
        private const val MOCK_EXISTING_RESOURCE_GROUP = "ExistingResourceGroupName"
    }

    @Mock private val authMethodManagerMock: AuthMethodManager? = null
    @Mock private val azureMock: Azure? = null
    @Mock private val azureManagerMock: AzureManager? = null
    @Mock private val subscriptionManagerMock: SubscriptionManager? = null
    @Mock private val functionAppsMock: FunctionApps? = null
    @Mock private val appServiceManagerMock: AppServiceManager? = null
    @Mock private val azureMvpModelMock: AzureMvpModel? = null

    @Before
    fun setUp() {
        PowerMockito.mockStatic(AuthMethodManager::class.java)
        PowerMockito.mockStatic(AzureMvpModel::class.java)
        PowerMockito.mockStatic(AzureFunctionAppMvpModel::class.java)
        Mockito.`when`(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock)
        Mockito.`when`(authMethodManagerMock?.getAzureClient(MOCK_SUBSCRIPTION)).thenReturn(azureMock)
        Mockito.`when`(authMethodManagerMock?.azureManager).thenReturn(azureManagerMock)
        Mockito.`when`(azureManagerMock?.subscriptionManager).thenReturn(subscriptionManagerMock)
        Mockito.`when`(azureMock?.appServices()).thenReturn(appServiceManagerMock)
        Mockito.`when`(appServiceManagerMock?.functionApps()).thenReturn(functionAppsMock)
        Mockito.`when`(AzureMvpModel.getInstance()).thenReturn(azureMvpModelMock)
    }

    @After
    fun tearDown() {
        Mockito.reset(functionAppsMock)
        Mockito.reset(azureMock)
        Mockito.reset(azureMvpModelMock)
        Mockito.reset(authMethodManagerMock)
        AzureFunctionAppMvpModel.clearSubscriptionIdToFunctionMap()
    }

    @Test
    fun testListAllFunctionApps_ValuesNotCached_NotForced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        val subscriptions = prepareMockSubscriptions(1)
        Mockito.`when`(azureMvpModelMock?.selectedSubscriptions).thenReturn(subscriptions)
        Mockito.`when`(authMethodManagerMock?.getAzureClient(ArgumentMatchers.anyString())).thenReturn(azureMock)

        val resultList = AzureFunctionAppMvpModel.listAllFunctionApps(force = false)

        Mockito.verify(functionAppsMock, Mockito.times(1))?.list()
        assertEquals(1, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testListAllFunctionApps_ValuesNotCached_Forced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        val subscriptions = prepareMockSubscriptions(1)
        Mockito.`when`(azureMvpModelMock?.selectedSubscriptions).thenReturn(subscriptions)
        Mockito.`when`(authMethodManagerMock?.getAzureClient(ArgumentMatchers.anyString())).thenReturn(azureMock)

        val resultList = AzureFunctionAppMvpModel.listAllFunctionApps(force = true)

        Mockito.verify(functionAppsMock, Mockito.times(1))?.list()
        assertEquals(1, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testListAllFunctionApps_ValuesCached_NotForced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        val subscriptions = prepareMockSubscriptions(1)
        Mockito.`when`(azureMvpModelMock?.selectedSubscriptions).thenReturn(subscriptions)
        Mockito.`when`(authMethodManagerMock?.getAzureClient(ArgumentMatchers.anyString())).thenReturn(azureMock)

        AzureFunctionAppMvpModel.listAllFunctionApps(force = false)
        val resultList = AzureFunctionAppMvpModel.listAllFunctionApps(force = false)

        Mockito.verify(functionAppsMock, Mockito.times(1))?.list()
        assertEquals(1, resultList.size, "Mismatch Azure Function Apps number")
        assertEquals("1", resultList.first().subscriptionId)
    }

    @Test
    fun testListAllFunctionApps_ValuesCached_Forced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        val subscriptions = prepareMockSubscriptions(1)
        Mockito.`when`(azureMvpModelMock?.selectedSubscriptions).thenReturn(subscriptions)
        Mockito.`when`(authMethodManagerMock?.getAzureClient(ArgumentMatchers.anyString())).thenReturn(azureMock)

        AzureFunctionAppMvpModel.listAllFunctionApps(force = true)
        val resultList = AzureFunctionAppMvpModel.listAllFunctionApps(force = true)

        Mockito.verify(functionAppsMock, Mockito.times(2))?.list()
        assertEquals(1, resultList.size, "Mismatch Azure Function Apps number")
        assertEquals("1", resultList.first().subscriptionId)
    }

    @Test
    fun testListAllFunctionApps_MultipleSubscriptions_NotForced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        val subscriptions = prepareMockSubscriptions(2)
        Mockito.`when`(azureMvpModelMock?.selectedSubscriptions).thenReturn(subscriptions)
        Mockito.`when`(authMethodManagerMock?.getAzureClient(ArgumentMatchers.anyString())).thenReturn(azureMock)

        val resultList = AzureFunctionAppMvpModel.listAllFunctionApps(force = false)

        Mockito.verify(functionAppsMock, Mockito.times(2))?.list()
        assertEquals(2, resultList.size, "Mismatch Azure Function Apps number")
        assertEquals("1", resultList[0].subscriptionId)
        assertEquals("2", resultList[1].subscriptionId)
    }

    @Test
    fun testListFunctionAppsBySubscriptionId_ValuesNotCached_NotForced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        val resultList = AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = false)

        assertEquals(2, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testListFunctionAppsBySubscriptionId_ValuesNotCached_Forced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        val resultList = AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = true)

        assertEquals(2, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testListFunctionAppsBySubscriptionId_ValuesCached_NotForced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = false)

        val resultList = AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = false)

        Mockito.verify(functionAppsMock, Mockito.times(1))?.list()
        assertEquals(2, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testListFunctionAppsBySubscriptionId_ValuesCached_Forced() {
        val functionAppList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX))
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = true)

        val resultList = AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = true)

        Mockito.verify(functionAppsMock, Mockito.times(2))?.list()
        assertEquals(2, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testListFunctionAppsBySubscriptionId_EmptyFunctionApps_NotForced() {
        val functionAppList = emptyList<FunctionApp>() as? PagedList
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = false)

        val resultList = AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = false)

        Mockito.verify(functionAppsMock, Mockito.times(1))?.list()
        assertEquals(0, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testListFunctionAppsBySubscriptionId_EmptyFunctionApps_Forced() {
        val functionAppList = emptyList<FunctionApp>() as? PagedList
        Mockito.`when`(functionAppsMock?.list()).thenReturn(functionAppList)

        AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = true)

        val resultList = AzureFunctionAppMvpModel
                .listFunctionAppsBySubscriptionId(subscriptionId = MOCK_SUBSCRIPTION, force = true)

        Mockito.verify(functionAppsMock, Mockito.times(2))?.list()
        assertEquals(0, resultList.size, "Mismatch Azure Function Apps number")
    }

    @Test
    fun testGetFunctionAppsBySubscriptionId_ExistingFunctionApps() {
        val functionAppPageList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX))
        Mockito.`when`<PagedList<FunctionApp>>(functionAppsMock?.list()).thenReturn(functionAppPageList)

        val functionAppList = AzureFunctionAppMvpModel.getAzureFunctionAppsBySubscriptionId(MOCK_SUBSCRIPTION)
        assertEquals(2, functionAppList.size, "Mismatch Function Apps number")
    }

    @Test
    fun testGetFunctionAppsBySubscriptionId_EmptyFunctionApps() {
        val emptyFunctionAppPageList = emptyList<FunctionApp>() as? PagedList
        Mockito.`when`<PagedList<FunctionApp>>(functionAppsMock?.list()).thenReturn(emptyFunctionAppPageList)

        val functionAppList = AzureFunctionAppMvpModel.getAzureFunctionAppsBySubscriptionId(MOCK_SUBSCRIPTION)
        assertEquals(0, functionAppList.size, "Mismatch Function Apps number")
    }

    @Test
    fun testGetFunctionAppsBySubscriptionId_Invocations() {
        val functionAppPageList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX))
        Mockito.`when`<PagedList<FunctionApp>>(functionAppsMock?.list()).thenReturn(functionAppPageList)

        AzureFunctionAppMvpModel.getAzureFunctionAppsBySubscriptionId(MOCK_SUBSCRIPTION)
        Mockito.verify(functionAppsMock, Mockito.times(1))?.list()
    }

    @Test
    fun testGetAzureFunctionAppsByResourceGroup_ExistingFunctionApps() {
        val functionAppPageList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS))
        Mockito.`when`<PagedList<FunctionApp>>(functionAppsMock?.listByResourceGroup(MOCK_EXISTING_RESOURCE_GROUP)).thenReturn(functionAppPageList)

        val functionAppList = AzureFunctionAppMvpModel.getAzureFunctionAppsByResourceGroup(MOCK_SUBSCRIPTION, MOCK_EXISTING_RESOURCE_GROUP)
        assertEquals(1, functionAppList.size, "Mismatch Function Apps number")
    }

    @Test
    fun testGetAzureFunctionAppsByResourceGroup_Invocations() {
        val functionAppPageList = prepareMockFunctionAppList(listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX))
        Mockito.`when`<PagedList<FunctionApp>>(functionAppsMock?.listByResourceGroup(MOCK_EXISTING_RESOURCE_GROUP))
                .thenReturn(functionAppPageList)

        AzureFunctionAppMvpModel.getAzureFunctionAppsByResourceGroup(MOCK_SUBSCRIPTION, MOCK_EXISTING_RESOURCE_GROUP)
        Mockito.verify(functionAppsMock, Mockito.times(1))?.listByResourceGroup(MOCK_EXISTING_RESOURCE_GROUP)
    }

    @Test
    fun testStartWebApp() {
        val functionAppId = "testAppId"
        val mockFunctionApp = Mockito.mock(FunctionApp::class.java)
        Mockito.`when`(functionAppsMock?.getById(functionAppId)).thenReturn(mockFunctionApp)

        AzureFunctionAppMvpModel.startFunctionApp(MOCK_SUBSCRIPTION, functionAppId)
        Mockito.verify(mockFunctionApp, Mockito.times(1)).start()
    }

    @Test
    fun testRestartFunctionApp() {
        val functionAppId = "testAppId"
        val mockFunctionApp = Mockito.mock(FunctionApp::class.java)
        Mockito.`when`(functionAppsMock?.getById(functionAppId)).thenReturn(mockFunctionApp)

        AzureFunctionAppMvpModel.restartFunctionApp(MOCK_SUBSCRIPTION, functionAppId)
        Mockito.verify(mockFunctionApp, Mockito.times(1)).restart()
    }

    @Test
    fun testStopFunctionApp() {
        val functionAppId = "testAppId"
        val mockFunctionApp = Mockito.mock(FunctionApp::class.java)
        Mockito.`when`(functionAppsMock?.getById(functionAppId)).thenReturn(mockFunctionApp)

        AzureFunctionAppMvpModel.stopFunctionApp(MOCK_SUBSCRIPTION, functionAppId)
        Mockito.verify(mockFunctionApp, Mockito.times(1)).stop()
    }

    @Test
    fun testDeleteFunctionApp() {
        val functionAppId = "testAppId"

        AzureFunctionAppMvpModel.deleteFunctionApp(MOCK_SUBSCRIPTION, functionAppId)
        Mockito.verify(functionAppsMock, Mockito.times(1))?.deleteById(functionAppId)
    }

    private fun prepareMockSubscriptions(count: Int): List<Subscription> {
        val subscriptionList = ArrayList<Subscription>()

        for (index in 1..count) {
            val subscription = Mockito.mock<Subscription>(Subscription::class.java)
            Mockito.`when`<String>(subscription.subscriptionId()).thenReturn(index.toString())
            subscriptionList.add(subscription)
        }

        return subscriptionList
    }

    private fun prepareMockFunctionAppList(operatingSystems: List<OperatingSystem>): PagedList<FunctionApp> {

        val functionAppList = object : PagedList<FunctionApp>() {
            override fun nextPage(nextPageLink: String): Page<FunctionApp>? = null
        }

        operatingSystems.forEach { os ->
            val functionApp = Mockito.mock<FunctionApp>(FunctionApp::class.java)
            Mockito.`when`<OperatingSystem>(functionApp.operatingSystem()).thenReturn(os)
            functionAppList.add(functionApp)
        }

        return functionAppList
    }
}