/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.model

import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import org.jetbrains.mock.DeploymentSlotMock
import org.jetbrains.mock.WebAppMock
import org.testng.annotations.Test

class WebAppPublishModelTest {

    @Test
    fun testResetOnPublish_Defaults() {
        val mockWebApp = WebAppMock(id = "test-web-app-id")

        val model = WebAppPublishModel()
        model.resetOnPublish(mockWebApp)

        model.isCreatingNewApp.shouldBeFalse()
        model.appId.shouldBe(mockWebApp.id())
        model.appName.shouldBeEmpty()

        model.isCreatingResourceGroup.shouldBeFalse()
        model.resourceGroupName.shouldBeEmpty()

        model.isCreatingAppServicePlan.shouldBeFalse()
        model.appServicePlanName.shouldBeEmpty()
    }

    @Test(description = "https://github.com/JetBrains/azure-tools-for-intellij/issues/423")
    fun testResetOnPublish_DeploymentSlot() {
        val mockWebApp = WebAppMock(id = "test-web-app-id")
        val mockDeploymentSlot = DeploymentSlotMock(parent = mockWebApp)

        val model = WebAppPublishModel()
        model.resetOnPublish(mockDeploymentSlot)

        model.isCreatingNewApp.shouldBeFalse()
        model.appId.shouldBe(mockWebApp.id())
        model.appName.shouldBeEmpty()

        model.isCreatingResourceGroup.shouldBeFalse()
        model.resourceGroupName.shouldBeEmpty()

        model.isCreatingAppServicePlan.shouldBeFalse()
        model.appServicePlanName.shouldBeEmpty()
    }
}
