/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.utils;

import com.microsoft.azure.functions.annotation.CustomBinding;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationUtils {

    public static Map<String, Object> resolveAnnotationProperties(IAnnotationBinding annotationBinding, List<String> requiredProperties) {
        Map<String, Object> properties = new HashMap<>();
        if (requiredProperties != null) {
            Arrays.stream(annotationBinding.getAllMemberValuePairs()).forEach(t -> {
                if (requiredProperties.contains(t.getName())) {
                    System.out.printf("%s = %s\n", t.getName(), t.getValue());
                    properties.put(t.getName(), calculateJdtValue(t.getValue()));
                }
            });
        }
        Arrays.stream(annotationBinding.getDeclaredMemberValuePairs()).forEach(t -> {
            if (!properties.containsKey(t.getName())) {
                System.out.printf("%s = %s\n", t.getName(), t.getValue());
                properties.put(t.getName(), calculateJdtValue(t.getValue()));
            }
        });
        return properties;
    }

    public static IAnnotationBinding getCustomBindingAnnotation(ITypeBinding typeBinding) {
        if (typeBinding == null) {
            return null;
        }

        return AnnotationUtils.findAnnotation(typeBinding.getAnnotations(), CustomBinding.class);
    }

    public static boolean isCustomBinding(IAnnotationBinding annotationBinding) {
        return annotationBinding != null && annotationBinding.getAnnotationType() != null &&
                StringUtils.equals(CustomBinding.class.getCanonicalName(), annotationBinding.getAnnotationType().getBinaryName());
    }

    public static Map<String, Object> resolveAllAnnotationProperties(IAnnotationBinding annotationBinding) {
        Map<String, Object> properties = new HashMap<>();
        Arrays.stream(annotationBinding.getAllMemberValuePairs()).forEach(t -> properties.put(t.getName(), calculateJdtValue(t.getValue())));
        return properties;
    }

    public static Object resolveAnnotationProperty(IAnnotationBinding annotationBinding, String key, boolean includeDefaultValue) {
        return Arrays.stream(includeDefaultValue ? annotationBinding.getAllMemberValuePairs() : annotationBinding.getDeclaredMemberValuePairs())
                .filter(t -> t != null && StringUtils.equals(key, t.getName())).map(t -> calculateJdtValue(t.getValue())).findFirst().orElse(null);
    }

    public static IAnnotationBinding findAnnotation(IAnnotationBinding[] annotations, Class<?> className) {
        return Arrays.stream(annotations).filter(t -> t != null && StringUtils.equals(className.getCanonicalName(), t.getAnnotationType().getBinaryName())).findFirst().orElse(null);
    }

    @Nonnull
    public static String getDeclaredStringAttributeValue(IAnnotationBinding annotation, String key) {
        return getDeclaredStringAttributeValue(annotation, key, true);
    }

    @Nonnull
    public static String getDeclaredStringAttributeValue(IAnnotationBinding annotation, String key, boolean includeDefaultValue) {
        Object obj = resolveAnnotationProperty(annotation, key, includeDefaultValue);
        if (obj != null && !(obj instanceof String)) {
            throw new AzureToolkitRuntimeException(String.format("Unexpected key '%s' with type '%s'", key, obj.getClass().getSimpleName()));
        } else {
            assert obj != null;
            return (String) obj;
        }
    }

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
