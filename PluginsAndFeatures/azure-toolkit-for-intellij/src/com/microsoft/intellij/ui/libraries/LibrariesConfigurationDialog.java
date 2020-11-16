/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class LibrariesConfigurationDialog extends AzureDialogWrapper {
    private JPanel contentPane;
    private JPanel librariesPanel;
    private JBList librariesList;

    private List<AzureLibrary> currentLibs;
    private List<AzureLibrary> tempList;
    private Module module;

    public LibrariesConfigurationDialog(Module module, List<AzureLibrary> currentLibs) {
        super(true);
        this.currentLibs = currentLibs;
        this.module = module;
        init();
    }

    @Override
    protected void init() {
        librariesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        librariesList = new JBList(currentLibs);
        tempList = new ArrayList(currentLibs);
        librariesPanel = ToolbarDecorator.createDecorator(librariesList)
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addLibrary();
                    }
                }).setRemoveAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        removeLibrary();
                    }
                }).setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        editLibrary();
                    }
                }).disableUpDownActions().createPanel();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        Action action = getOKAction();
        action.putValue(Action.NAME, "Close");
        return new Action[]{action};
    }

    private void addLibrary() {
        AddLibraryWizardModel model = new AddLibraryWizardModel(module);
        AddLibraryWizardDialog wizard = new AddLibraryWizardDialog(model);

        wizard.setTitle(message("addLibraryTitle"));
        wizard.show();
        if (wizard.isOK()) {
            AzureLibrary azureLibrary = model.getSelectedLibrary();
            final LibrariesContainer.LibraryLevel level = LibrariesContainer.LibraryLevel.MODULE;

            AccessToken token = WriteAction.start();
            try {
                final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
                for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
                    if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                            && azureLibrary.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                        AzureTaskRunner.getInstance().runLater(() -> PluginUtil.displayErrorDialog(message(
                            "error"), message("libraryExistsError")));
                        return;
                    }
                }

                Library newLibrary = LibrariesContainerFactory.createContainer(modifiableModel).createLibrary(azureLibrary.getName(), level, new ArrayList<OrderRoot>());
                if (model.isExported()) {
                    for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
                        if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                                && azureLibrary.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                            ((ModuleLibraryOrderEntryImpl) orderEntry).setExported(true);
                            break;
                        }
                    }
                }
                Library.ModifiableModel newLibraryModel = newLibrary.getModifiableModel();
                // if there is separate resources folder
                if (azureLibrary.getLocation() != null) {
                    File file = new File(String.format("%s%s%s", AzurePlugin.pluginFolder, File.separator, azureLibrary.getLocation()));
                    AddLibraryUtility.addLibraryRoot(file, newLibraryModel);
                }
                // if some files already contained in plugin dependencies, take them from there - true for azure sdk library
                if (azureLibrary.getFiles().length > 0) {
                    AddLibraryUtility.addLibraryFiles(new File(PluginHelper.getAzureLibLocation()), newLibraryModel, azureLibrary.getFiles());
                }
                newLibraryModel.commit();
                modifiableModel.commit();
                ((DefaultListModel) librariesList.getModel()).addElement(azureLibrary);
                tempList.add(azureLibrary);
            } catch (Exception ex) {
                PluginUtil.displayErrorDialogAndLog(message("error"), message("addLibraryError"), ex);
            } finally {
                token.finish();
            }
            LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        }
    }

    private void removeLibrary() {
        AzureLibrary azureLibrary = (AzureLibrary) librariesList.getSelectedValue();
        AccessToken token = WriteAction.start();
        try {
            final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
            modifiableModel.getModuleLibraryTable().removeLibrary(modifiableModel.getModuleLibraryTable().getLibraryByName(azureLibrary.getName()));
            modifiableModel.commit();
        } finally {
            token.finish();
        }
        ((DefaultListModel) librariesList.getModel()).removeElement(azureLibrary);
        tempList.remove(azureLibrary);
    }

    private void editLibrary() {
        AzureLibrary azureLibrary = (AzureLibrary) librariesList.getSelectedValue();
        final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        OrderEntry libraryOrderEntry = null;
        for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
            if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                    && azureLibrary.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                libraryOrderEntry = orderEntry;
                break;
            }
        }
        if (libraryOrderEntry != null) {
            LibraryPropertiesPanel libraryPropertiesPanel = new LibraryPropertiesPanel(module, azureLibrary, true,
                    ((ModuleLibraryOrderEntryImpl) libraryOrderEntry).isExported());
            DefaultDialogWrapper libraryProperties = new DefaultDialogWrapper(module.getProject(), libraryPropertiesPanel);
            libraryProperties.show();
            if (libraryProperties.isOK()) {
                AccessToken token = WriteAction.start();
                try {
                    ((ModuleLibraryOrderEntryImpl) libraryOrderEntry).setExported(libraryPropertiesPanel.isExported());
                    modifiableModel.commit();
                } finally {
                    token.finish();
                }
                LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
            }
        } else {
            PluginUtil.displayInfoDialog("Library not found", "Library was not found");
        }
    }

    @Override
    protected void doOKAction() {
        for (AzureLibrary lib : tempList) {
            if (!currentLibs.contains(lib)) {
                if (lib.getName().equalsIgnoreCase(AzureLibrary.AZURE_LIBRARIES.toString())) {
                    AppInsightsClient.create("Azure Libraries", AzurePlugin.AZURE_LIBRARIES_VERSION);
                } else if (lib.getName().equalsIgnoreCase(AzureLibrary.SQL_JDBC.toString())) {
                    AppInsightsClient.create("Microsoft JDBC Driver", AzurePlugin.JDBC_LIBRARIES_VERSION);
                }
            }
        }
        super.doOKAction();
    }
}
