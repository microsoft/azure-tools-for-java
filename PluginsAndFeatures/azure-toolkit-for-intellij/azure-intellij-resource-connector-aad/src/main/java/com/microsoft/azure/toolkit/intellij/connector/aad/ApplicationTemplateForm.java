/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.graph.models.Application;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ApplicationTemplateForm implements AzureForm<Application> {
    private static final Logger LOG = Logger.getInstance(ApplicationTemplateForm.class);

    // custom fields
    private final Map<ApplicationCodeTemplate, EditorTextField> templateEditors = new HashMap<>();

    // UI designer
    private JPanel contentPanel;
    private AzureApplicationComboBox applicationsBox;
    private JBTabbedPane templatesPane;
    private JBLabel credentialsWarning;
    private SubscriptionComboBox subscriptionBox;
    private JBLabel subscriptionLabel;

    ApplicationTemplateForm(@Nonnull Project project, @Nullable Application predefinedApplication) {
        credentialsWarning.setForeground(JBColor.RED);
        credentialsWarning.setAllowAutoWrapping(true);

        // init after createUIComponents, which is called first in the generated constructor
        for (var type : ApplicationCodeTemplate.values()) {
            var editor = createTextEditor(project, type.getFilename());
            templatesPane.add(type.getFilename(), editor);
            templateEditors.put(type, editor);
        }

        if (predefinedApplication != null) {
            subscriptionLabel.setVisible(false);
            subscriptionBox.setEnabled(false);
            subscriptionBox.setVisible(false);

            applicationsBox.setPredefinedItems(Collections.singletonList(predefinedApplication));
        } else {
            // the subscription box does a refresh and update of selected item in its constructor
            var selected = subscriptionBox.getValue();
            if (selected != null) {
                LOG.debug("Setting initially selected subscription to refresh applications");
                applicationsBox.setSubscription(selected);
            }

            subscriptionBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LOG.debug("Subscription changed, updating applications");
                    applicationsBox.setSubscription((Subscription) e.getItem());
                }
            });
        }
    }

    @Nonnull
    EditorTextField getCurrentEditor() {
        return (EditorTextField) templatesPane.getSelectedComponent();
    }

    void refreshSelectedApplication() {
        var application = getValue();
        if (application != null) {
            applyTemplate(application);
        }
    }

    private void applyTemplate(@Nonnull Application app) {
        var now = OffsetDateTime.now();

        // locate the first valid client secret
        String clientSecret = "";
        if (app.passwordCredentials != null) {
            clientSecret = app.passwordCredentials
                    .stream()
                    .filter(pwd -> (pwd.startDateTime == null || pwd.startDateTime.isBefore(now))
                            && (pwd.endDateTime == null || pwd.endDateTime.isAfter(now)))
                    .map(pwd -> StringUtil.defaultIfEmpty(pwd.secretText, ""))
                    .findFirst()
                    .orElse("");
        }

        credentialsWarning.setVisible(!clientSecret.isEmpty());

        var subscription = subscriptionBox.getValue();

        for (var entry : templateEditors.entrySet()) {
            var templateType = entry.getKey();
            var editor = entry.getValue();

            try {
                editor.setText(templateType.render(subscription != null ? subscription.getTenantId() : "",
                        app.appId != null ? app.appId : "",
                        clientSecret,
                        "group-names"));
            } catch (IOException e) {
                editor.setText(MessageBundle.message("templateDialog.editorInitFailed.text", e.getMessage()));
            }
        }
    }

    private void createUIComponents() {
        subscriptionBox = new SubscriptionComboBox();
        subscriptionBox.setEditable(false);

        applicationsBox = new AzureApplicationComboBox();
        applicationsBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyTemplate((Application) e.getItem());
            }
        });

        templatesPane = new JBTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    @Nullable
    public Subscription getSubscription() {
        return subscriptionBox.getValue();
    }

    @Override
    @Nullable
    public Application getValue() {
        return applicationsBox.getValue();
    }

    @Override
    public void setValue(Application applicationInner) {
        applicationsBox.setSelectedItem(applicationInner);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return List.of(subscriptionBox, applicationsBox);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    JComponent getPreferredFocusedComponent() {
        return subscriptionBox;
    }

    private static EditorTextField createTextEditor(@Nonnull final Project project, @Nonnull String filename) {
        LanguageFileType fileType = PlainTextFileType.INSTANCE;
        var extension = PathUtil.getFileExtension(filename);
        if (extension != null) {
            var extensionFileType = FileTypeManager.getInstance().getFileTypeByExtension(extension);
            if (extensionFileType != FileTypes.UNKNOWN && extensionFileType instanceof LanguageFileType) {
                fileType = (LanguageFileType) extensionFileType;
            }
        }
        var language = fileType.getLanguage();
        var textFieldProvider = EditorTextFieldProvider.getInstance();
        var editor = textFieldProvider.getEditorField(language, project, Collections.singleton(editorEx -> {
            editorEx.putUserData(IncrementalFindAction.SEARCH_DISABLED, Boolean.TRUE);

            var scheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
            var c = scheme.getColor(EditorColors.READONLY_BACKGROUND_COLOR);
            if (c == null) {
                c = scheme.getDefaultBackground();
            }
            editorEx.setBackgroundColor(c);
            editorEx.setColorsScheme(scheme);
            editorEx.setVerticalScrollbarVisible(true);
            editorEx.setHorizontalScrollbarVisible(true);
            editorEx.setBorder(JBUI.Borders.empty(5));
            editorEx.getSettings().setUseSoftWraps(true);
            editorEx.setEmbeddedIntoDialogWrapper(true);
        }));

        editor.setOneLineMode(false);
        editor.setViewer(true);
        editor.setPreferredSize(JBUI.size(650, 550));
        return editor;
    }
}
