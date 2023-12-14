// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode.ViewCodeDialog;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

public class ChatBox {
    @Getter
    private ChatBot chatBot;
    private JLabel sendBtn;
    @Getter
    private JPanel contentPanel;
    private JPanel messageBox;
    private JBTextArea promptInput;
    private JPanel promptPanel;
    private ActionLink clearBtn;
    private ActionLink viewCodeBtn;
    private JBLabel placeholderMessage1;
    private JLabel placeholderIcon;
    private JBLabel placeholderMessage2;
    private JPanel placeholder;
    private JBScrollPane scrollPane;

    public ChatBox() {
        this.initComponents();
    }

    public ChatBox(final ChatBot chatBot) {
        this.initComponents();
        this.setChatBot(chatBot);
    }

    public void setChatBot(ChatBot chatBot) {
        this.chatBot = chatBot;
        AzureTaskManager.getInstance().runLater(() -> {
            this.clearBtn.doClick();
            this.sendBtn.setEnabled(true);
        }, AzureTask.Modality.ANY);
    }

    private void initComponents() {
        final Icon botIcon = IntelliJAzureIcons.getIcon(AzureIcons.Common.CHATBOT);
        final Icon sendIcon = IntelliJAzureIcons.getIcon(AzureIcons.Action.SEND);
        this.clearBtn.setEnabled(false);
        this.clearBtn.addActionListener(e -> Optional.ofNullable(this.chatBot).ifPresent((c) -> clearSession()));
        this.viewCodeBtn.addActionListener(e -> viewCode());

        this.messageBox.setVisible(false);
        this.placeholder.setVisible(true);
        this.placeholderIcon.setIcon(IconUtil.scale(botIcon, this.placeholder, 2.5f));
        this.placeholderMessage1.setForeground(UIUtil.getContextHelpForeground());
        this.placeholderMessage2.setForeground(UIUtil.getContextHelpForeground());

        final JBColor borderColor = new JBColor(12895428, 6185056);
        final CompoundBorder promptPanelBorder = BorderFactory.createCompoundBorder(this.promptPanel.getBorder(), BorderFactory.createLineBorder(borderColor, 1, true));
        this.promptPanel.setBorder(promptPanelBorder);
        this.promptPanel.setBackground(JBUI.CurrentTheme.EditorTabs.background());

        this.promptInput.getEmptyText().setText("Type user query here. (Shift + Enter for new line, Enter to send)");
        this.promptInput.setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.promptInput.setLineWrap(true);
        this.promptInput.setFont(JBUI.Fonts.label());
        this.promptInput.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) {
                    try {
                        final int caretPosition = promptInput.getCaretPosition();
                        promptInput.getDocument().insertString(caretPosition, "\n", null);
                    } catch (final BadLocationException ignored) {
                    }
                }
            }
        });

        this.promptInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        this.promptInput.getActionMap().put("send", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Optional.ofNullable(chatBot).ifPresent((c) -> send());
            }
        });

        this.sendBtn.setEnabled(false);
        this.sendBtn.setIcon(sendIcon);
        this.sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.sendBtn.setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.sendBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                Optional.ofNullable(chatBot).ifPresent((c) -> send());
            }
        });
        this.contentPanel.repaint();
    }

    @AzureOperation(value = "user/openai.view_code", source = "this.chatBot.getDeployment()")
    private void viewCode() {
        Optional.ofNullable(this.chatBot).ifPresent((c) -> {
            final Project project = DataManager.getInstance().getDataContext(this.contentPanel).getData(CommonDataKeys.PROJECT);
            new ViewCodeDialog(project, c).show();
        });
    }

    @AzureOperation(value = "user/openai.clear_session.deployment",
        params = {"this.chatBot.getDeployment().getName()"}, source = "this.chatBot.getDeployment()")
    public void clearSession() {
        Optional.ofNullable(this.chatBot).ifPresent(ChatBot::reset);
        this.messageBox.removeAll();
        this.messageBox.setVisible(false);
        this.placeholder.setVisible(true);
        this.scrollPane.revalidate();
        this.scrollPane.repaint();
    }

    @AzureOperation(value = "user/openai.send_message.deployment",
        params = {"this.chatBot.getDeployment().getName()"}, source = "this.chatBot.getDeployment()")
    public void send() {
        final String prompt = this.promptInput.getText();
        if (prompt.isBlank() || !this.sendBtn.isEnabled()) {
            return;
        }
        if (this.messageBox.getComponentCount() == 0) {
            this.placeholder.setVisible(false);
            this.messageBox.setVisible(true);
            this.scrollPane.revalidate();
            this.scrollPane.repaint();
        }
        this.sendBtn.setEnabled(false);
        this.clearBtn.setEnabled(false);
        this.sendBtn.setToolTipText("Sending...");
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runOnPooledThread(() -> {
            try {
                this.addMessage(new MarkdownText(prompt), ChatRole.USER);
                tm.runLater(() -> this.promptInput.setText(""));
                final ChatChoice response = this.chatBot.send(prompt);
                final ChatMessage message = response.getMessage();
                final String content = message.getContent();
                this.addMessage(new MarkdownText(content), ChatRole.ASSISTANT);
            } finally {
                tm.runLater(() -> {
                    this.sendBtn.setEnabled(true);
                    this.clearBtn.setEnabled(true);
                    this.sendBtn.setToolTipText("Send");
                });
            }
        });
    }

    private void addMessage(MarkdownText markdown, ChatRole role) {
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runLater(() -> {
            final JPanel message = role == ChatRole.USER ?
                new UserMessagePane(markdown).getContentPanel() : new BotMessagePane(markdown).getContentPanel();
            this.messageBox.add(message);
            this.messageBox.revalidate();
            this.messageBox.repaint();
            this.scrollPane.revalidate();
            this.scrollPane.repaint();

            this.scrollToBottom();
        });
    }

    private void scrollToBottom() {
        final JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        final AdjustmentListener downScroller = new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                final Adjustable adjustable = e.getAdjustable();
                adjustable.setValue(adjustable.getMaximum());
                verticalBar.removeAdjustmentListener(this);
            }
        };
        verticalBar.addAdjustmentListener(downScroller);
    }

    private void createUIComponents() {
        this.messageBox = new JPanel();
        this.messageBox.setLayout(new BoxLayout(this.messageBox, BoxLayout.Y_AXIS));
    }
}
