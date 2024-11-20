/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.utils

import com.intellij.openapi.ui.InputValidator

class PreviewPortInputValidator : InputValidator {
    override fun checkInput(input: String?): Boolean {
        return try {
            val port = input?.toIntOrNull()

            port != null && ((port in 1025..8079) || (port in 8091..49151))
        } catch (e: Exception) {
            false
        }
    }

    override fun canClose(input: String?): Boolean {
        return checkInput(input)
    }
}