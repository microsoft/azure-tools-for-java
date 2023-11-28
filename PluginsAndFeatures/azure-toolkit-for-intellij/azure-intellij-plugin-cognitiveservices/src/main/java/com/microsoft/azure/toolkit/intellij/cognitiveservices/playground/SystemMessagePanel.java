/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.OpenAISystemTemplateComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.component.AzureTextArea;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class SystemMessagePanel implements AzureForm<SystemMessage> {
    private static final int DEBOUNCE_DELAY = 200;
    public static final String SYSTEM_MESSAGE_DESCRIPTION = "Use a template to get started, or just start writing your own system " +
        "message below. Want some tips? <a href = \"https://go.microsoft.com/fwlink/?linkid=2235756\">Learn more</a>\u2197";
    public static final String EXAMPLE_DESCRIPTION = "Add examples to show the chat what responses you want. " +
        "It will try to mimic any responses you add here so make sure they match the rules you laid out in the system message.";
    public static final String CHANGE_SYSTEM_MESSAGE_CONFIRM_MESSAGE = "Loading a new example will replace the current system message and start a new chat session. To save the current system message, copy it to a separate document before continuing.";
    public static final String UPDATE_SYSTEM_MESSAGE = "Update system message?";
    private JPanel pnlRoot;
    private JBLabel lblSystemDescription;
    private JLabel lblTemplate;
    private JLabel lblSystemMessage;
    private JTextArea areaSystemMessage;
    private JBLabel lblExampleDescription;
    private JScrollPane scrollPane;
    private JPanel pnlExample;
    private AzureActionButton<Void> btnAddExample;
    private OpenAISystemTemplateComboBox cbSystemTemplate;
    private JPanel pnlSystemMessageContainer;
    private JPanel pnlExamplesContainer;
    private JScrollPane scrollPaneSystemMessage;
    private ActionLink lblSaveChanges;
    private final TailingDebouncer debouncer = new TailingDebouncer(this::onValueChanged, DEBOUNCE_DELAY);
    private final List<ExamplePanel> panels = new ArrayList<>();

    private SystemMessage systemMessage;
    @Getter
    @Setter
    private Consumer<SystemMessage> valueChangedListener;

    public SystemMessagePanel() {
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        final Action<Void> addExampleAction = new Action<Void>(Action.Id.of("user/openai.add_example"))
            .withAuthRequired(false)
            .withHandler(ignore -> this.onAddNewExample());
        this.btnAddExample.setAction(addExampleAction);
        this.btnAddExample.setIcon(AllIcons.General.Add);

        this.lblSystemDescription.setText(SYSTEM_MESSAGE_DESCRIPTION);
        this.lblSystemDescription.setAllowAutoWrapping(true);
        this.lblSystemDescription.setCopyable(true);// this makes label auto wrapping
        this.lblSystemDescription.setForeground(UIUtil.getContextHelpForeground());

        this.lblExampleDescription.setText(EXAMPLE_DESCRIPTION);
        this.lblExampleDescription.setAllowAutoWrapping(true);
        this.lblExampleDescription.setCopyable(true);// this makes label auto wrapping
        this.lblExampleDescription.setForeground(UIUtil.getContextHelpForeground());

        this.lblSystemMessage.setIcon(AllIcons.General.ContextHelp);

        this.scrollPane.setBorder(JBUI.Borders.empty());
        this.scrollPaneSystemMessage.setBorder(AzureTextArea.DEFAULT_BORDER);
        this.areaSystemMessage.setBorder(JBUI.Borders.empty());
        this.lblTemplate.setBorder(JBUI.Borders.empty(6, 0));
        this.lblSystemMessage.setBorder(JBUI.Borders.empty(6, 0));

        this.lblSaveChanges.setBorder(JBUI.Borders.emptyTop(20));
        this.lblSaveChanges.addActionListener(ignore -> this.onSaveChange());

        this.cbSystemTemplate.addItemListener(this::onSelectTemplate);
        ((AzureTextArea) areaSystemMessage).addValueChangedListener(ignore -> debouncer.debounce());
    }

    private void onSelectTemplate(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SystemMessage value = cbSystemTemplate.getValue();
            if (ObjectUtils.equals(value, this.systemMessage)) {
                return;
            }
            try {
                final boolean confirm = AzureTaskManager.getInstance().runAndWait(() -> AzureMessager.getMessager().confirm(CHANGE_SYSTEM_MESSAGE_CONFIRM_MESSAGE, UPDATE_SYSTEM_MESSAGE)).get();
                if (confirm) {
                    this.setValue(value);
                    this.onSaveChange();
                } else {
                    this.cbSystemTemplate.setValue(this.systemMessage);
                }
            } catch (final Exception ex) {
                this.cbSystemTemplate.setValue(this.systemMessage);
            }
        }
    }

    private void onSaveChange() {
        this.systemMessage = getValue();
        this.lblSaveChanges.setVisible(false);
        this.valueChangedListener.accept(this.systemMessage);
    }

    private void onValueChanged() {
        final SystemMessage value = getValue();
        this.lblSaveChanges.setVisible(!ObjectUtils.equals(value, this.systemMessage));
    }

    private void onAddNewExample() {
        addExamplePanel(SystemMessage.Example.builder().build());
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbSystemTemplate = new OpenAISystemTemplateComboBox();
        this.areaSystemMessage = new AzureTextArea();
        this.pnlExample = new JPanel();
        this.pnlExample.setLayout(new BoxLayout(pnlExample, BoxLayout.Y_AXIS));
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }

    @Override
    public SystemMessage getValue() {
        final SystemMessage.SystemMessageBuilder builder = SystemMessage.builder();
        builder.systemMessage(areaSystemMessage.getText());
        final ArrayList<SystemMessage.Example> examples = new ArrayList<>();
        for (final ExamplePanel panel : panels) {
            examples.add(panel.getValue());
        }
        builder.examples(examples);
        return builder.build();
    }

    @Override
    public void setValue(@Nonnull final SystemMessage data) {
        this.systemMessage = data;
        this.cbSystemTemplate.setValue(data);
        renderValue(data);
    }

    private void renderValue(@Nonnull final SystemMessage data) {
        this.areaSystemMessage.setText(data.getSystemMessage());
        // workaround to fix that value change listener may be missing in some cases
        if (CollectionUtils.isEmpty(((AzureTextArea) areaSystemMessage).getValueChangedListeners())) {
            ((AzureTextArea) areaSystemMessage).addValueChangedListener(ignore -> debouncer.debounce());
        }
        Optional.ofNullable(data.getExamples()).ifPresent(this::renderExamples);
    }

    private void renderExamples(@Nonnull final List<SystemMessage.Example> examples) {
        panels.clear();
        pnlExample.removeAll();
        examples.forEach(this::addExamplePanel);
    }

    public void addExamplePanel(final SystemMessage.Example example) {
        final ExamplePanel panel = new ExamplePanel();
        panel.setValue(example);
        panel.addDeleteListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                removeExamplePanel(panel);
            }
        });
        panel.addValueChangedListener(ignore -> debouncer.debounce());
        this.pnlExample.add(panel.getPanel());
        panels.add(panel);
    }

    private void removeExamplePanel(final ExamplePanel panel) {
        panels.remove(panel);
        pnlExample.remove(panel.getPanel());
        onValueChanged();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return ListUtils.union(Arrays.asList(cbSystemTemplate, (AzureFormInput<?>) areaSystemMessage), panels);
    }
}
