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

package com.microsoft.azure.toolkit.lib.common.operation;

import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.Objects;

@Getter
@Builder
public class AzureOperationRef {
    private final Method method;
    private final String[] paramNames;
    private final Object[] paramValues;
    private final Object instance;

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AzureOperationRef)) {
            return false;
        }
        final AzureOperationRef operation = (AzureOperationRef) obj;
        return operation.getMethod() == this.method || Objects.equals(operation.getMethod(), this.getMethod());
    }

    @Override
    public String toString() {
        final AzureOperation annotation = AzureOperationUtils.getAnnotation(this);
        return String.format("{title:'%s', method:%s}", annotation.value(), method.getName());
    }
}
