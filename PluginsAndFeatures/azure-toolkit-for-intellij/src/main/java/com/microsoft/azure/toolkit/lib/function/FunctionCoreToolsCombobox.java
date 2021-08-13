/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.function;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.AzureConfigurable;
import org.apache.commons.lang3.StringUtils;

import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FunctionCoreToolsCombobox extends AzureComboBox<String> {
    private static final String AZURE_TOOLKIT_FUNCTION_CORE_TOOLS_HISTORY = "azure_toolkit.function.core.tools.history";
    private static final String OPEN_AZURE_SETTINGS = "Open Azure Settings";
    private static final int MAX_HISTORY_SIZE = 15;
    private final Set<String> funcCoreToolsPathList = new LinkedHashSet<>();

    private Condition<? super VirtualFile> fileFilter;
    private Project project;

    private String lastSelected;
    private boolean includeSettings;
    private boolean pendingOpenAzureSettings = false;

    public FunctionCoreToolsCombobox(Project project, boolean includeSettings) {
        super(false);
        this.project = project;
        this.includeSettings = includeSettings;
        final List<String> exePostfix = Arrays.asList("exe|bat|cmd|sh|bin|run".split("\\|"));
        this.fileFilter = file ->
            Comparing.equal(file.getNameWithoutExtension(), "func", file.isCaseSensitive())
                && (file.getExtension() == null || exePostfix.contains(
                file.isCaseSensitive() ? file.getExtension() : StringUtils.lowerCase(file.getExtension())
            )

            );
        reset();
        if (includeSettings) {
            this.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
                label.setText(value);
                if (StringUtils.equals(value, OPEN_AZURE_SETTINGS)) {
                    label.setIcon(AllIcons.General.GearPlain);
                }
            }));

            this.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (StringUtils.equals((String) e.getItem(), OPEN_AZURE_SETTINGS)) {
                        if (!pendingOpenAzureSettings) {
                            AzureTaskManager.getInstance().runLater(() -> {
                                ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureConfigurable.AzureAbstractConfigurable.class);
                                FunctionCoreToolsCombobox.this.reset();
                                pendingOpenAzureSettings = false;
                            });
                            pendingOpenAzureSettings = true;
                        }

                        FunctionCoreToolsCombobox.this.setValue(lastSelected);
                    } else {
                        lastSelected = (String) e.getItem();
                    }
                }
            });
        }
    }

    public void reset() {
        funcCoreToolsPathList.clear();
        this.clear();
        funcCoreToolsPathList.addAll(loadHistory());
        funcCoreToolsPathList.addAll(FunctionCliResolver.resolve());

        final String valueFromAzConfig = Azure.az().config().getFunctionCoreToolsPath();
        if (StringUtils.isNotBlank(valueFromAzConfig) && Files.exists(Path.of(valueFromAzConfig))) {
            funcCoreToolsPathList.add(valueFromAzConfig);
        }
        funcCoreToolsPathList.forEach(this::addItem);

        if (includeSettings) {
            this.addItem(OPEN_AZURE_SETTINGS);
        }
        if (StringUtils.isNotBlank(valueFromAzConfig)) {
            this.setValue(valueFromAzConfig);
        } else {
            this.setSelectedIndex(-1);
        }
    }

    @Override
    public void setValue(String value) {
        saveHistory();
        super.setValue(value);
    }

    private void onSelectFile(String lastFilePath) {
        final FileChooserDescriptor fileDescriptor =
            new FileChooserDescriptor(true, false, false, false, false, false);
        if (fileFilter != null) {
            fileDescriptor.withFileFilter(fileFilter);
        }
        fileDescriptor.withTitle("Select Path to Azure Functions Core Tools");
        final VirtualFile lastFile = lastFilePath != null && new File(lastFilePath).exists()
            ? LocalFileSystem.getInstance().findFileByIoFile(new File(lastFilePath)) : null;
        FileChooser.chooseFile(fileDescriptor, project, this, lastFile, (file) -> {
            if (file != null && file.exists()) {
                addOrSelectExistingVirtualFile(file);
            }
        });
    }

    @Nullable
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.OpenDisk, "Open file", () -> onSelectFile(getItem()));
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        try {
            final String selectFile = Paths.get(virtualFile.getPath()).toRealPath().toString();
            if (funcCoreToolsPathList.add(selectFile)) {
                this.addItem(selectFile);
            }
            this.setSelectedItem(selectFile);
        } catch (IOException e) {
            AzureMessager.getMessager().error(e);
        }
    }

    private List<String> loadHistory() {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final String history = propertiesComponent.getValue(AZURE_TOOLKIT_FUNCTION_CORE_TOOLS_HISTORY);
        if (history != null) {
            final String[] items = history.split("\n");
            List<String> result = new ArrayList<>();
            for (String item : items) {
                if (StringUtils.isNotBlank(item) && new File(item).exists()) {
                    try {
                        result.add(Paths.get(item).toRealPath().toString());
                    } catch (Exception ignore) {
                        // ignore since the history data is not important
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private void saveHistory() {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final List<String> subList = funcCoreToolsPathList.stream().skip(Math.max(funcCoreToolsPathList.size() - MAX_HISTORY_SIZE, 0))
            .collect(Collectors.toList());
        propertiesComponent.setValue(AZURE_TOOLKIT_FUNCTION_CORE_TOOLS_HISTORY, StringUtils.join(
            subList.toArray(), "\n"));
    }
}
