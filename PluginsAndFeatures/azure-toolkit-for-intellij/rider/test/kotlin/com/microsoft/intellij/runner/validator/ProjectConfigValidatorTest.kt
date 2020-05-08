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

package com.microsoft.intellij.runner.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeTrue
import org.jetbrains.mock.PublishableProjectMock
import org.testng.annotations.Test

class ProjectConfigValidatorTest {

    @Test
    fun testValidateProjectForWebApp_IsSet() {
        val mockProject = PublishableProjectMock.createMockProject(
                name = "TestProject",
                isWeb = true,
                isDotNetCore = true
        )

        val validationResult = ProjectConfigValidator.validateProjectForWebApp(mockProject)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not defined.")
    fun testValidateProjectForWebApp_IsNotSet() {
        ProjectConfigValidator.validateProjectForWebApp(null)
    }

    @Test
    fun testValidateProjectForFunctionApp_IsSet() {
        val mockProject = PublishableProjectMock.createMockProject(
                name = "TestProject",
                isAzureFunction = true
        )

        val validationResult = ProjectConfigValidator.validateProjectForFunctionApp(mockProject)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not defined.")
    fun testValidateProjectForFunctionApp_IsNotSet() {
        ProjectConfigValidator.validateProjectForFunctionApp(null)
    }
}
