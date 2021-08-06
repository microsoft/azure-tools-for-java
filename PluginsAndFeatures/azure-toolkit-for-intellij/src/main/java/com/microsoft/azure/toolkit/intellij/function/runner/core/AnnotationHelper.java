/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.core;

import com.google.common.collect.Lists;
import com.intellij.lang.jvm.types.JvmArrayType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.ClassUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AnnotationHelper {
    public static Map<String, Object> evaluateAnnotationProperties(Project project, PsiAnnotation annotation,
            List<String> requiredProperties) throws AzureExecutionException {
        PsiConstantEvaluationHelper
                evaluationHelper = JavaPsiFacade.getInstance(project).getConstantEvaluationHelper();
        Map<String, Object> properties = new HashMap<>();
        PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
        if (referenceElement != null) {
            PsiElement resolved = referenceElement.resolve();
            if (resolved != null) {
                List<PsiMethod> methods = Arrays.stream(((PsiClass) resolved).getAllMethods())
                                                .filter(PsiUtil::isAnnotationMethod).collect(Collectors.toList());

                for (PsiMethod method : methods) {
                    // we only care user declared or required properties
                    if (annotation.findDeclaredAttributeValue(method.getName()) != null ||
                            (requiredProperties != null && requiredProperties.contains(method.getName()))) {
                        PsiAnnotationMemberValue attrValue = annotation.findAttributeValue(method.getName());
                        Object obj;
                        if (attrValue instanceof PsiAnnotation) {
                            obj = evaluateAnnotationProperties(project, (PsiAnnotation) attrValue, null);
                        } else if (attrValue instanceof PsiArrayInitializerMemberValue) {
                            obj = handleArrayAnnotationValue(evaluationHelper,
                                                             (PsiArrayInitializerMemberValue) attrValue);
                        } else {
                            obj = getPsiAnnotationMemberValue(evaluationHelper, attrValue);
                        }
                        if (method.getReturnType() instanceof JvmArrayType && obj != null && !obj.getClass().isArray()) {
                            obj = new Object[]{obj};
                        }
                        properties.put(method.getName(), obj);
                    }

                }

            }
        }
        return properties;
    }

    private static Object[] handleArrayAnnotationValue(PsiConstantEvaluationHelper helper, PsiArrayInitializerMemberValue value)
            throws AzureExecutionException {
        final PsiAnnotationMemberValue[] initializers = value.getInitializers();
        final List<Object> result = Lists.newArrayListWithCapacity(initializers.length);

        for (final PsiAnnotationMemberValue initializer : initializers) {
            result.add(getPsiAnnotationMemberValue(helper, initializer));
        }
        return result.toArray();
    }

    private static String getEnumFieldString(final String className, final String fieldName)
            throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
        final Class<?> clz = Class.forName(className);
        final Field[] fields = clz.getFields();
        final Optional<Field> targetField = Arrays.stream(fields).filter(field -> field.getName().equals(fieldName)).findFirst();
        if (targetField.isPresent()) {
            return Objects.toString(targetField.get().get(null));
        }
        return null;
    }

    private static String getEnumConstantString(PsiAnnotationMemberValue value) throws AzureExecutionException {
        if (value instanceof PsiReferenceExpression) {
            final PsiReferenceExpression referenceExpression = (PsiReferenceExpression) value;
            final Object resolved = referenceExpression.resolve();
            if (resolved instanceof PsiEnumConstant) {
                final PsiEnumConstant enumConstant = (PsiEnumConstant) resolved;
                final PsiClass enumClass = enumConstant.getContainingClass();
                if (enumClass != null) {
                    try {
                        return getEnumFieldString(enumClass.getQualifiedName(), enumConstant.getName());
                    } catch (ClassNotFoundException | IllegalAccessException e) {
                        throw new AzureExecutionException(e.getMessage(), e);
                    }
                } else {
                    return enumConstant.getName();
                }
            }

        }
        return null;
    }

    private static Object getPsiAnnotationMemberValue(PsiConstantEvaluationHelper helper,
                                                      PsiAnnotationMemberValue value) throws AzureExecutionException {
        if (value == null) {
            return null;
        }

        // annotation only allows primitive type or String or Class or enums

        // 1. enums
        Object obj = getEnumConstantString(value);
        if (obj != null) {
            return obj;
        }

        // 2. String or primitive
        obj = helper.computeConstantExpression(value);
        if (obj == null) {
            return null;
        }

        if (ClassUtils.isPrimitiveOrWrapper(obj.getClass()) || obj instanceof String) {
            return obj;
        }

        // 3. class
        if (obj instanceof PsiClassType) {
            return ((PsiClassType) obj).resolve().getQualifiedName();
        }
        final String error = message("function.annotation.error.invalidAnnotationType",
                                     PsiAnnotation.class.getCanonicalName(), obj.getClass().getCanonicalName());
        throw new AzureExecutionException(error);
    }
}
