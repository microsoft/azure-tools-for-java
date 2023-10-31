package com.microsoft.azure.toolkit.intellij.connector.code.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class AnnotationIdentifier {
    private String annotation;
    private String property;
}
