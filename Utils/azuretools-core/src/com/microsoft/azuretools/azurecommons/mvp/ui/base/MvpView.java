package com.microsoft.azuretools.azurecommons.mvp.ui.base;

import javax.swing.JOptionPane;

public interface MvpView {
    
    default void onError(String message) {
        JOptionPane.showMessageDialog(null, null, message, JOptionPane.ERROR_MESSAGE, null);
    }
    
    default void OnErrorWithException(String message, Exception ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(), message, JOptionPane.ERROR_MESSAGE, null);
    }
}
