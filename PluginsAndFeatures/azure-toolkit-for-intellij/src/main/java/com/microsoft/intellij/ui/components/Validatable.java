/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.components;

import com.intellij.openapi.ui.ValidationInfo;

public interface Validatable {
    ValidationInfo doValidate();
}
