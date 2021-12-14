/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.util.Arrays;

public class AnnotationUtils {
    public static Object calculateJdtValue(Object value) {
        if (value == null) {
            return null;
        }

        Class<?> clz = value.getClass();
        if (ClassUtils.isPrimitiveOrWrapper(clz) || value instanceof String) {
            return value;
        }
        if (clz.isArray()) {
            Object[] values = (Object[]) value;
            return Arrays.stream(values).map(AnnotationUtils::calculateJdtValue).toArray();
        }
        if (value instanceof IVariableBinding) {
            if (((IVariableBinding) value).isEnumConstant()) {
                return ((IVariableBinding) value).getName();
            }
        }
        throw new AzureToolkitRuntimeException("Cannot evaluate annotation value for type: " + clz.getName());
    }

}
