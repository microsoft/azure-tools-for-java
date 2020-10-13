package com.microsoft.azure.toolkit.appservice.intellij.view;

public interface AzureFormPanel<T> extends AzureForm<T> {
    void setVisible(boolean visible);
    default void $$$setupUI$$$() {
    }
}
