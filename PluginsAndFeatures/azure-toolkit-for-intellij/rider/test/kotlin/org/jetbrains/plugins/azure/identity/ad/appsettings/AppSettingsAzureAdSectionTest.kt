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

package org.jetbrains.plugins.azure.identity.ad.appsettings

import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import org.testng.annotations.Test

class AppSettingsAzureAdSectionTest {

    @Test
    fun testIsDefaultProjectTemplateContent_AspDotNetCore_Template_Values() {
        val subject = AppSettingsAzureAdSection(
                instance = "https://login.microsoftonline.com/",
                domain = "qualified.domain.name",
                tenantId = "22222222-2222-2222-2222-222222222222",
                clientId = "11111111-1111-1111-11111111111111111",
                callbackPath = "/signin-oidc"
        )

        subject.isDefaultProjectTemplateContent()
                .shouldBeTrue("isDefaultProjectTemplateContent should be true for the AzureAd section from the" +
                        "ASP.NET Core template")
    }

    @Test
    fun testIsDefaultProjectTemplateContent_Customized_Values() {
        val subject = AppSettingsAzureAdSection(
                instance = "https://login.microsoftonline.com/",
                domain = "qualified.domain.name",
                tenantId = "22222222-2222-2222-2222-222222222222",
                clientId = "810f08fd-6cc2-4d4a-be8b-9d61a08a83ef",
                callbackPath = "/signin-here"
        )

        subject.isDefaultProjectTemplateContent()
                .shouldBeTrue("isDefaultProjectTemplateContent should be true for an AzureAd section with domain and " +
                        "tenant from the ASP.NET Core template")
    }

    @Test
    fun testIsDefaultProjectTemplateContent_Configured_Values() {
        val subject = AppSettingsAzureAdSection(
                instance = "https://login.microsoftonline.com/",
                domain = "example.onmicrosoft.com",
                tenantId = "aaaaaaaa-6cc2-4d4a-be8b-9d61a08a83ef",
                clientId = "810f08fd-6cc2-4d4a-be8b-9d61a08a83ef",
                callbackPath = "/signin-here"
        )

        subject.isDefaultProjectTemplateContent()
                .shouldBeFalse("isDefaultProjectTemplateContent should be false for an AzureAd section that has a " +
                        "non-default domain and tenant")
    }
}