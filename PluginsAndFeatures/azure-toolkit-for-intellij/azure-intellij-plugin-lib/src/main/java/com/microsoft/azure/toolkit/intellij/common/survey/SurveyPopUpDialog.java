/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common.survey;

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import com.intellij.ui.Gray;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

public class SurveyPopUpDialog extends JDialog {

    private static final int DISPOSE_TIME = 10;
    private static final String LABEL_DO_NOT_SHOW_AGAIN = "Don't show again.";

    private JPanel contentPane;
    private JButton giveFeedbackButton;
    private JButton notNowButton;
    private HyperlinkLabel lblDoNotShowAgain;
    private JLabel lblMessage;
    private JLabel lblAzureIcon;
    private JLabel lblClose;
    private JLabel lblFeedBack;
    private JLabel lblTakeSurvey;

    private Point dragPosition;
    private Color buttonOnHoverColor = JBColor.WHITE;
    private boolean isDisposed;

    private final Timer disposeTimer;
    private final ICustomerSurvey survey;
    private final MessageBusConnection connection;
    private final Consumer<? super CustomerSurveyResponse> listener;

    public SurveyPopUpDialog(@Nonnull final Project project, @Nonnull final ICustomerSurvey customerSurvey,
                             @Nonnull final Consumer<? super CustomerSurveyResponse> listener) {
        super();

        this.listener = listener;
        this.survey = customerSurvey;
        this.disposeTimer = new Timer(1000 * DISPOSE_TIME, (e) -> takeSurvey(CustomerSurveyResponse.PUT_OFF_AUTO));
        this.connection = project.getMessageBus().connect();
        $$$setupUI$$$();

        this.setAlwaysOnTop(true);
        this.setSize(250, 250);
        this.setContentPane(contentPane);
        this.setUndecorated(true);
        this.getRootPane().setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
        this.setModal(false);
        this.setLocationRelativeToIDE(project);
        this.setDisposeTimer();

        giveFeedbackButton.addActionListener((e) -> takeSurvey(CustomerSurveyResponse.ACCEPT));
        giveFeedbackButton.setFocusable(false);

        notNowButton.addActionListener((e) -> takeSurvey(CustomerSurveyResponse.PUT_OFF));
        notNowButton.setFocusable(false);

        lblClose.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                takeSurvey(CustomerSurveyResponse.PUT_OFF);
            }
        });

        addDragListener();
        // call onCancel() when cross is clicked
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                takeSurvey(CustomerSurveyResponse.PUT_OFF);
            }
        });
        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> takeSurvey(CustomerSurveyResponse.PUT_OFF), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);

        // Add listener to intellij theme change
        renderUiByTheme(LafManager.getInstance());
        connection.subscribe(LafManagerListener.TOPIC, (LafManagerListener) manager -> renderUiByTheme(manager));

        this.pack();
        this.disposeTimer.restart();
    }

    private void renderUiByTheme(final LafManager manager) {
        // Use default ui setting for mac
        if (SystemUtils.IS_OS_MAC) {
            return;
        }
        final UIManager.LookAndFeelInfo theme = manager.getCurrentLookAndFeel();
        if (StringUtils.containsIgnoreCase(theme.getName(), "light")) {
            setPanelBackGroundColor(contentPane, JBColor.WHITE);
            final ButtonUI buttonUI = new MetalButtonUI();
            giveFeedbackButton.setUI(buttonUI);
            giveFeedbackButton.setForeground(Gray._255);
            giveFeedbackButton.setBackground(new Color(0, 114, 198));
            notNowButton.setUI(buttonUI);
            notNowButton.setForeground(Gray._255);
            notNowButton.setBackground(Gray._105);
            buttonOnHoverColor = JBColor.LIGHT_GRAY;
        } else {
            setPanelBackGroundColor(contentPane, null);
            final ButtonUI buttonUI = new JButton().getUI();
            giveFeedbackButton.setForeground(null);
            giveFeedbackButton.setBackground(null);
            giveFeedbackButton.setUI(buttonUI);
            notNowButton.setForeground(null);
            notNowButton.setBackground(null);
            notNowButton.setUI(buttonUI);
            buttonOnHoverColor = JBColor.WHITE;
        }
        giveFeedbackButton.setBorderPainted(false);
        setButtonHoverListener(giveFeedbackButton);

        notNowButton.setBorderPainted(false);
        setButtonHoverListener(notNowButton);
    }

    private void setDisposeTimer() {
        this.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                SurveyPopUpDialog.this.disposeTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                SurveyPopUpDialog.this.disposeTimer.restart();
            }
        });
    }

    private void setButtonHoverListener(JButton button) {
        button.addMouseListener(new MouseInputAdapter() {
            Color originForegroundColor;

            @Override
            public void mouseEntered(MouseEvent e) {
                originForegroundColor = ((JButton) e.getSource()).getForeground();
                super.mouseEntered(e);
                button.setForeground(buttonOnHoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                button.setForeground(originForegroundColor);
            }
        });
    }

    // Set pop up window to right bottom side of IDE
    private void setLocationRelativeToIDE(@Nonnull Project project) {
        JFrame ideFrame = WindowManagerImpl.getInstance().getFrame(project);
        if (ideFrame == null) {
            // In case user close project after start up
            ideFrame = WindowManagerImpl.getInstance().findVisibleFrame();
        }

        final int locationX = Optional.ofNullable(ideFrame).map(frame -> frame.getX() + frame.getWidth() - this.getWidth())
                .orElseGet(() -> Toolkit.getDefaultToolkit().getScreenSize().width - this.getWidth());
        final int locationY = Optional.ofNullable(ideFrame).map(frame -> frame.getY() + frame.getHeight() - this.getHeight())
                .orElseGet(() -> Toolkit.getDefaultToolkit().getScreenSize().height - this.getHeight());
        this.setLocation(locationX, locationY);
    }

    private void addDragListener() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragPosition = new Point(e.getX(), e.getY());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                SurveyPopUpDialog.this.setLocation(e.getX() + getX() - dragPosition.x, e.getY() + getY() - dragPosition.y);
            }
        });
    }

    private synchronized void takeSurvey(CustomerSurveyResponse response) {
        if (isDisposed) {
            return;
        }
        close();
        this.listener.consume(response);
    }

    private synchronized void close() {
        isDisposed = true;
        disposeTimer.stop();
        connection.disconnect();
        dispose();
    }

    private void setPanelBackGroundColor(JPanel panel, Color color) {
        panel.setBackground(color);
        for (final Component child : panel.getComponents()) {
            if (child instanceof JPanel) {
                setPanelBackGroundColor((JPanel) child, color);
            }
        }
    }

    private void createUIComponents() {
        lblDoNotShowAgain = new HyperlinkLabel(LABEL_DO_NOT_SHOW_AGAIN);
        lblDoNotShowAgain.addHyperlinkListener(e -> takeSurvey(CustomerSurveyResponse.NEVER_SHOW_AGAIN));

        lblMessage = new JLabel(survey.getDescription());
        lblMessage.setFont(new Font(lblMessage.getFont().getName(), Font.BOLD, lblMessage.getFont().getSize()));

        lblAzureIcon = new JLabel();
        lblAzureIcon.setIcon(survey.getIcon());
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
