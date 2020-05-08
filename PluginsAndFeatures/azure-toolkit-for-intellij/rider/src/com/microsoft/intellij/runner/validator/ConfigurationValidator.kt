/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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
import com.intellij.openapi.options.ConfigurationException
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.intellij.helpers.validator.ValidationResult
import org.jetbrains.plugins.azure.RiderAzureBundle

open class ConfigurationValidator {

    /**
     * Check whether user is signed in to Azure account
     *
     * @throws [ConfigurationException] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    fun validateAzureAccountIsSignedIn() {
        try {
            if (!AuthMethodManager.getInstance().isSignedIn) {
                val message = RiderAzureBundle.message("run_config.publish.validation.sign_in.required")
                throw RuntimeConfigurationError(message)
            }
        } catch (e: Throwable) {
            throw RuntimeConfigurationError(RiderAzureBundle.message("run_config.publish.validation.sign_in.required"))
        }
    }

    @Throws(RuntimeConfigurationError::class)
    fun checkStatus(status: ValidationResult): ValidationResult {
        if (status.isValid) return status
        throw RuntimeConfigurationError(status.errors.first())
    }
}
