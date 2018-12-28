/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SparkJobLogConsoleView extends ConsoleViewImpl {
    @Nullable
    private JComponent mainPanel;

    @NotNull
    private ConsoleView secondaryConsoleView;

    public SparkJobLogConsoleView(@NotNull Project project) {
        super(project, true);

        // set `usePredefinedMessageFilter = false` to disable predefined filter by console view and avoid filter conflict
        this.secondaryConsoleView = new ConsoleViewImpl(project, GlobalSearchScope.allScope(project), true, false);
    }

    @Override
    public void print(@NotNull String s, @NotNull ConsoleViewContentType contentType) {
        if (contentType == ConsoleViewContentType.NORMAL_OUTPUT) {
            super.print(s, contentType);
        } else {
            getSecondaryConsoleView().print(s, contentType);
        }
    }

    @NotNull
    public ConsoleView getSecondaryConsoleView() {
        return secondaryConsoleView;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        if (mainPanel == null) {
            JPanel primary = (JPanel) super.getComponent();
            JComponent primaryMain = (JComponent) primary.getComponent(0);
            primary.remove(primaryMain);

            mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, primaryMain, getSecondaryConsoleView().getComponent());
            ((JSplitPane) mainPanel).setDividerSize(6);
            ((JSplitPane) mainPanel).setDividerLocation(480);

            add(mainPanel, BorderLayout.CENTER);
        }

        return this;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this.secondaryConsoleView);

        super.dispose();
    }
}
