/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FunctionCoreToolsCombobox extends AzureComboBox<String> {
    private static final String AZURE_TOOLKIT_FUNCTION_CORE_TOOLS_HISTORY = "azure_toolkit.function.core.tools.history";
    private static final String OPEN_AZURE_SETTINGS = "Open Azure Settings";
    private static final int MAX_HISTORY_SIZE = 15;
    private final Set<String> funcCoreToolsPathList = new LinkedHashSet<>();

    private final Condition<? super VirtualFile> fileFilter;
    private final Project project;

    private String lastSelected;
    private final boolean includeSettings;
    private boolean pendingOpenAzureSettings = false;

    public FunctionCoreToolsCombobox(Project project, boolean includeSettings) {
        super(false);
        this.project = project;
        this.includeSettings = includeSettings;
        final List<String> exePostfix = Arrays.asList("exe|bat|cmd|sh|bin|run".split("\\|"));
        this.fileFilter = file -> Comparing.equal(file.getNameWithoutExtension(), "func", file.isCaseSensitive()) &&
            (file.getExtension() == null || exePostfix.contains(file.isCaseSensitive() ? file.getExtension() : StringUtils.lowerCase(file.getExtension())));
        this.reset();
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
                                FunctionCoreToolsCombobox.this.openAzureSettingsPanel();
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

    private void openAzureSettingsPanel() {
        final Action<Object> openSettingsAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
        final AnActionEvent event = AnActionEvent.createFromInputEvent(null, ActionPlaces.UNKNOWN, null, DataManager.getInstance().getDataContext(FunctionCoreToolsCombobox.this));
        openSettingsAction.getHandler(null, event).accept(null, event); // Open Azure Settings Panel sync
    }

    private String getDefaultFuncPath() {
        final String functionCoreToolsPath = Azure.az().config().getFunctionCoreToolsPath();
        return StringUtils.isNotEmpty(functionCoreToolsPath) && Files.exists(Path.of(functionCoreToolsPath)) ? functionCoreToolsPath : null;
    }

    @Nonnull
    @Override
    protected List<? extends String> loadItems() throws Exception {
        final List<String> result = new ArrayList<>();
        result.addAll(loadHistory());
        try {
            result.addAll(FunctionCliResolver.resolve());
        } catch (RuntimeException e) {
            // swallow exception while resolve function path
            // todo @hanli: handle the exception in lib
            log.warn(e.getMessage(), e);
        }
        result.add(getDefaultFuncPath());
        result.add(includeSettings ? OPEN_AZURE_SETTINGS : null);
        return result.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    public void reset() {
        final String defaultFuncPath = getDefaultFuncPath();
        if (StringUtils.isNotEmpty(defaultFuncPath)) {
            this.setValue(defaultFuncPath);
        }
        this.reloadItems();
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
        final VirtualFile lastFile = lastFilePath != null && new File(lastFilePath).exists() ?
            LocalFileSystem.getInstance().findFileByIoFile(new File(lastFilePath)) : null;
        FileChooser.chooseFile(fileDescriptor, project, this, lastFile, (file) -> {
            if (file != null && file.exists()) {
                addOrSelectExistingVirtualFile(file);
            }
        });
    }

    @Nonnull
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        final String tooltip = String.format("Open file (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension openEx = Extension.create(AllIcons.General.OpenDisk, tooltip, () -> onSelectFile(getItem()));
        this.registerShortcut(keyStroke, openEx);
        extensions.add(openEx);
        return extensions;
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        try {
            final String selectFile = Paths.get(virtualFile.getPath()).toRealPath().toString();
            if (funcCoreToolsPathList.add(selectFile)) {
                this.addItem(selectFile);
            }
            this.setSelectedItem(selectFile);
        } catch (final IOException e) {
            AzureMessager.getMessager().error(e);
        }
    }

    private List<String> loadHistory() {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final String history = propertiesComponent.getValue(AZURE_TOOLKIT_FUNCTION_CORE_TOOLS_HISTORY);
        if (history != null) {
            final String[] items = history.split("\n");
            final List<String> result = new ArrayList<>();
            for (final String item : items) {
                if (StringUtils.isNotBlank(item) && new File(item).exists()) {
                    try {
                        result.add(Paths.get(item).toRealPath().toString());
                    } catch (final Exception ignore) {
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
        propertiesComponent.setValue(AZURE_TOOLKIT_FUNCTION_CORE_TOOLS_HISTORY, StringUtils.join(
            funcCoreToolsPathList.stream().skip(Math.max(funcCoreToolsPathList.size() - MAX_HISTORY_SIZE, 0)).toArray(), "\n"));
    }
}
