package com.microsoft.intellij.helpers;

import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelper;

import javax.swing.*;

public class MvpUIHelperImpl implements MvpUIHelper {

    @Override
    public void showError(String msg) {
        JOptionPane.showMessageDialog(null, null, msg, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showException(String msg, Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), msg, JOptionPane.ERROR_MESSAGE);
    }
}
