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

        if (name.length < nameLengthMin || name.length > nameLengthMax)
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