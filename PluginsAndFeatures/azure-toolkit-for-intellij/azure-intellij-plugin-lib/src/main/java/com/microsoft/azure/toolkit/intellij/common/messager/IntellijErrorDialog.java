/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.HideableDecorator;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.util.Optional;

public class IntellijErrorDialog extends DialogWrapper {
    private JPanel contentPanel;
    private JPanel detailsContainer;
    private JLabel iconLabel;
    private JTextPane detailsPane;
    private JEditorPane contentPane;
    private JScrollPane detailsScrollPane;

    private final IntellijAzureMessage message;

    public IntellijErrorDialog(final IntellijAzureMessage message) {
        super(message.getProject(), true);
        this.setTitle(message.getTitle());
        this.setModal(true);
        this.message = message;
        init();
    }

    @Override
    protected void init() {
        super.init();
        final HideableDecorator slotDecorator = new HideableDecorator(detailsContainer, "Details", true);
        slotDecorator.setContentComponent(detailsScrollPane);
        slotDecorator.setOn(false);
        this.contentPane.setText(message.getContent(false));
        this.detailsPane.setText(message.getDetails());
        iconLabel.setIcon(Messages.getErrorIcon());
        contentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        detailsPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
    }

    @Override
    protected Action[] createActions() {
        final Action[] actions = this.message.getAnActions().stream().map(a -> {
            final String name = Optional.ofNullable(a.getTemplateText()).orElse(a.toString());
            return new DialogWrapperAction(name) {
                protected void doAction(final ActionEvent e) {
                    // refer `com.intellij.designer.LightToolWindow.ActionButton`
                    final InputEvent inputEvent = e.getSource() instanceof InputEvent ? (InputEvent) e.getSource() : null;
                    final Component source = Optional.ofNullable(inputEvent)
                            .map(ComponentEvent::getComponent)
                            .or(() -> Optional.ofNullable(e.getSource()).filter(s -> s instanceof Component).map(c -> ((Component) c)))
                            .orElse(IntellijErrorDialog.this.getWindow());
                    final DataContext context = DataManager.getInstance().getDataContext(source);
                    final AnActionEvent actionEvent = AnActionEvent.createFromAnAction(a, inputEvent, ActionPlaces.UNKNOWN, context);
                    a.actionPerformed(actionEvent);
                }
            };
        }).toArray(Action[]::new);
        return ArrayUtils.addAll(new Action[]{getOKAction()}, actions);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }
}
