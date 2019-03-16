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
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import rx.Observable

class FunctionImpl(private val parent: FunctionApp,
                   private val name: String,
                   private val id: String,
                   private val resourceGroupName: String,
                   private val regionName: String,
                   private val isEnabled: Boolean) : Function {

    override fun name() = name
    override fun id() = id
    override fun inner(): SiteInner? = null

    override fun manager(): AppServiceManager? = null

    override fun region(): Region = Region.fromName(regionName)

    override fun key(): String? = null

    override fun regionName() = regionName
    override fun tags(): MutableMap<String, String> = mutableMapOf()

    override fun resourceGroupName() = resourceGroupName

    override fun type() = ""

    override fun refreshAsync(): Observable<Function>? = null

    override fun refresh(): Function = this

    override fun isEnabled() = isEnabled

    override fun parent() = parent
}
