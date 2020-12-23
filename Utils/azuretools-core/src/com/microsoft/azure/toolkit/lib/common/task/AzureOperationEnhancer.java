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

package com.microsoft.azure.toolkit.lib.common.task;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitOperationException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import lombok.extern.java.Log;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;

@Aspect
@Log
public final class AzureOperationEnhancer {

    @Pointcut("execution(@com.microsoft.azure.toolkit.lib.common.operation.AzureOperation * *..*.*(..))")
    public void operation() {
    }

    @Before("operation()")
    public void enterOperation(JoinPoint point) {
        final AzureOperationRef operation = toOperationRef(point);
        log.info(String.format("enter operation[%s] in context[%s]", operation, AzureTaskContext.current()));
        AzureTaskContext.current().pushOperation(operation);
    }

    @AfterReturning("operation()")
    public void exitOperation(JoinPoint point) {
        final AzureOperationRef operation = toOperationRef(point);
        log.info(String.format("exit operation[%s] in context[%s]", operation, AzureTaskContext.current()));
        final AzureOperationRef popped = AzureTaskContext.current().popOperation();
        assert Objects.equals(popped, operation) : String.format("popped operation[%s] is not the exiting operation[%s]", popped, operation);
    }

    @AfterThrowing(pointcut = "operation()", throwing = "e")
    public void onOperationException(JoinPoint point, Throwable e) throws Throwable {
        this.exitOperation(point);
        if (!(e instanceof RuntimeException)) {
            throw e; // Do not handle checked exception
        }
        final AzureOperationRef operation = toOperationRef(point);
        throw new AzureToolkitOperationException(operation, e);
    }

    private static AzureOperationRef toOperationRef(JoinPoint point) {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Object[] args = point.getArgs();
        final Object instance = point.getThis();
        return AzureOperationRef.builder()
                                .instance(instance)
                                .method(signature.getMethod())
                                .paramNames(signature.getParameterNames())
                                .paramValues(args)
                                .build();
    }
}
