/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.ui.ValidationInfo;

import javax.swing.*;

public interface AzureAbstractPanel {

    public abstract JComponent getPanel();

    public abstract String getDisplayName();

    public abstract boolean doOKAction();

    public abstract String getSelectedValue();

    public abstract ValidationInfo doValidate();

    public String getHelpTopic();
}
