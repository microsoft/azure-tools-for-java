/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroupExportResult;
import com.microsoft.azure.management.resources.ResourceGroupExportTemplateOptions;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.implementation.ResourceGroupInner;
import com.microsoft.azure.toolkit.lib.common.OperationNotSupportedException;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rx.Observable;

import java.util.Map;

@Setter
@Builder
public class ResourceGroupMock implements ResourceGroup, Mock {
    @Getter
    private Subscription subscription;
    private String name;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Region region() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String provisioningState() {
        throw new OperationNotSupportedException();
    }

    @Override
    public ResourceGroupExportResult exportTemplate(final ResourceGroupExportTemplateOptions resourceGroupExportTemplateOptions) {
        throw new OperationNotSupportedException();
    }

    @Override
    public Observable<ResourceGroupExportResult> exportTemplateAsync(final ResourceGroupExportTemplateOptions resourceGroupExportTemplateOptions) {
        throw new OperationNotSupportedException();
    }

    @Override
    public ServiceFuture<ResourceGroupExportResult> exportTemplateAsync(final ResourceGroupExportTemplateOptions resourceGroupExportTemplateOptions,
                                                                        final ServiceCallback<ResourceGroupExportResult> serviceCallback) {
        throw new OperationNotSupportedException();
    }

    @Override
    public String type() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String regionName() {
        throw new OperationNotSupportedException();
    }

    @Override
    public Map<String, String> tags() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String id() {
        throw new OperationNotSupportedException();
    }

    @Override
    public ResourceGroupInner inner() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String key() {
        throw new OperationNotSupportedException();
    }

    @Override
    public ResourceGroup refresh() {
        throw new OperationNotSupportedException();
    }

    @Override
    public Observable<ResourceGroup> refreshAsync() {
        throw new OperationNotSupportedException();
    }

    @Override
    public Update update() {
        throw new OperationNotSupportedException();
    }
}
