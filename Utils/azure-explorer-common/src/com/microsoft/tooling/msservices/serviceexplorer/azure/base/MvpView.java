package com.microsoft.tooling.msservices.serviceexplorer.azure.base;

import javax.swing.JOptionPane;

public interface MvpView {
    
    default void onError(String message) {
        JOptionPane.showMessageDialog(null, message, message, JOptionPane.ERROR_MESSAGE, null);
    }
    
}
