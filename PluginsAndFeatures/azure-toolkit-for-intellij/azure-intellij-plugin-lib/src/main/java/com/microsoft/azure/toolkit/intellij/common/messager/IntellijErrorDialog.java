/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.JBColor;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.Optional;

public class IntellijErrorDialog extends DialogWrapper {
    private JPanel contentPanel;
    private JPanel detailsContainer;
    private JLabel iconLabel;
    private JEditorPane detailsPane;
    private JEditorPane contentPane;
    private JScrollPane detailsScrollPane;

    private final IntellijAzureMessage message;

    public IntellijErrorDialog(final IntellijAzureMessage message) {
        super(true);
        this.setTitle(message.getTitle());
        this.setModal(true);
        this.message = message;
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.iconLabel.setIcon(Messages.getErrorIcon());
        this.contentPane.setText(message.getContent());
        this.contentPane.setBackground(JBColor.WHITE);
        this.contentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        this.contentPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL());
            }
        });
        final String details = message.getDetails();
        if (StringUtils.isNotBlank(details)) {
            final HideableDecorator slotDecorator = new HideableDecorator(detailsContainer, "&Call Stack", false);
            slotDecorator.setContentComponent(detailsScrollPane);
            slotDecorator.setOn(false);
            this.detailsPane.setText(details);
            this.detailsPane.setBackground(JBColor.WHITE);
            this.detailsPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        } else {
            this.detailsContainer.setVisible(false);
        }
    }

    @Override
    protected Action[] createActions() {
        final Action[] actions = Arrays.stream(this.message.getActions()).map((com.microsoft.azure.toolkit.lib.common.action.Action<?> a) -> {
            final String name = Optional.ofNullable(a.view(null)).map(IView.Label::getLabel).orElse(a.toString());
            return new DialogWrapperAction(name) {
                protected void doAction(final ActionEvent e) {
                    // refer `com.intellij.designer.LightToolWindow.ActionButton`
                    final InputEvent inputEvent = e.getSource() instanceof InputEvent ? (InputEvent) e.getSource() : null;
                    final Component source = Optional.ofNullable(inputEvent)
                            .map(ComponentEvent::getComponent)
                            .or(() -> Optional.ofNullable(e.getSource()).filter(s -> s instanceof Component).map(c -> ((Component) c)))
                            .orElse(IntellijErrorDialog.this.getWindow());
                    final DataContext context = DataManager.getInstance().getDataContext(source);
                    final AnAction dummyAction = new EmptyAction();
                    final AnActionEvent actionEvent = AnActionEvent.createFromAnAction(dummyAction, inputEvent, ActionPlaces.UNKNOWN, context);
                    a.handle(null, actionEvent);
                }
            };
        }).toArray(Action[]::new);
        return ArrayUtils.addAll(actions, getOKAction());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }
}
