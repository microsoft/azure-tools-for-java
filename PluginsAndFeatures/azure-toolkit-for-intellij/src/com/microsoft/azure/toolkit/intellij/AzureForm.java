package com.microsoft.azure.toolkit.intellij;

public interface AzureForm<T> {
    T getData();

    default Object get(String name){
        throw new RuntimeException();
    }
}
