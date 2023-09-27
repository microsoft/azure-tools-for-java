/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import lombok.Getter;

import javax.swing.*;

public class DatabaseServerPropertyActionPanel extends JPanel {
    @Getter
    private AzureActionButton saveButton;
    @Getter
    private AzureActionButton discardButton;
    private JPanel rootPanel;

}
