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

package com.microsoft.intellij.runner.functions.deploy.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTable;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTableUtils;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;
import javax.swing.event.PopupMenuEvent;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.microsoft.intellij.runner.functions.AzureFunctionsConstants.EMPTY_TEXT;
import static com.microsoft.intellij.runner.functions.AzureFunctionsConstants.LOADING_TEXT;


public class FunctionDeploymentPanel extends AzureSettingPanel<FunctionDeployConfiguration> implements FunctionDeployMvpView {

    private static final String CREATE_NEW_FUNCTION_APP = "Create New FunctionApp";
    private static final String REFRESHING_FUNCTION_APP = "Refreshing...";
    private static final String CREATE_NEW_FUNCTION = "No available function, click to create a new one";

    private ResourceEx<FunctionApp> selectedFunctionApp = null;
    private FunctionDeployViewPresenter presenter = null;

    private JPanel pnlRoot;
    private JComboBox cbxFunctionApp;
    private HyperlinkLabel lblCreateFunctionApp;
    private JPanel pnlAppSettings;
    private TextFieldWithBrowseButton txtStagingFolder;
    private JComboBox<Module> cbFunctionModule;
    private AppSettingsTable appSettingsTable;

    // presenter
    private FunctionDeployConfiguration functionDeployConfiguration;

