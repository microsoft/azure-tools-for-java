/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.webappconfig.validator

import com.intellij.execution.configurations.RuntimeConfigurationError

open class ConfigurationValidator {

    /**
     * Validate Azure resource name against Azure requirements
     * Please see for details - https://docs.microsoft.com/en-us/azure/architecture/best-practices/naming-conventions
     *
     * @throws [RuntimeConfigurationError] in case name does not match requirements
     */
    @Throws(RuntimeConfigurationError::class)
    fun validateResourceName(name: String,
                             nameLengthMin: Int,
                             nameLengthMax: Int,
                             nameLengthErrorMessage: String,
                             nameRegex: Regex,
                             nameInvalidCharsMessage: String) {

        validateResourceNameRegex(name, nameRegex, nameInvalidCharsMessage)

        if (name.length !in nameLengthMin..nameLengthMax)
            throw RuntimeConfigurationError(nameLengthErrorMessage)
    }

    @Throws(RuntimeConfigurationError::class)
    fun validateResourceNameRegex(name: String,
                                  nameRegex: Regex,
                                  nameInvalidCharsMessage: String) {
        val matches = nameRegex.findAll(name)
        if (matches.count() > 0) {
            val invalidChars = matches.map { it.value }.distinct().joinToString("', '", "'", "'")
            throw RuntimeConfigurationError(String.format(nameInvalidCharsMessage, invalidChars))
        }
    }

    /**
     * Validate the field is set in a configuration
     *
     * @param value filed value to validate
     * @param message failure message to show to a user
     *
     * @throws [RuntimeConfigurationError] if field value is not set
     */
    @Throws(RuntimeConfigurationError::class)
    fun checkValueIsSet(value: String, message: String) {
        if (value.isEmpty()) throw RuntimeConfigurationError(message)
    }
}