/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.azuretools.core.mvp.model.functionapp.functions

import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.appservice.implementation.AppServiceManager
import com.microsoft.azure.management.appservice.implementation.SiteInner
import com.microsoft.azure.management.resources.fluentcore.arm.models.GroupableResource
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasParent
import com.microsoft.azure.management.resources.fluentcore.model.Refreshable

interface Function :
        HasName,
        GroupableResource<AppServiceManager, SiteInner>,
        Refreshable<Function>,
        HasParent<FunctionApp> {

    // This is currently the placeholder until issue is fixed - https://github.com/Azure/azure-functions-host/issues/2623
    // There is no strict way to define current azure function status
    fun isEnabled(): Boolean
}
