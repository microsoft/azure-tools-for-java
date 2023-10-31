package com.microsoft.azure.toolkit.intellij.connector.code;

import com.microsoft.azure.toolkit.intellij.connector.code.model.MethodIdentifier;
import com.microsoft.azure.toolkit.intellij.connector.code.model.ParameterIdentifier;
import com.microsoft.azure.toolkit.intellij.connector.code.provider.AnnotationCompletionMetadata;
import com.microsoft.azure.toolkit.intellij.connector.code.provider.MethodCompletionMetadata;

import java.util.Collections;
import java.util.List;

public interface CompletionMetadataProvider {
    default List<MethodCompletionMetadata> getMethodCompletionMetadata() {
        return Collections.emptyList();
    }

    default List<AnnotationCompletionMetadata> getAnnotationCompletionMetadata() {
        return Collections.emptyList();
    }

    default ParameterIdentifier buildParameterIdentifier(final String className, final String methodName, final List<String> parameterTypes, int index) {
        final MethodIdentifier identifier = MethodIdentifier.builder()
                .className(className)
                .methodName(methodName)
                .parameterTypes(parameterTypes)
                .build();
        return ParameterIdentifier.builder().method(identifier).parameterIndex(index).build();
    }
}
