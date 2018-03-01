package com.microsoft.azuretools.azureexplorer.forms;

import java.net.URL;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
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
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.rxjava.EclipseSchedulers;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

public class AddNewClusterForm extends AzureTitleAreaDialogWrapper implements SettableControl<AddNewClusterModel> {
    private Text clusterNameField;
    private Text userNameField;
    private Text storageNameField;
    private Text storageKeyField;
    private Combo containersComboBox;
    private Text passwordField;

    private HDInsightRootModule hdInsightModule;

    private Label clusterNameLabel;
    private Label userNameLabel;
    private Label passwordLabel;

    public AddNewClusterForm(Shell parentShell, HDInsightRootModule module) {
        super(parentShell);
        this.hdInsightModule = module;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setSize(439, 415);
        newShell.setText("Link New HDInsight Cluster");

    }

    private AddNewClusterCtrlProvider prepareCtrl() {
        AddNewClusterModel current = new AddNewClusterModel();

        getData(current);

        return new AddNewClusterCtrlProvider(current);
    }

    private void refreshContainers() {
        prepareCtrl()
                .refreshContainers()
                .subscribeOn(EclipseSchedulers.processBarVisibleAsync("Getting storage account containers..."))
                .observeOn(EclipseSchedulers.dispatchThread())
                .subscribe(this::setData);
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Link New HDInsight Cluster");
        setMessage("Please enter HDInsight Cluster details");
        // enable help button
        setHelpAvailable(true);
        
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

        Group clusterStorageGroup = new Group(container, SWT.NONE);
        clusterStorageGroup.setText("The Cluster Storage Information");
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
        
        return super.createDialogArea(parent);
    }

    @Override
    protected void okPressed() {
        prepareCtrl().validateAndAdd()
                .subscribeOn(EclipseSchedulers.processBarVisibleAsync("Validating the cluster settings..."))
                .observeOn(EclipseSchedulers.dispatchThread())
                .doOnNext(this::setData)
                .map(AddNewClusterModel::getErrorMessage)
                .filter(StringUtils::isEmpty)
                .subscribe(toUpdate -> {
                    hdInsightModule.refreshWithoutAsync();
                    AppInsightsClient.create(Messages.HDInsightAddNewClusterAction, null);

                    super.okPressed();
                },
                           err -> {
                               setErrorMessage(err.getMessage());
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

        getShell().layout(true, true);
    }
}
