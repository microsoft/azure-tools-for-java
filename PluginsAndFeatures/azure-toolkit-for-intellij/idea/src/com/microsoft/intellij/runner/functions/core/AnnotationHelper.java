/**
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.intellij.runner.functions.core;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.JavaConstantExpressionEvaluator;
import com.microsoft.azure.common.exceptions.AzureExecutionException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class AnnotationHelper {

    public static String getJvmAnnotationAttributeValue(JvmAnnotationAttributeValue value) throws AzureExecutionException {
        if (value instanceof JvmAnnotationConstantValue) {
            return Objects.toString(((JvmAnnotationConstantValue) value).getConstantValue(), null);
        }
        if (value instanceof PsiExpression) {
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
            final Object obj = JavaConstantExpressionEvaluator.computeConstantExpression((PsiExpression) value, true);
            return Objects.toString(obj, null);
        } else if (value instanceof PsiLiteral) {
            return Objects.toString(((PsiLiteral) value).getValue(), null);
        }
        throw new AzureExecutionException("Cannot get annotation value of type : " + value.getClass().getName());
    }

    public static String getPsiAnnotationMemberValue(PsiAnnotationMemberValue value) throws AzureExecutionException {
        if (value instanceof PsiExpression) {
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
            final Object obj = JavaConstantExpressionEvaluator.computeConstantExpression((PsiExpression) value, true);
            return Objects.toString(obj, null);
        } else if (value instanceof PsiLiteral) {
            return Objects.toString(((PsiLiteral) value).getValue(), null);
        }
        throw new AzureExecutionException("Cannot get annotation value of type : " + value.getClass().getName());
    }

    private static String getEnumFieldString(final String className, final String fieldName)
            throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
        final Class<?> c = Class.forName(className);
        final Field[] a = c.getFields();
        final Optional<Field> targetField = Arrays.stream(a).filter(t -> t.getName().equals(fieldName)).findFirst();
        if (targetField.isPresent()) {
            return Objects.toString(targetField.get().get(null));
        }
        return null;
    }
}
