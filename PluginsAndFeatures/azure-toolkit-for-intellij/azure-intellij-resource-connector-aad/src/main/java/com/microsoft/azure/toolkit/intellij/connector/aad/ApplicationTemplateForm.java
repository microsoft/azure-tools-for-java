package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class ApplicationTemplateForm implements AzureForm<Application> {
    private final Subscription subscription;
    @Nullable
    private final GraphServiceClient<Request> graphClient;
    @Nullable
    private final List<Application> predefinedItems;
    @NotNull
    private final Project project;

    // UI designer
    private JPanel contentPanel;
    private AzureApplicationComboBox applicationsBox;
    private EditorTextField templateEditor;

    ApplicationTemplateForm(@NotNull Project project,
                            @NotNull Subscription subscription,
                            @Nullable GraphServiceClient<Request> graphClient,
                            List<Application> predefinedItems) {
        this.project = project;
        this.subscription = subscription;
        this.graphClient = graphClient;
        this.predefinedItems = predefinedItems;
        assert graphClient != null || predefinedItems != null;

        applicationsBox.refreshItems();
    }

    private void applyTemplate(@NotNull Application app) {
        var template = String.format("# Specifies your Active Directory ID:\n" +
                        "azure.activedirectory.tenant-id=%s\n" +
                        "# Specifies your App Registration's Application ID:\n" +
                        "azure.activedirectory.client-id=%s\n" +
                        "# Specifies your App Registration's secret key:\n" +
                        "azure.activedirectory.client-secret=%s\n" +
                        "# Specifies the list of Active Directory groups to use for authorization:\n" +
                        "azure.activedirectory.user-group.allowed-groups=%s",
                subscription.getTenantId(),
                app.appId,
                "client-secret",
                "groups");
        templateEditor.setText(template);
    }

    private void createUIComponents() {
        Supplier<List<Application>> supplier;
        if (graphClient != null) {
            supplier = () -> AzureUtils.loadApplications(graphClient)
                    .stream()
                    .sorted(Comparator.comparing(a -> StringUtil.defaultIfEmpty(a.displayName, "")))
                    .collect(Collectors.toList());
        } else {
            supplier = () -> predefinedItems;
        }

        applicationsBox = new AzureApplicationComboBox(supplier);
        applicationsBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyTemplate((Application) e.getItem());
            }
        });

        templateEditor = createTextEditor(project);
    }

    @Override
    public Application getData() {
        return applicationsBox.getValue();
    }

    @Override
    public void setData(Application applicationInner) {
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
