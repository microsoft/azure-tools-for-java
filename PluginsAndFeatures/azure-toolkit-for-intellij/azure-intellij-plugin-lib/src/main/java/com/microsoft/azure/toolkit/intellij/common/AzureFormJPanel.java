package com.microsoft.azure.toolkit.intellij.common;

import javax.swing.*;

public interface AzureFormJPanel<T> extends AzureFormPanel<T> {
    JPanel getContentPanel();
}
