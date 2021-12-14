/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.jdt;

import com.microsoft.azure.toolkit.eclipse.function.utils.AnnotationUtils;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotation;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotationType;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

class JdtFunctionAdaptor {
    public static FunctionMethod create(IMethodBinding method) {
        FunctionMethod functionMethod = new FunctionMethod();
        functionMethod.setName(method.getName());
        functionMethod.setReturnTypeName(method.getReturnType().getQualifiedName());
        functionMethod.setAnnotations(method.getAnnotations() == null ? Collections.emptyList() :
                Arrays.stream(method.getAnnotations()).map(JdtFunctionAdaptor::create).collect(Collectors.toList()));

        int len = method.getParameterTypes().length;
        List<FunctionAnnotation[]> parameterAnnotations = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            IAnnotationBinding[] paramAnnotations = method.getParameterAnnotations(i);
            if (paramAnnotations != null) {
                parameterAnnotations.add(Arrays.stream(paramAnnotations).map(JdtFunctionAdaptor::create).toArray(FunctionAnnotation[]::new));
            }
        }
        functionMethod.setParameterAnnotations(parameterAnnotations);
        functionMethod.setDeclaringTypeName(method.getDeclaringClass().getQualifiedName());
        return functionMethod;
    }

    public static FunctionAnnotation create(@Nonnull IAnnotationBinding obj) {
        return create(obj, true);
    }

    private static FunctionAnnotation create(@Nonnull IAnnotationBinding obj, boolean resolveAnnotationType) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> defaultMap = new HashMap<>();

        for (IMemberValuePairBinding pair : obj.getDeclaredMemberValuePairs()) {
            map.put(pair.getName(), AnnotationUtils.calculateJdtValue(pair.getValue()));
        }

        for (IMemberValuePairBinding pair : obj.getAllMemberValuePairs()) {
            defaultMap.put(pair.getName(), AnnotationUtils.calculateJdtValue(pair.getValue()));
        }

        FunctionAnnotation functionAnnotation = new FunctionAnnotation();
        functionAnnotation.setAnnotationType(toFunctionAnnotationType(obj.getAnnotationType(), resolveAnnotationType));

        functionAnnotation.setProperties(map);
        functionAnnotation.setDefaultProperties(defaultMap);
        return functionAnnotation;
    }

    private static FunctionAnnotationType toFunctionAnnotationType(ITypeBinding type, boolean resolveAnnotationType) {
        FunctionAnnotationType res = new FunctionAnnotationType();
        res.setFullName(type.getQualifiedName());
        res.setName(type.getName());
        if (resolveAnnotationType) {
            res.setAnnotations(Arrays.stream(type.getAnnotations()).map(a -> create(a, false)).collect(Collectors.toList()));
        }
        return res;
    }
}
