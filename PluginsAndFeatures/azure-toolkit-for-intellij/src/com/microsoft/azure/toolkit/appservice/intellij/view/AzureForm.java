package com.microsoft.azure.toolkit.appservice.intellij.view;

public interface AzureForm<T> {
    T getData();

    default Object get(String name){
        throw new RuntimeException();
    }
}
