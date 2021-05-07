package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.azure.resourcemanager.authorization.fluent.GraphRbacManagementClient;
import com.azure.resourcemanager.authorization.fluent.models.ApplicationInner;
import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class ApplicationTemplateForm implements AzureForm<ApplicationInner> {
    private final Subscription subscription;
    @Nullable
    private final GraphRbacManagementClient graphClient;
    @Nullable
    private final List<ApplicationInner> predefinedItems;
    @NotNull
    private final Project project;

    // UI designer
    private JPanel contentPanel;
    private AzureApplicationComboBox applicationsBox;
    private EditorTextField templateEditor;

    ApplicationTemplateForm(@NotNull Project project,
                            @NotNull Subscription subscription,
                            @Nullable GraphRbacManagementClient graphClient,
                            @Nullable List<ApplicationInner> predefinedItems) {
        this.project = project;
        this.subscription = subscription;
        this.graphClient = graphClient;
        this.predefinedItems = predefinedItems;
        assert graphClient != null || predefinedItems != null;

        applicationsBox.refreshItems();
    }

    private void applyTemplate(@NotNull ApplicationInner app) {
        var template = String.format("# Specifies your Active Directory ID:\n" +
                        "azure.activedirectory.tenant-id=%s\n" +
                        "# Specifies your App Registration's Application ID:\n" +
                        "azure.activedirectory.client-id=%s\n" +
                        "# Specifies your App Registration's secret key:\n" +
                        "azure.activedirectory.client-secret=%s\n" +
                        "# Specifies the list of Active Directory groups to use for authorization:\n" +
                        "azure.activedirectory.user-group.allowed-groups=%s",
                subscription.getTenantId(),
                app.appId(),
                "client-secret",
                "groups");
        templateEditor.setText(template);
    }

    private void createUIComponents() {
        Supplier<List<ApplicationInner>> supplier;
        if (graphClient != null) {
            supplier = () -> graphClient.getApplications().list().stream()
                    .sorted(Comparator.comparing(ApplicationInner::displayName))
                    .collect(Collectors.toList());
        } else {
            supplier = () -> predefinedItems;
        }

        applicationsBox = new AzureApplicationComboBox(supplier);
        applicationsBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyTemplate((ApplicationInner) e.getItem());
            }
        });

        templateEditor = createTextEditor(project);
    }

    @Override
    public ApplicationInner getData() {
        return applicationsBox.getValue();
    }

    @Override
    public void setData(ApplicationInner applicationInner) {
        applicationsBox.setSelectedItem(applicationInner);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.emptyList();
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    JComponent getPreferredFocusedComponent() {
        return applicationsBox;
    }

    private static EditorTextField createTextEditor(@NotNull final Project project) {
        var fileType = FileTypeManager.getInstance().getFileTypeByExtension("properties");
        if (fileType == UnknownFileType.INSTANCE) {
            fileType = PlainTextFileType.INSTANCE;
        }

        var editor = new EditorTextField("", project, fileType) {
            @Override
            protected EditorEx createEditor() {
                var editor = super.createEditor();
                editor.putUserData(IncrementalFindAction.SEARCH_DISABLED, Boolean.TRUE);

                var globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
                var c = globalScheme.getColor(EditorColors.READONLY_BACKGROUND_COLOR);
                if (c == null) {
                    c = globalScheme.getDefaultBackground();
                }
                editor.setBackgroundColor(c);

                setupBorder(editor);
                return editor;
            }
        };
        editor.setOneLineMode(false);
        editor.setViewer(true);
        editor.setPreferredSize(JBUI.size(650, 450));
        return editor;
    }
}
