package com.microsoft.azuretools.azureexplorer.forms;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.serverexplore.AddHDInsightAdditionalClusterImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

public class AddNewClusterForm extends AzureTitleAreaDialogWrapper {

	private String clusterName;
	private String userName;
	private String password;

	private String storageName;
	private String storageKey;

	private HDStorageAccount storageAccount;

	private boolean isCarryOnNextStep;

	private Text clusterNameField;
	private Text userNameField;
	private Text storageNameField;
	private Text storageKeyField;
	private Text passwordField;

	private HDInsightRootModule hdInsightModule;

	private static final String URL_PREFIX = "https://";

	public AddNewClusterForm(Shell parentShell, HDInsightRootModule module) {
		super(parentShell);
		this.hdInsightModule = module;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Link New HDInsight Cluster");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Link New HDInsight Cluster");
		setMessage("Please enter HDInsight Cluster details");
		setHelpAvailable(false);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		container.setLayout(gridLayout);
		GridData gridData = new GridData();
		gridData.widthHint = 350;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		container.setLayoutData(gridData);

		Label clusterNameLabel = new Label(container, SWT.LEFT);
		clusterNameLabel.setText("Cluster Name:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		clusterNameLabel.setLayoutData(gridData);
		clusterNameField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		clusterNameField.setLayoutData(gridData);

		Label storageNameLabel = new Label(container, SWT.LEFT);
		storageNameLabel.setText("Storage Name:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		storageNameLabel.setLayoutData(gridData);
		storageNameField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		storageNameField.setLayoutData(gridData);

		Label storageKeyLabel = new Label(container, SWT.LEFT);
		storageKeyLabel.setText("Storage Key:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		storageKeyLabel.setLayoutData(gridData);
		storageKeyField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		storageKeyField.setLayoutData(gridData);

		Label userNameLabel = new Label(container, SWT.LEFT);
		userNameLabel.setText("User Name:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		userNameLabel.setLayoutData(gridData);
		userNameField = new Text(container, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		userNameField.setLayoutData(gridData);

		Label passwordLabel = new Label(container, SWT.LEFT);
		passwordLabel.setText("Password:");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		passwordLabel.setLayoutData(gridData);
		passwordField = new Text(container, SWT.PASSWORD | SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		passwordField.setLayoutData(gridData);

		return super.createDialogArea(parent);
	}

	@Override
	protected void okPressed() {
		synchronized (AddNewClusterForm.class) {
			isCarryOnNextStep = true;
			setErrorMessage(null);

			AppInsightsClient.create(Messages.HDInsightAddNewClusterAction, null);

			String clusterNameOrUrl = clusterNameField.getText().trim();
			userName = userNameField.getText().trim();
			storageName = storageNameField.getText().trim();

			storageKey = storageKeyField.getText().trim();

			password = passwordField.getText();

			if (StringHelper.isNullOrWhiteSpace(clusterNameOrUrl) || StringHelper.isNullOrWhiteSpace(storageName)
					|| StringHelper.isNullOrWhiteSpace(storageKey) || StringHelper.isNullOrWhiteSpace(userName)
					|| StringHelper.isNullOrWhiteSpace(password)) {
				setErrorMessage("Cluster Name, Storage Key, User Name, or Password shouldn't be empty");
				isCarryOnNextStep = false;
			} else {
				clusterName = getClusterName(clusterNameOrUrl);

				if (clusterName == null) {
					setErrorMessage("Wrong cluster name or endpoint");
					isCarryOnNextStep = false;
				} else {
					int status = ClusterManagerEx.getInstance().isHDInsightAdditionalStorageExist(clusterName,
							storageName);
					if (status == 1) {
						setErrorMessage("Cluster already exist in current list");
						isCarryOnNextStep = false;
					} else if (status == 2) {
						setErrorMessage("Default storage account is required");
						isCarryOnNextStep = false;
					}
				}
			}

			if (isCarryOnNextStep) {
				getStorageAccount();
			}

			if (isCarryOnNextStep) {
				if (storageAccount != null) {
					HDInsightAdditionalClusterDetail hdInsightAdditionalClusterDetail = new HDInsightAdditionalClusterDetail(
							clusterName, userName, password, storageAccount);
					ClusterManagerEx.getInstance().addHDInsightAdditionalCluster(hdInsightAdditionalClusterDetail);
					hdInsightModule.refreshWithoutAsync();
				}
				super.okPressed();
			}
		}
	}

	// format input string
	private static String getClusterName(String userNameOrUrl) {
		if (userNameOrUrl.startsWith(URL_PREFIX)) {
			return StringHelper.getClusterNameFromEndPoint(userNameOrUrl);
		} else {
			return userNameOrUrl;
		}
	}

	private void getStorageAccount() {
		PluginUtil.showBusy(true, getShell());
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					storageAccount = AddHDInsightAdditionalClusterImpl.getStorageAccount(clusterName, storageName,
							storageKey, userName, password);
					isCarryOnNextStep = true;
				} catch (AzureCmdException | HDIException e) {
					isCarryOnNextStep = false;
					setErrorMessage(e.getMessage());
				}
			}
		});
		PluginUtil.showBusy(false, getShell());
	}
}
