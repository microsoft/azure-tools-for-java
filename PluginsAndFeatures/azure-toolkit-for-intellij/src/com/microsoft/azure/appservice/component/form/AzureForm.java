package com.microsoft.azure.appservice.component.form;

public interface AzureForm<T> {
    T getData();

    default Object get(String name){
        throw new RuntimeException();
    }
}
