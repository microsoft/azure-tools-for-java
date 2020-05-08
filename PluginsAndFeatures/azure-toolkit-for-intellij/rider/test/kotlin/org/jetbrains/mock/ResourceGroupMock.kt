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

package org.jetbrains.mock

import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.ResourceGroupExportResult
import com.microsoft.azure.management.resources.ResourceGroupExportTemplateOptions
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.resources.implementation.ResourceGroupInner
import com.microsoft.rest.ServiceCallback
import com.microsoft.rest.ServiceFuture
import rx.Observable

class ResourceGroupMock(
        private val name: String = "test-resource-group"
) : ResourceGroup {

    override fun exportTemplate(p0: ResourceGroupExportTemplateOptions?): ResourceGroupExportResult {
        TODO("Not yet implemented")
    }

    override fun id(): String {
        TODO("Not yet implemented")
    }

    override fun inner(): ResourceGroupInner {
        TODO("Not yet implemented")
    }

    override fun exportTemplateAsync(p0: ResourceGroupExportTemplateOptions?): Observable<ResourceGroupExportResult> {
        TODO("Not yet implemented")
    }

    override fun exportTemplateAsync(p0: ResourceGroupExportTemplateOptions?, p1: ServiceCallback<ResourceGroupExportResult>?): ServiceFuture<ResourceGroupExportResult> {
        TODO("Not yet implemented")
    }

    override fun name(): String = name

    override fun region(): Region {
        TODO("Not yet implemented")
    }

    override fun update(): ResourceGroup.Update {
        TODO("Not yet implemented")
    }

    override fun key(): String {
        TODO("Not yet implemented")
    }

    override fun provisioningState(): String {
        TODO("Not yet implemented")
    }

    override fun refreshAsync(): Observable<ResourceGroup> {
        TODO("Not yet implemented")
    }

    override fun regionName(): String {
        TODO("Not yet implemented")
    }

    override fun tags(): MutableMap<String, String> {
        TODO("Not yet implemented")
    }

    override fun refresh(): ResourceGroup {
        TODO("Not yet implemented")
    }

    override fun type(): String {
        TODO("Not yet implemented")
    }
}
