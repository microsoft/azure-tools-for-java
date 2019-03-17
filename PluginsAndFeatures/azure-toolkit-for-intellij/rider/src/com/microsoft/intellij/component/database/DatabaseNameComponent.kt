/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.component.database

import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.intellij.component.AzureComponent
import com.microsoft.intellij.component.AzureResourceNameComponent
import com.microsoft.intellij.component.extension.initValidationWithResult
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.ValidationResult

class DatabaseNameComponent(private val lifetime: Lifetime) :
        AzureResourceNameComponent("Database Name"),
        AzureComponent {

    init {
        initComponentValidation()
    }

    override fun initComponentValidation() {
        txtNameValue.initValidationWithResult(
                lifetime = lifetime.createNested(),
                textChangeValidationAction = { SqlDatabaseValidator.checkInvalidCharacters(txtNameValue.text) },
                focusLostValidationAction = { ValidationResult() }
        )
    }
}
