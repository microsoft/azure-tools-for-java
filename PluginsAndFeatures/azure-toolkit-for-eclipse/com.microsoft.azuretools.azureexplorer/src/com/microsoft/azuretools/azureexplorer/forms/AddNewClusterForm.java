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
package com.microsoft.azuretools.azureexplorer.forms;

import java.net.URL;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterCtrlProvider;
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterModel;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.rxjava.EclipseSchedulers;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

public class AddNewClusterForm extends AzureTitleAreaDialogWrapper implements SettableControl<AddNewClusterModel> {
    @NotNull
    private AddNewClusterCtrlProvider ctrlProvider;

    protected Text clusterNameField;
    private Text userNameField;
    private Text storageNameField;
    private Text storageKeyField;
    private Combo containersComboBox;
    private Text passwordField;

    @Nullable
    private HDInsightRootModule hdInsightModule;

    private Label clusterNameLabel;
    private Label userNameLabel;
    private Label passwordLabel;

    public AddNewClusterForm(Shell parentShell, @Nullable HDInsightRootModule module) {
        super(parentShell);
        // enable help button
        setHelpAvailable(true);

        this.hdInsightModule = module;
        this.ctrlProvider = new AddNewClusterCtrlProvider(this, new EclipseSchedulers(Activator.PLUGIN_ID));
    }

    private void refreshContainers() {
        ctrlProvider.refreshContainers()
                .subscribe();
    }

