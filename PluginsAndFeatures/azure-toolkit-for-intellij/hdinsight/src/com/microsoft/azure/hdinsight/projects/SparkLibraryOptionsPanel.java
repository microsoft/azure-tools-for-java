/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.projects;

import com.intellij.facet.impl.ui.libraries.LibraryCompositionSettings;
import com.intellij.framework.library.FrameworkLibraryVersionFilter;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.impl.FileChooserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTypeServiceImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import com.intellij.openapi.roots.ui.OrderEntryAppearanceService;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.*;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NotNullComputable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.PathUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.hash.HashMap;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SparkLibraryOptionsPanel extends JPanel {
    private ComboBox comboBox = new ComboBox();
    private JButton button = new JButton("Select...");

    private Project project;
    private SparkLibraryDescription myLibraryDescription;
    private LibraryCompositionSettings mySettings;
    private LibrariesContainer librariesContainer;
    private List<String> cachedLibraryPath = new ArrayList<>();
    private List<Library> suitableLibraries;

    private Map<SparkLibraryInfo, LibraryEditorBase> libraryEditorMap = new HashMap<>();

    public LibraryCompositionSettings apply() {
        LibraryEditorBase editorBase = (LibraryEditorBase)this.comboBox.getSelectedItem();

        if(editorBase instanceof ExistingLibraryEditor) {
            this.mySettings.setSelectedExistingLibrary(((ExistingLibraryEditor) editorBase).getLibrary());
            this.mySettings.setNewLibraryEditor(null);
        } else if(editorBase instanceof NewLibraryEditor){
            this.mySettings.setNewLibraryEditor((NewLibraryEditor)editorBase);
            this.mySettings.setNewLibraryLevel(LibrariesContainer.LibraryLevel.GLOBAL);
            this.mySettings.setSelectedExistingLibrary(null);
        }
        this.mySettings.setLibraryProvider(null);

//        if(project == null && libraryEditorMap.containsValue(editorBase)) {
//            saveSetting(editorBase);
//        }

        return this.mySettings;
    }

    private List<Library> calculateSuitableLibraries() {
        ArrayList suitableLibraries = new ArrayList();
        Library[] libraries = this.librariesContainer.getAllLibraries();

        for(int i = 0; i < libraries.length; ++i) {
            Library library = libraries[i];
            PersistentLibraryKind type = ((LibraryEx)library).getKind();
            if(type != null && type instanceof SparkLibraryKind) {
                suitableLibraries.add(library);
            }
        }

        return suitableLibraries;
    }

    public SparkLibraryOptionsPanel(@Nullable final Project project,@NotNull LibrariesContainer librariesContainer, @NotNull SparkLibraryDescription myLibraryDescription) {
        this.project = project;
        this.myLibraryDescription = myLibraryDescription;
        this.librariesContainer = librariesContainer;
        this.suitableLibraries = calculateSuitableLibraries();

        mySettings = new LibraryCompositionSettings(myLibraryDescription, new NotNullComputable<String>() {
            @NotNull
            @Override
            public String compute() {
                return "";
            }
        }, FrameworkLibraryVersionFilter.ALL, new ArrayList());

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        add(comboBox);
        add(button);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(comboBox, constraints);

        GridBagConstraints constraints2 = new GridBagConstraints();
        constraints2.gridwidth = 0;
        constraints2.weightx = 0;
        constraints2.weighty = 0;
        constraints2.insets = new Insets(0, 10, 0, 0);
        layout.setConstraints(button, constraints2);

        this.comboBox.setRenderer(new ColoredListCellRenderer(this.comboBox) {
            protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
                if (value == null) {
                    this.append("[No library selected]");
                } else if (value instanceof ExistingLibraryEditor) {
                    Library name = ((ExistingLibraryEditor) value).getLibrary();
                    boolean invalid = !((LibraryEx) name).getInvalidRootUrls(OrderRootType.CLASSES).isEmpty();
                    OrderEntryAppearanceService.getInstance().forLibrary(project != null ? project : ProjectManager.getInstance().getDefaultProject(), name, invalid).customize(this);
                } else if (value instanceof NewLibraryEditor) {
                    this.setIcon(PlatformIcons.LIBRARY_ICON);
                    String name1 = ((NewLibraryEditor) value).getName();
                    this.append(name1 != null ? name1 : "<unnamed>");
                }
            }
        });

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCreate();
            }
        });

        initializeCachedSDK();
    }

    private String suggestUniqueLibraryName(@NotNull String baseName) {
        return PathUtil.getFileName(baseName);
    }

    private void initializeCachedSDK() {
        for (int i = 0; i < suitableLibraries.size(); ++i) {
            ExistingLibraryEditor existingLibraryEditor = new ExistingLibraryEditor(suitableLibraries.get(i), null);
            this.comboBox.addItem(existingLibraryEditor);
        }

        if (project == null || suitableLibraries.size() == 0) {
            for (int i = 0; i < cachedLibraryPath.size(); ++i) {
                NewLibraryEditor editor = getNewLibraryEditor(cachedLibraryPath.get(i));
                if (editor != null) {
                    comboBox.addItem(editor);
                    try {
                        SparkLibraryInfo info = new SparkLibraryInfo(cachedLibraryPath.get(i));
                        libraryEditorMap.put(info, editor);
                    } catch (Exception e) {
                        //do nothing if we can not get the library info
                    }
                }
            }

            if (comboBox.getItemCount() == 0) {
                comboBox.addItem((Object) null);
            }

            comboBox.setSelectedIndex(0);
        }
    }

    private void saveSetting(LibraryEditorBase editorBase) {
        cachedLibraryPath.clear();
        for (Map.Entry<SparkLibraryInfo, LibraryEditorBase> entry : libraryEditorMap.entrySet()) {
            String str = entry.getKey().getLocalPath();
            if(!StringHelper.isNullOrWhiteSpace(str)) {
                if(entry.getValue() == editorBase) {
                    cachedLibraryPath.add(0, entry.getKey().getLocalPath());
                } else {
                    cachedLibraryPath.add(entry.getKey().getLocalPath());
                }
            }
        }
    }

    private NewLibraryEditor getNewLibraryEditor(@NotNull String path) {
        if (StringHelper.isNullOrWhiteSpace(path)) {
            return null;
        }

        VirtualFile root = LocalFileSystem.getInstance().findFileByPath(path);
        if (root == null) {
            return null;
        }

        final FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(false, false, true, false, true, false);
        VirtualFile[] libraryFiles = VfsUtilCore.toVirtualFileArray(FileChooserUtil.getChosenFiles(chooserDescriptor, Arrays.asList(root)));

        try {
            chooserDescriptor.validateSelectedFiles(libraryFiles);
        } catch (Exception exception) {
            //do noting if check failed
            return null;
        }

        final List<OrderRoot> roots = RootDetectionUtil.detectRoots(Arrays.asList(libraryFiles), null, null, new DefaultLibraryRootsComponentDescriptor());
        if (roots.isEmpty()) {
            return null;
        }

        NewLibraryConfiguration configuration = new NewLibraryConfiguration(LibraryTypeServiceImpl.suggestLibraryName(roots), SparkLibraryType.getInstance(), new SparkLibraryProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor libraryEditor) {
                libraryEditor.addRoots(roots);
            }
        };

        if (configuration != null) {
            NewLibraryEditor libraryEditor = new NewLibraryEditor(configuration.getLibraryType(), configuration.getProperties());
            libraryEditor.setName(suggestUniqueLibraryName(configuration.getDefaultLibraryName()));
            configuration.addRoots(libraryEditor);

            return libraryEditor;
        }

        return null;
    }

    private void doCreate() {
        VirtualFile root = LocalFileSystem.getInstance().findFileByPath(PluginUtil.getPluginRootDirectory());
        NewLibraryConfiguration libraryConfiguration = this.myLibraryDescription.createNewLibrary(this.button, root);

        if (libraryConfiguration != null) {
            NewLibraryEditor libraryEditor = new NewLibraryEditor(libraryConfiguration.getLibraryType(), libraryConfiguration.getProperties());
            libraryEditor.setName(suggestUniqueLibraryName(libraryConfiguration.getDefaultLibraryName()));
            libraryConfiguration.addRoots(libraryEditor);

            try {
                SparkLibraryInfo info = new SparkLibraryInfo(myLibraryDescription.getLocalPath());
                if(info != null) {
                    libraryEditorMap.put(info, libraryEditor);
                }
            } catch (Exception e) {
                //do nothing if we can not get the library info
            }

            if (this.comboBox.getItemAt(0) == null) {
                this.comboBox.remove(0);
            }

            this.comboBox.addItem(libraryEditor);
            this.comboBox.setSelectedItem(libraryEditor);
        }
    }
}
