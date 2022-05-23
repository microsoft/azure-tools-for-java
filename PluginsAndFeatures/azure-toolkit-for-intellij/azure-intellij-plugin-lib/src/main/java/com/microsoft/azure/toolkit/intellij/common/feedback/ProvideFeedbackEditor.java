/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProvideFeedbackEditor extends BaseEditor implements DumbAware {
    private JPanel pnlRoot;

    public ProvideFeedbackEditor(final Project project, VirtualFile virtualFile) {
        super(virtualFile);

        final JBCefBrowser browser = new JBCefBrowser("https://www.surveymonkey.com/r/PNB5NBL?mode=simple");
        pnlRoot.add(browser.getComponent(), new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 3, 3, null, null, null, 0));

    }

    @Override
    public @NotNull JComponent getComponent() {
        return pnlRoot;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "Provide Feedback";
    }

    @Override
    public void dispose() {

    }
}