    protected void customizeUI() {
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Link New HDInsight Cluster");
        setMessage("Please enter HDInsight Cluster details");

        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 350;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        container.setLayoutData(gridData);

        clusterNameLabel = new Label(container, SWT.LEFT);
        clusterNameLabel.setText("Cluster Name:");
        gridData = new GridData();
        gridData.horizontalIndent = 30;
        gridData.horizontalAlignment = SWT.RIGHT;
        clusterNameLabel.setLayoutData(gridData);
        clusterNameField = new Text(container, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        clusterNameField.setLayoutData(gridData);
        clusterNameField.setToolTipText("The HDInsight cluster name, such as 'mycluster' of cluster URL 'mycluster.azurehdinsight.net'.\n\n Press the F1 key or click the '?'(Help) button to get more details.");

        Group clusterAccountGroup = new Group(container, SWT.NONE);
        clusterAccountGroup.setText("The Cluster Account");
        gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        clusterAccountGroup.setLayout(gridLayout);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.widthHint = 350;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        clusterAccountGroup.setLayoutData(gridData);

        userNameLabel = new Label(clusterAccountGroup, SWT.LEFT);
        userNameLabel.setText("User Name:");
        gridData = new GridData();
        gridData.horizontalIndent = 38;
        gridData.horizontalAlignment = SWT.RIGHT;
        userNameLabel.setLayoutData(gridData);
        userNameField = new Text(clusterAccountGroup, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        userNameField.setLayoutData(gridData);
        userNameField.setToolTipText("The user name of the HDInsight cluster.\n\n Press the F1 key or click the '?'(Help) button to get more details.");

        passwordLabel = new Label(clusterAccountGroup, SWT.LEFT);
        passwordLabel.setText("Password:");
        gridData = new GridData();
        gridData.horizontalIndent = 38;
        gridData.horizontalAlignment = SWT.RIGHT;
        passwordLabel.setLayoutData(gridData);
        passwordField = new Text(clusterAccountGroup, SWT.PASSWORD | SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        passwordField.setLayoutData(gridData);
        passwordField.setToolTipText("The password of the HDInsight cluster user provided.\n\n Press the F1 key or click the '?'(Help) button to get more details.");

        Group clusterStorageGroup = new Group(container, SWT.NONE);
        clusterStorageGroup.setText("The Cluster Storage Information (Optional)");
        clusterStorageGroup.setToolTipText("The Cluster Storage Information provided can enable the Storage Explorer support in the left tree view.");
        gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        clusterStorageGroup.setLayout(gridLayout);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.widthHint = 350;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        clusterStorageGroup.setLayoutData(gridData);

        Label storageNameLabel = new Label(clusterStorageGroup, SWT.LEFT);
        storageNameLabel.setText("Storage Account:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        storageNameLabel.setLayoutData(gridData);
        storageNameField = new Text(clusterStorageGroup, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        storageNameField.setLayoutData(gridData);
        storageNameField.setToolTipText("The default storage account of the HDInsight cluster, which can be found from HDInsight cluster properties of Azure portal.\n\n Press the F1 key or click the '?'(Help) button to get more details");

        Label storageKeyLabel = new Label(clusterStorageGroup, SWT.LEFT);
        storageKeyLabel.setText("Storage Key:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        storageKeyLabel.setLayoutData(gridData);
        storageKeyField = new Text(clusterStorageGroup, SWT.BORDER | SWT.WRAP);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.heightHint = 4 * storageKeyField.getLineHeight();
        storageKeyField.setLayoutData(gridData);
        storageKeyField.setToolTipText("The storage key of the default storage account, which can be found from HDInsight cluster storage accounts of Azure portal.\n\n Press the F1 key or click the '?'(Help) button to get more details.");

        storageNameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                refreshContainers();
            }
        });

        storageKeyField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                refreshContainers();
            }
        });

        Label storageContainerLabel = new Label(clusterStorageGroup, SWT.LEFT);
        storageContainerLabel.setText("Storage Container:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        storageContainerLabel.setLayoutData(gridData);
        containersComboBox = new Combo(clusterStorageGroup, SWT.DROP_DOWN | SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        containersComboBox.setLayoutData(gridData);

        container.addHelpListener(new HelpListener() {
            @Override public void helpRequested(HelpEvent e) {
                try {
                    IWebBrowser webBrowser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                    if (webBrowser != null)
                        webBrowser.openURL(new URL("https://go.microsoft.com/fwlink/?linkid=866472"));
                } catch (Exception ignored) {
                }
            }
        });

        customizeUI();
        return container;
    }

    protected void afterOkActionPerformed() {
        if (hdInsightModule != null) {
            hdInsightModule.load(false);
        }
    }

    @Override
    protected void okPressed() {
        ctrlProvider.validateAndAdd()
                .subscribe(toUpdate -> {
                    afterOkActionPerformed();
                    AppInsightsClient.create(Messages.HDInsightAddNewClusterAction, null);

                    super.okPressed();
                });
    }

    @Override
    public void getData(AddNewClusterModel data) {
        // Components -> Data
        data.setClusterName(clusterNameField.getText()).setClusterNameLabelTitle(clusterNameLabel.getText())
                .setUserName(userNameField.getText()).setUserNameLabelTitle(userNameLabel.getText())
                .setPassword(passwordField.getText()).setPasswordLabelTitle(passwordLabel.getText())
                .setStorageName(storageNameField.getText()).setStorageKey(storageKeyField.getText())
                .setErrorMessage(getErrorMessage()).setSelectedContainerIndex(containersComboBox.getSelectionIndex())
                .setContainers(Arrays.asList(containersComboBox.getItems()));
    }

    @Override
    public void setData(AddNewClusterModel data) {
        // Data -> Components

        // Text fields
        clusterNameField.setText(data.getClusterName());
        clusterNameLabel.setText(data.getClusterNameLabelTitle());
        userNameField.setText(data.getUserName());
        userNameLabel.setText(data.getUserNameLabelTitle());
        passwordField.setText(data.getPassword());
        passwordLabel.setText(data.getPasswordLabelTitle());
        storageNameField.setText(data.getStorageName());
        storageKeyField.setText(data.getStorageKey());
        setErrorMessage(data.getErrorMessage());

        // Combo box
        containersComboBox.removeAll();
        data.getContainers().forEach(containersComboBox::add);
        containersComboBox.select(data.getSelectedContainerIndex());

        // Resize layout
        getShell().layout(true, true);
    }
}
