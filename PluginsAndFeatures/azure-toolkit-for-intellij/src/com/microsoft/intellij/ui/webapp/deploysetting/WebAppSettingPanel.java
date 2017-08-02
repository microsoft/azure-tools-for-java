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

package com.microsoft.intellij.ui.webapp.deploysetting;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppSettingModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WebAppSettingPanel implements WebAppDeployMvpView {

    // presenter
    private final WebAppDeployViewPresenter<WebAppSettingPanel> webAppDeployViewPresenter;

    // cache variable
    private ResourceEx<WebApp> selectedWebApp = null;
    private Project project;
    private List<ResourceEx<WebApp>> cachedWebAppList = null;

    // const
    private static final String URL_PREFIX = "https://";
    private static final String GO_TO_PORTAL = "Go to Portal";

    //widgets
    private JPanel rootPanel;
    private JRadioButton rdoUseExist;
    private JRadioButton rdoCreateNew;
    private JPanel pnlExist;
    private JPanel pnlCreate;
    private JCheckBox chkToRoot;
    private JPanel pnlWebAppTable;
    private JTable table;

    public WebAppSettingPanel(Project project) {
        this.project = project;
        this.webAppDeployViewPresenter = new WebAppDeployViewPresenter<>();
        this.webAppDeployViewPresenter.onAttachView(this);

        final ButtonGroup btnGrp = new ButtonGroup();
        btnGrp.add(rdoUseExist);
        btnGrp.add(rdoCreateNew);
        rdoUseExist.setSelected(true);
        rdoUseExist.addActionListener(e -> togglePanel(true /*showUsingExisting*/));
        rdoCreateNew.setSelected(false);
        rdoCreateNew.addActionListener(e -> togglePanel(false /*showUsingExisting*/));
        togglePanel(true /*showUsingExisting*/);
    }

    public JPanel getMainPanel() {
        return rootPanel;
    }

    public void resetEditorForm(WebAppSettingModel model) {
        chkToRoot.setSelected(model.isDeployToRoot());
        this.webAppDeployViewPresenter.onRefresh(false /*forceRefresh*/);
    }

    @Override
    public void renderWebAppsTable(@NotNull List<ResourceEx<WebApp>> webAppLists) {
        List<ResourceEx<WebApp>> sortedList = webAppLists.stream()
                .filter(resource -> resource.getResource().javaVersion() != JavaVersion.OFF)
                .sorted(byWebAppName)
                .collect(Collectors.toList());
        cachedWebAppList = sortedList;
        if (sortedList.size() > 0) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.getDataVector().clear();
            for (ResourceEx<WebApp> resource: sortedList) {
                WebApp app = resource.getResource();
                model.addRow(new String[]{
                        app.name(),
                        app.javaVersion().toString(),
                        app.javaContainer() + " " + app.javaContainerVersion(),
                        app.resourceGroupName(),
                });
            }
            model.fireTableDataChanged();
        }
    }

    public String getTargetPath() {
        List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getRootProjects();
        return new File(mavenProjects.get(0).getBuildDirectory()).getPath()
                + File.separator + mavenProjects.get(0).getFinalName() + "." + MavenConstants.TYPE_WAR;
    }

    public String getTargetName() {
        return MavenProjectsManager.getInstance(project).getRootProjects()
                .get(0).getFinalName() + "." + MavenConstants.TYPE_WAR;
    }

    public String getSelectedWebAppId() {
        return selectedWebApp == null ? "" : selectedWebApp.getResource().id();
    }

    public String getSubscriptionIdOfSelectedWebApp() {
        return selectedWebApp == null ? "" : selectedWebApp.getSubscriptionId();
    }

    public String getWebAppUrl() {
        return selectedWebApp == null ? "" : URL_PREFIX + selectedWebApp.getResource().defaultHostName();
    }

    public boolean isDeployToRoot() {
        return chkToRoot.isSelected();
    }

    public boolean isCreatingNew() {
        return rdoCreateNew.isSelected();
    }

    private void togglePanel(boolean showUsingExisting) {
        pnlExist.setVisible(showUsingExisting);
        pnlCreate.setVisible(!showUsingExisting);
    }

    private void resetWidget() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.getDataVector().clear();
        model.fireTableDataChanged();
    }

    private Comparator<ResourceEx<WebApp>> byWebAppName = Comparator.comparing(webAppInfo -> webAppInfo.getResource().name());

    private void createUIComponents() {
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn("Name");
        tableModel.addColumn("JDK");
        tableModel.addColumn("Web container");
        tableModel.addColumn("Resource group");

        table = new JBTable(tableModel);

        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(event -> {
            if (cachedWebAppList != null) {
                selectedWebApp = cachedWebAppList.get(table.getSelectedRow());
            }
        });

        AnActionButton refreshAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                resetWidget();
                webAppDeployViewPresenter.onRefresh(true /*forceRefresh*/);
            }
        };

        ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraActions(refreshAction);

        pnlWebAppTable = tableToolbarDecorator.createPanel();
    }
}