    public FunctionDeploymentPanel(@NotNull Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.functionDeployConfiguration = functionDeployConfiguration;
        this.presenter = new FunctionDeployViewPresenter();
        this.presenter.onAttachView(this);

        cbxFunctionApp.setRenderer(new FunctionAppCombineBoxRender(cbxFunctionApp));
        cbxFunctionApp.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onFunctionSelected();
            }
        });
        // Set the editor of combobox, otherwise it will use box render when popup is invisible, which may render the
        // combobox to twoline
        cbxFunctionApp.setEditor(new ComboBoxEditor() {
            private Object item;
            private JLabel label = new JLabel();
            private EventListenerList listenerList = new EventListenerList();

            @Override
            public Component getEditorComponent() {
                return label;
            }

            @Override
            public void setItem(Object anObject) {
                item = anObject;
                if (anObject == null) {
                    return;
                } else if (anObject instanceof String) {
                    label.setText((String) anObject);
                } else {
                    final ResourceEx<FunctionApp> function = (ResourceEx<FunctionApp>) anObject;
                    label.setText(function.getResource().name());
                }
                label.getAccessibleContext().setAccessibleName(label.getText());
                label.getAccessibleContext().setAccessibleDescription(label.getText());
            }

            @Override
            public Object getItem() {
                return item;
            }

            @Override
            public void selectAll() {
                return;
            }

            @Override
            public void addActionListener(ActionListener l) {
                listenerList.add(ActionListener.class, l);
            }

            @Override
            public void removeActionListener(ActionListener l) {
                listenerList.remove(ActionListener.class, l);
            }
        });

        cbFunctionModule.setRenderer(new ListCellRendererWrapper<Module>() {
            @Override
            public void customize(JList list, Module module, int i, boolean b, boolean b1) {
                if (module != null) {
                    setText(module.getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });

        cbFunctionModule.addItemListener(itemEvent -> {
            final String targetFolder = FunctionUtils.getTargetFolder((Module) cbFunctionModule.getSelectedItem());
            txtStagingFolder.setText(targetFolder);
        });

        fillModules();
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Deploy Azure Functions";
    }

    @Override
    public void disposeEditor() {
    }

    @Override
    public void beforeFillFunctionApps() {
        cbxFunctionApp.removeAllItems();
        cbxFunctionApp.setEnabled(false);
        cbxFunctionApp.addItem(REFRESHING_FUNCTION_APP);
    }

    @Override
    public synchronized void fillFunctionApps(List<ResourceEx<FunctionApp>> functionList) {
        cbxFunctionApp.removeAllItems();
        if (functionList.size() == 0) {
            lblCreateFunctionApp.setVisible(true);
            cbxFunctionApp.setVisible(false);
        } else {
            lblCreateFunctionApp.setVisible(false);
            cbxFunctionApp.setVisible(true);
            cbxFunctionApp.addItem(CREATE_NEW_FUNCTION_APP);
            functionList.forEach(functionAppResourceEx -> cbxFunctionApp.addItem(functionAppResourceEx));
            // Find function which id equals to configuration, or use the first available one.
            final ResourceEx<FunctionApp> selectedFunction = functionList.stream()
                    .filter(webAppResourceEx -> webAppResourceEx.getResource().id().equals(functionDeployConfiguration.getFunctionId()))
                    .findFirst().orElse(functionList.get(0));
            cbxFunctionApp.setSelectedItem(selectedFunction);
        }
        onFunctionSelected();
        cbxFunctionApp.setEnabled(true);
    }

    @Override
    public void beforeFillAppSettings() {
        appSettingsTable.getEmptyText().setText(LOADING_TEXT);
        appSettingsTable.clear();
    }

    @Override
    public void fillAppSettings(Map<String, String> appSettings) {
        appSettingsTable.getEmptyText().setText(EMPTY_TEXT);
        appSettingsTable.setAppSettings(appSettings);
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return new JComboBox<Artifact>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new JComboBox<MavenProject>();
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    @Override
    protected void resetFromConfig(@NotNull FunctionDeployConfiguration configuration) {
        configuration.setFunctionId(configuration.getFunctionId());
        final Module previousModule = configuration.getModule();
        if (previousModule != null) {
            for (int i = 0; i < cbFunctionModule.getItemCount(); i++) {
                final Module module = cbFunctionModule.getItemAt(i);
                if (Paths.get(module.getModuleFilePath()).equals(Paths.get(previousModule.getModuleFilePath()))) {
                    cbFunctionModule.setSelectedIndex(i);
                    break;
                }
            }
        }
        presenter.loadFunctionApps(false);
    }

    @Override
    protected void apply(@NotNull FunctionDeployConfiguration configuration) {
        if (selectedFunctionApp == null || selectedFunctionApp.getResource() == null) {
            return;
        }
        configuration.setTargetFunction(selectedFunctionApp.getResource());
        configuration.setAppSettings(appSettingsTable.getAppSettings());
        configuration.setModule((Module) cbFunctionModule.getSelectedItem());
        configuration.setDeploymentStagingDirectory(txtStagingFolder.getText());
    }

    private void onFunctionSelected() {
        final Object value = cbxFunctionApp.getSelectedItem();
        if (value != null && value instanceof ResourceEx) {
            selectedFunctionApp = (ResourceEx<FunctionApp>) cbxFunctionApp.getSelectedItem();
            presenter.loadAppSettings(selectedFunctionApp.getResource());
        } else if (Comparing.equal(CREATE_NEW_FUNCTION_APP, value)) {
            // Create new function app
            cbxFunctionApp.setSelectedItem(null);
            ApplicationManager.getApplication().invokeLater(() -> createNewFunctionApp());
        }
    }

    private void createNewFunctionApp() {
        // todo: add create function dialog
    }

    private void createUIComponents() {
        lblCreateFunctionApp = new HyperlinkLabel(CREATE_NEW_FUNCTION);
        lblCreateFunctionApp.addHyperlinkListener(e -> createNewFunctionApp());

        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new AppSettingsTable(localSettingPath);
        pnlAppSettings = AppSettingsTableUtils.createAppSettingPanel(appSettingsTable);
    }

    class FunctionAppCombineBoxRender extends ListCellRendererWrapper {

        private final JComboBox comboBox;
        private final int cellHeight;
        private static final String TEMPLATE_STRING = "<html><div>TEMPLATE</div><small>TEMPLATE</small></html>";

        public FunctionAppCombineBoxRender(JComboBox comboBox) {
            this.comboBox = comboBox;
            final JLabel template = new JLabel(TEMPLATE_STRING);
            //Create a multi-line jlabel and calculate its preferred size
            this.cellHeight = template.getPreferredSize().height;
        }

        @Override
        public void customize(JList list, Object value, int index, boolean b, boolean b1) {
            if (value == null) {
                return;
            } else if (value instanceof String) {
                setText(getStringLabelText((String) value));
            } else {
                final ResourceEx<FunctionApp> function = (ResourceEx<FunctionApp>) value;
                // For label in combobox textfield, just show function app name
                final String text = index >= 0 ? getFunctionAppLabelText(function.getResource()) : function.getResource().name();
                setText(text);
            }
            list.setFixedCellHeight(cellHeight);
        }

        private String getStringLabelText(String message) {
            return comboBox.isPopupVisible() ?
                    String.format("<html><div>%s</div><small></small></html>", message) : message;
        }

        private String getFunctionAppLabelText(FunctionApp functionApp) {
            final String name = functionApp.name();
            final String os = StringUtils.capitalize(functionApp.operatingSystem().toString());
            final String resourceGroup = functionApp.resourceGroupName();

            return comboBox.isPopupVisible() ? String.format("<html><div>%s</div></div><small>OS:%s " +
                    "ResourceGroup:%s</small></html>", name, os, resourceGroup) : name;
        }
    }

    private void fillModules() {
        Arrays.stream(FunctionUtils.listFunctionModules(project)).forEach(module -> cbFunctionModule.addItem(module));
    }
}
