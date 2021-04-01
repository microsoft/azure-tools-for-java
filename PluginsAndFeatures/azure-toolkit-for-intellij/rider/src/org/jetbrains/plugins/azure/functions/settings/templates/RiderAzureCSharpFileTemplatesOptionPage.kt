/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package org.jetbrains.plugins.azure.functions.settings.templates

import com.intellij.openapi.options.Configurable
import com.jetbrains.rider.settings.simple.SimpleOptionsPage

class RiderAzureCSharpFileTemplatesOptionPage: SimpleOptionsPage("Azure (C#)", "RiderAzureCSharpFileTemplatesSettings"), Configurable.NoScroll {
    override fun getId() = pageId + "Id"
}

