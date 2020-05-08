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

package com.microsoft.intellij.helpers.validator

import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import org.jetbrains.mock.PublishableProjectMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.SkipException
import org.testng.annotations.Test

class ProjectValidatorTest {

    //region Project for Web App

    @Test(description = "Verify a valid case for every OS (dotnet core web project)")
    fun testValidateProjectForWebApp_IsSet() {
        val validationResult = ProjectValidator.validateProjectForWebApp(
                PublishableProjectMock.createMockProject(isWeb = true, isDotNetCore = true))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateProjectForWebApp_NotWeb() {
        val mockProject = PublishableProjectMock.createMockProject(isWeb = false, isDotNetCore = true)
        val validationResult = ProjectValidator.validateProjectForWebApp(mockProject)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.project.publish_not_supported", mockProject.projectName))
    }

    @Test
    fun testValidateProjectForWebApp_PublishNotSupported_NetFramework() {
        if (SystemInfo.isWindows)
            throw SkipException("Skip test on Windows instances since publish any web apps is supported on full .NET Framework.")

        val mockProject = PublishableProjectMock.createMockProject(isWeb = true, isDotNetCore = false)
        val validationResult = ProjectValidator.validateProjectForWebApp(mockProject)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.project.publish_os_not_supported", mockProject.projectName, SystemInfo.OS_NAME))
    }

    @Test
    fun testValidateProjectForWebApp_PublishNotSupported_NoWebPublishTargets() {
        if (SystemInfo.isMac || SystemInfo.isLinux)
            throw SkipException("Skip test on non-Windows instances since check for web publish targets is proceed on " +
                    "full .NET Framework Apps which are supported on Windows OS only.")

        val mockProject = PublishableProjectMock.createMockProject(isWeb = true, isDotNetCore = false, hasWebPublishTarget = false)
        val validationResult = ProjectValidator.validateProjectForWebApp(mockProject)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.project.target_not_defined", mockProject.projectName))
    }

    @Test
    fun testValidateProjectForWebApp_IsNotSet() {
        val validationResult = ProjectValidator.validateProjectForWebApp(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.project.not_defined"))
    }

    //endregion Project for Web App

    //region Project for Function App

    @Test
    fun testValidateProjectForFunctionApp_IsSet() {
        val validationResult = ProjectValidator.validateProjectForFunctionApp(
                PublishableProjectMock.createMockProject(isAzureFunction = true))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateProjectForFunctionApp_PublishNotSupported() {
        val mockProject = PublishableProjectMock.createMockProject(isAzureFunction = false)
        val validationResult = ProjectValidator.validateProjectForFunctionApp(mockProject)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.project.not_function_app", mockProject.projectName))
    }

    @Test
    fun testValidateProjectForFunctionApp_IsNotSet() {
        val validationResult = ProjectValidator.validateProjectForFunctionApp(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.project.not_defined"))
    }

    //endregion Project for Function App
}
