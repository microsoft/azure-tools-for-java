package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.PathUtil;
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
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ApplicationTemplateForm implements AzureForm<Application> {
    private final Subscription subscription;

    // custom fields
    private final Map<ApplicationTemplateType, EditorTextField> templateEditors = new HashMap<>();

    // UI designer
    private JPanel contentPanel;
    private AzureApplicationComboBox applicationsBox;
    private JBTabbedPane templatesPane;

    ApplicationTemplateForm(@NotNull Project project,
                            @NotNull Subscription subscription,
                            @Nullable GraphServiceClient<Request> graphClient,
                            List<Application> predefinedItems) {
        this.subscription = subscription;
        assert graphClient != null || predefinedItems != null;

        // init after createUIComponents, which is called as first in the generated constructor
        for (var type : ApplicationTemplateType.values()) {
            var editor = createTextEditor(project, type.getFilename());
            templatesPane.add(type.getFilename(), editor);
            templateEditors.put(type, editor);
        }

        if (graphClient != null) {
            applicationsBox.setItemsLoader(() -> AzureUtils.loadApplications(graphClient)
                    .stream()
                    .sorted(Comparator.comparing(a -> StringUtil.defaultIfEmpty(a.displayName, "")))
                    .collect(Collectors.toList()));
        } else {
            applicationsBox.setItemsLoader(() -> predefinedItems);
        }
        applicationsBox.refreshItems();
    }

    private void applyTemplate(@NotNull Application app) {
        for (var entry : templateEditors.entrySet()) {
            var template = new ApplicationTemplate(
                    entry.getKey().getResourcePath(),
                    subscription.getTenantId(),
                    app.appId,
                    "client-secret",
                    "group-names");
            try {
                entry.getValue().setText(template.content());
            } catch (IOException e) {
                entry.getValue().setText("ERROR");
            }
        }
    }

    private void createUIComponents() {
        applicationsBox = new AzureApplicationComboBox();
        applicationsBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applyTemplate((Application) e.getItem());
            }
        });

        templatesPane = new JBTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
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

    private static EditorTextField createTextEditor(@NotNull final Project project, @NotNull String filename) {
        FileType fileType = PlainTextFileType.INSTANCE;
        var extension = PathUtil.getFileExtension(filename);
        if (extension != null) {
            var extensionFileType = FileTypeManager.getInstance().getFileTypeByExtension(extension);
            if (extensionFileType != FileTypes.UNKNOWN) {
                fileType = extensionFileType;
            }
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
                editor.setVerticalScrollbarVisible(true);
                editor.setBorder(JBUI.Borders.empty(5));
                return editor;
            }
        };
        editor.setOneLineMode(false);
        editor.setViewer(true);
        editor.setPreferredSize(JBUI.size(650, 550));
        return editor;
    }
}
