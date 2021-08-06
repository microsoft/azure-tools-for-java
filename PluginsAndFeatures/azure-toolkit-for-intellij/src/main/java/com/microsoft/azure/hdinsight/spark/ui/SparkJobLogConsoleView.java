/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
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
        this.secondaryConsoleView = new ConsoleViewWithMessageBars(project);
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

        getEditor().getContentComponent().setFocusCycleRoot(false);

        if (secondaryConsoleView instanceof ConsoleViewImpl) {
            ((ConsoleViewImpl) secondaryConsoleView).getEditor().getContentComponent().setFocusCycleRoot(false);
        }

        this.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
            @Override
            public Component getComponentAfter(Container aContainer, Component aComponent) {
                if (getEditor() != null && aComponent == getEditor().getContentComponent()) {
                    return secondaryConsoleView.getPreferredFocusableComponent();
                }

                return null;
            }

            @Override
            public Component getComponentBefore(Container aContainer, Component aComponent) {
                if (secondaryConsoleView != null
                        && aComponent == secondaryConsoleView.getPreferredFocusableComponent()) {
                    return getEditor().getContentComponent();
                }

                return null;
            }

            @Override
            public Component getFirstComponent(Container aContainer) {
                return getEditor() != null ? getEditor().getContentComponent() : null;
            }

            @Override
            public Component getLastComponent(Container aContainer) {
                return secondaryConsoleView != null ? secondaryConsoleView.getPreferredFocusableComponent() : null;
            }
        });

        this.setFocusCycleRoot(true);

        return this;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this.secondaryConsoleView);

        super.dispose();
    }
}
