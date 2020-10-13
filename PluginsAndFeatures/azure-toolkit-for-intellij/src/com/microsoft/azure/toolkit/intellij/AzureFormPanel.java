package com.microsoft.azure.toolkit.intellij;

public interface AzureFormPanel<T> extends AzureForm<T> {
    void setVisible(boolean visible);
    default void $$$setupUI$$$() {
    }
}
