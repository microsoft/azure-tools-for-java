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

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.application.options.ModuleDescriptionsComboBox;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.util.JreVersionDetector;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.MacroAwareTextBrowseFolderListener;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.hdinsight.spark.common.SparkLocalRunConfigurableModel;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

public class SparkLocalRunParamsPanel {
    public static final String HADOOP_HOME_ENV = "HADOOP_HOME";
    public static final String WINUTILS_EXE_NAME = "winutils.exe";

    private JPanel myWholePanel;
    private SparkLocalRunCommonParametersPanel myCommonProgramParameters;
    private JCheckBox myParallelExecutionCheckbox;
    private TextFieldWithBrowseButton myWinutilsPathTextFieldWithBrowserButton;
    private TextFieldWithBrowseButton myDataRootDirectoryFieldWithBrowseButton;
    private JBTextField myDataDefaultDirectory;
    private JBTextField myHadoopUserDefaultDirectoryLabel;
    private JPanel myWinutilsLocationPanel;
    private ModuleDescriptionsComboBox modules;
    private LabeledComponent<ModuleDescriptionsComboBox> myClasspathModule;
    @Nullable
    private ConfigurationModuleSelector myModuleSelector;

    @Nullable
    private JComponent myAnchor;
    @NotNull
    private final JreVersionDetector myVersionDetector;
    @NotNull
    private final Project myProject;

    public SparkLocalRunParamsPanel(@NotNull final Project project) {
        this.myProject = project;
        myVersionDetector = new JreVersionDetector();

        myAnchor = UIUtil.mergeComponentsWithAnchor(myCommonProgramParameters, myClasspathModule);

        // Connect the workingDirectory update event with dataRootDirectory update
        myCommonProgramParameters.addWorkingDirectoryUpdateListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent event) {
                String workingDirectory = myCommonProgramParameters.getWorkingDirectory();

                myDataRootDirectoryFieldWithBrowseButton.setText(Paths.get(workingDirectory, "data").toString());
            }
        });

        // Update other data directory texts
        myDataRootDirectoryFieldWithBrowseButton.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                myDataDefaultDirectory.setText(
                        Paths.get(myDataRootDirectoryFieldWithBrowseButton.getText(), "__default__").toString());
                myHadoopUserDefaultDirectoryLabel.setText(
                    Paths.get(FilenameUtils.getFullPath(myDataDefaultDirectory.getText()), "user", "current").toString());
            }
        });

        // Bind the folder file chooser for data root directory
        FileChooserDescriptor dataRootDirectoryChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        myDataRootDirectoryFieldWithBrowseButton.addBrowseFolderListener(
                new MacroAwareTextBrowseFolderListener(dataRootDirectoryChooser, myProject));

        // Winutils.exe setting, only for windows
        if (SystemUtils.IS_OS_WINDOWS) {
            updateWinUtilsPathTextField(System.getenv(HADOOP_HOME_ENV));
        } else {
            myWinutilsLocationPanel.setVisible(false);
        }

        // Set name for telemetry listener purpose
        myWinutilsPathTextFieldWithBrowserButton.getTextField().setName("winUtilsText");
        myWinutilsPathTextFieldWithBrowserButton.getButton().setName("winUtilsButton");
        myDataRootDirectoryFieldWithBrowseButton.getTextField().setName("dataRootPathText");
        myDataRootDirectoryFieldWithBrowseButton.getButton().setName("dataRootPathButton");
    }

    public SparkLocalRunParamsPanel withInitialize() {
        myModuleSelector = new ConfigurationModuleSelector(myProject, myClasspathModule.getComponent());
        myCommonProgramParameters.setModuleContext(myModuleSelector.getModule());
        modules.setSelectedModule(
                Arrays.stream(ModuleManager.getInstance(myProject).getModules())
                        .filter(module -> module.getName().equalsIgnoreCase(myProject.getName()))
                        .findFirst()
                        .orElse(null));

        return this;
    }

    private void updateWinUtilsPathTextField(@Nullable String hadoopHomeEnv) {
        String windUtilsPath = Optional.ofNullable(hadoopHomeEnv)
                .map(hadoopHome -> Paths.get(hadoopHome, "bin", WINUTILS_EXE_NAME).toString())
                .map(File::new)
                .filter(File::exists)
                .map(File::toString)
                .orElse("");

        myWinutilsPathTextFieldWithBrowserButton.setText(windUtilsPath);

        // Bind winutils.exe file chooser
        FileChooserDescriptor winUtilsFileChooser =
                new FileChooserDescriptor(true, false, false, false, false, false)
                        .withFileFilter(file -> file.getName().equals(WINUTILS_EXE_NAME) && file.getParent().getName().equals("bin"));

        myWinutilsPathTextFieldWithBrowserButton.addBrowseFolderListener(
                new MacroAwareTextBrowseFolderListener(winUtilsFileChooser, myProject));

    }

    private void createUIComponents() {
        modules = new ModuleDescriptionsComboBox();
        modules.setAllModulesFromProject(myProject);
        myClasspathModule = LabeledComponent.create(modules, "Use classpath of module:");
    }

    public void setData(@NotNull SparkLocalRunConfigurableModel data) {
        // Data -> Component
        myParallelExecutionCheckbox.setSelected(data.isIsParallelExecution());
        myCommonProgramParameters.reset(data);

        final String classpathModuleNameToSet = data.getClasspathModule();
        if (classpathModuleNameToSet != null) {
            modules.setSelectedModule(myProject, classpathModuleNameToSet);
        }

        if (!data.getDataRootDirectory().trim().isEmpty()) {
            myDataRootDirectoryFieldWithBrowseButton.setText(data.getDataRootDirectory());
        }

        Optional.ofNullable(myCommonProgramParameters.getEnvs().get(HADOOP_HOME_ENV))
                .ifPresent(this::updateWinUtilsPathTextField);
    }

    public void getData(@NotNull SparkLocalRunConfigurableModel data) {
        // Component -> Data
        data.setIsParallelExecution(myParallelExecutionCheckbox.isSelected());
        myCommonProgramParameters.applyTo(data);
        data.setDataRootDirectory(myDataRootDirectoryFieldWithBrowseButton.getText());

        data.setClasspathModule(Optional.ofNullable(myModuleSelector)
                                        .map(ConfigurationModuleSelector::getModuleName)
                                        .orElse(null));

        Optional.of(myWinutilsPathTextFieldWithBrowserButton.getText())
                .map((winUtilsFilePath) -> Paths.get(winUtilsFilePath))
                .filter(path -> path.endsWith(Paths.get("bin", WINUTILS_EXE_NAME)))
                .map(path -> path.getParent().getParent().toString())
                .ifPresent(hadoopHome -> data.getEnvs().put(HADOOP_HOME_ENV, hadoopHome));
    }
}
