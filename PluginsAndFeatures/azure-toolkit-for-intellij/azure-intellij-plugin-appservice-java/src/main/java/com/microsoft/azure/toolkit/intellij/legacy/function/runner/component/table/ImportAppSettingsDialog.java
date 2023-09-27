/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ToolbarDecorator;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ImportAppSettingsDialog extends AzureDialog<ImportAppSettingsDialog.Result>
        implements AzureForm<ImportAppSettingsDialog.Result> {
    public static final String LOADING_TEXT = "Loading...";
    public static final String EMPTY_TEXT = "Empty";
    public static final String REFRESH_TEXT = "Refreshing...";

    private static final String LOCAL_SETTINGS_JSON = "local.settings.json";
    private ComboBox<Object> cbAppSettingsSource;
    private AppSettingsTable tblAppSettings;
    private JLabel lblAppSettingsSource;
    private JPanel pnlAppSettings;
    private JCheckBox chkErase;
    private JPanel pnlContent;
    private JPanel pnlRoot;

    private final Project project;

    public ImportAppSettingsDialog(@Nullable final Project project) {
        super();
        this.project = project;
        setModal(true);
        // setMinimumSize(new Dimension(-1, 250));
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.cbAppSettingsSource.setUsePreferredSizeAsMinimum(false);
        this.cbAppSettingsSource.addItemListener(this::onSelectTarget);
    }

    private void onSelectTarget(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Object item = e.getItem();
            if (item instanceof FunctionApp) {
                this.fillFunctionAppSettings(()-> ((FunctionApp) item).getAppSettings());
            } else if (item instanceof VirtualFile) {
                final File file = ((VirtualFile) item).toNioPath().toFile();
                this.fillFunctionAppSettings(() -> FunctionAppSettingsTableUtils.getAppSettingsFromLocalSettingsJson(file));
            }
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlContent;
    }

    @Override
    public AzureForm<Result> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return message("function.appSettings.import.title");
    }

    private void fillLocalAppSettings(Path nioPath) {
    }

    public void fillFunctionAppSettings(final Supplier<Map<String,String>> supplier) {
        tblAppSettings.getEmptyText().setText(LOADING_TEXT);
        tblAppSettings.clear();
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final Map<String, String> appSettings = supplier.get();
            AzureTaskManager.getInstance().runLater(() -> {
                tblAppSettings.setAppSettings(appSettings);
                if (appSettings.size() == 0) {
                    tblAppSettings.getEmptyText().setText(EMPTY_TEXT);
                }
            }, AzureTask.Modality.ANY);
        });
    }

    private void createUIComponents() {
        tblAppSettings = new FunctionAppSettingsTable("");
        tblAppSettings.getEmptyText().setText(LOADING_TEXT);
        pnlAppSettings = ToolbarDecorator.createDecorator(tblAppSettings).createPanel();

        cbAppSettingsSource = new AzureComboBox<>() {
            @Override
            protected String getItemText(Object object) {
                if (object instanceof FunctionApp) {
                    return ((FunctionApp) object).getName();
                } else if (object instanceof VirtualFile) {
                    return ((VirtualFile) object).getPath();
                }
                return super.getItemText(object);
            }

            @Nullable
            @Override
            protected Icon getItemIcon(Object object) {
                if (object instanceof FunctionApp) {
                    return IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.MODULE);
                } else if (object instanceof VirtualFile) {
                    return AllIcons.FileTypes.Json;
                }
                return super.getItemIcon(object);
            }

            @Nonnull
            @Override
            protected List<?> loadItems() {
                final Project project = Optional.ofNullable(ImportAppSettingsDialog.this.project)
                        .orElseGet(()-> DataManager.getInstance().getDataContext(this).getData(CommonDataKeys.PROJECT));
                final Collection<VirtualFile> files = Objects.isNull(project) ? Collections.emptyList() :
                        ReadAction.compute(() -> FilenameIndex.getVirtualFilesByName("local.settings.json", GlobalSearchScope.projectScope(project)));
                final List<FunctionApp> functionApps = Azure.az(AzureFunctions.class).functionApps();
                return Stream.concat(files.stream(), functionApps.stream()).collect(Collectors.toList());
            }
        };
    }

    @Override
    public Result getValue() {
        return new Result(tblAppSettings.getAppSettings(), chkErase.isSelected());
    }

    @Override
    public void setValue(Result data) {
        this.chkErase.setSelected(data.eraseExistingSettings);
        Optional.ofNullable(data.getAppSettings()).ifPresent(tblAppSettings::setAppSettings);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.emptyList();
    }

    @Data
    @AllArgsConstructor
    static class Result {
        private Map<String, String> appSettings;
        private boolean eraseExistingSettings;
    }
}
