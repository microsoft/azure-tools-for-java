package com.microsoft.azure.toolkit.intellij.sdkreferencebook;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import javax.annotation.Nonnull;

public class OpenSdkReferenceBook extends AnAction {
    @Override
    public void actionPerformed(@Nonnull final AnActionEvent anActionEvent) {
        System.out.println(OpenSdkReferenceBook.class.getName());
    }
}
