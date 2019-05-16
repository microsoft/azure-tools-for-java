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
package com.microsoft.azuretools.hdinsight.spark.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;

import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.hdinsight.Activator;
import com.microsoft.azuretools.hdinsight.projects.HDInsightProjectNature;
import com.microsoft.azuretools.hdinsight.spark.common2.SparkSubmitModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SparkSubmissionExDialog extends Dialog {

	private final int margin = 10;
	private static final String DIALOG_TITLE = "Spark Submission";
	private static final String[] COLUMN_NAMES = { "Key", "Value" };
	private static final String JAVA_NATURE_ID = "org.eclipse.jdt.core.javanature";
	
	private int row = 0;
	private Combo clustersListComboBox;
	private Combo selectedArtifactComboBox;
	private Text selectedArtifactTextField;
	private Button artifactBrowseButton;
	private Table jobConfigurationTable;
	private TableViewer tableViewer;
	private Combo mainClassCombo;
	private Text commandLineTextField;
	private Text referencedJarsTextField;
	private Text referencedFilesTextField;
	private Button intelliJArtifactRadioButton;
	private Button localArtifactRadioButton;

	private SparkSubmitModel submitModel;
	private IProject myProject;
	private CallBack callBack;
	private List<IClusterDetail> cachedClusterDetails;
	private Map<String, Object> jobConfigMap = new HashMap<String, Object>();

	public SparkSubmissionExDialog(Shell parentShell, @NotNull List<IClusterDetail> cachedClusterDetails,
			@Nullable IProject project, @Nullable CallBack callBack) {
		super(parentShell);
		this.cachedClusterDetails = cachedClusterDetails;
		this.myProject = project;
		this.callBack = callBack;
		submitModel = new SparkSubmitModel(cachedClusterDetails);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DIALOG_TITLE);
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(Dialog.OK).setText("Submit");
		return buttonBar;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		container.setLayout(gridLayout);
		GridData gridData = new GridData();
		gridData.widthHint = 550;
		container.setLayoutData(gridData);

		Label clusterListLabel = new Label(container, SWT.LEFT);
		clusterListLabel.setText("Cluster Name:");
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		clustersListComboBox = new Combo(composite, SWT.READ_ONLY);
		clustersListComboBox.setLayoutData(gridData);
		clustersListComboBox.setToolTipText(
				"The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
		for (IClusterDetail clusterDetail : cachedClusterDetails) {
            clustersListComboBox.add(clusterDetail.getTitle());
            clustersListComboBox.setData(clusterDetail.getTitle(), clusterDetail);
		}
		if (cachedClusterDetails.size() > 0) {
			clustersListComboBox.select(0);
		}
		Button clusterListButton = new Button(composite, SWT.PUSH);
		clusterListButton.setToolTipText("Refresh");
		clusterListButton.setImage(Activator.getImageDescriptor(CommonConst.RefreshIConPath).createImage());

		String tipInfo = "The Artifact you want to use.";
		Label artifactSelectLabel = new Label(container, SWT.LEFT);
		artifactSelectLabel.setText("Select an Artifact to submit");
		artifactSelectLabel.setToolTipText(tipInfo);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		artifactSelectLabel.setLayoutData(gridData);

		intelliJArtifactRadioButton = new Button(container, SWT.RADIO);
		intelliJArtifactRadioButton.setText("Artifact from Eclipse project:");
		intelliJArtifactRadioButton.setSelection(true);
		intelliJArtifactRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				selectedArtifactComboBox.setEnabled(true);
				selectedArtifactTextField.setEnabled(false);
				// mainClassTextField.setButtonEnabled(true);
				//
				// setVisibleForFixedErrorMessageLabel(2, false);
				//
				// if (selectedArtifactComboBox.getItemCount() == 0) {
				// setVisibleForFixedErrorMessageLabel(2, true);
				// }
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
		selectedArtifactComboBox = new Combo(container, SWT.READ_ONLY);
		selectedArtifactComboBox.setToolTipText(tipInfo);
		selectedArtifactComboBox.setLayoutData(gridData);
		String[] projects = getProjects();
		selectedArtifactComboBox.setItems(projects);
		if (myProject != null) {
			final String selectedProjectName = myProject.getName();
			for (int i = 0; i < projects.length; ++i) {
				if (projects[i].equalsIgnoreCase(selectedProjectName)) {
					selectedArtifactComboBox.select(i);
				}
			}
		} else if (projects.length > 0) {
			selectedArtifactComboBox.select(0);
		}

		localArtifactRadioButton = new Button(container, SWT.RADIO);
		localArtifactRadioButton.setText("Artifact from hard disk:");

		localArtifactRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				selectedArtifactComboBox.setEnabled(false);
				selectedArtifactTextField.setEnabled(true);
				// mainClassTextField.setButtonEnabled(false);
				//
				// setVisibleForFixedErrorMessageLabel(1, false);
				//
				// if
				// (StringHelper.isNullOrWhiteSpace(selectedArtifactTextField.getText()))
				// {
				// setVisibleForFixedErrorMessageLabel(2, true);
				// }
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}
		});

		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		composite = new Composite(container, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		selectedArtifactTextField = new Text(composite, SWT.LEFT | SWT.BORDER);
		selectedArtifactTextField.setLayoutData(gridData);
		artifactBrowseButton = new Button(composite, SWT.PUSH);
		artifactBrowseButton.setText("Browse");
		artifactBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				FileDialog dialog = new FileDialog(SparkSubmissionExDialog.this.getShell());
				String[] extensions = { "*.jar", "*.JAR" };
				dialog.setFilterExtensions(extensions);
				String file = dialog.open();
				if (file != null) {
					selectedArtifactTextField.setText(file);
				}
			}
		});

		Label sparkMainClassLabel = new Label(container, SWT.LEFT);
		sparkMainClassLabel.setText("Main class name");
		sparkMainClassLabel.setToolTipText("Application's java/spark main class");

		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		composite = new Composite(container, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;

		mainClassCombo = new Combo(composite, SWT.DROP_DOWN);
		mainClassCombo.setLayoutData(gridData);
		try {
			java.util.Set<String> classes = getClassesWithMainMethod();
		    String [] names = new String[classes.size()];
		    classes.toArray(names);
		    mainClassCombo.setItems(names);
		    mainClassCombo.select(0);
		} catch (CoreException e1) {
			Activator.getDefault().log("get main class list error", e1);
		}
	    
		Label jobConfigurationLabel = new Label(container, SWT.LEFT);
		jobConfigurationLabel.setText("Job configurations");
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		jobConfigurationLabel.setLayoutData(gridData);

		jobConfigurationTable = new Table(container, SWT.BORDER);
		jobConfigurationTable.setHeaderVisible(true);
		jobConfigurationTable.setLinesVisible(true);

		gridData = new GridData();
		gridData.heightHint = 75;
		gridData.horizontalAlignment = SWT.FILL;

		GridLayout gridLayoutTable = new GridLayout();
		gridLayoutTable.numColumns = 2;
		gridLayoutTable.marginRight = 0;
		jobConfigurationTable.setLayout(gridLayoutTable);
		jobConfigurationTable.setLayoutData(gridData);

		TableColumn key = new TableColumn(jobConfigurationTable, SWT.FILL);
		key.setText(COLUMN_NAMES[0]);
		key.setWidth(150);

		TableColumn value = new TableColumn(jobConfigurationTable, SWT.FILL);
		value.setText(COLUMN_NAMES[1]);
		value.setWidth(80);

		tableViewer = new TableViewer(jobConfigurationTable);
		tableViewer.setUseHashlookup(true);
		tableViewer.setColumnProperties(COLUMN_NAMES);

		CellEditor[] editors = new CellEditor[2];

		editors[1] = new TextCellEditor(jobConfigurationTable);

		tableViewer.setCellEditors(editors);
		tableViewer.setContentProvider(new JobConfigurationContentProvider());
		tableViewer.setLabelProvider(new JobConfigurationLabelProvider());
		tableViewer.setCellModifier(new JobConfigurationCellModifier());
		initializeTable();

		Label label = new Label(container, SWT.LEFT);
		label.setText("Command line arguments");
		tipInfo = "Command line arguments used in your main class; multiple arguments should be split by space.";
		label.setToolTipText(tipInfo);
		commandLineTextField = new Text(container, SWT.LEFT | SWT.BORDER);
		commandLineTextField.setToolTipText(tipInfo);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		commandLineTextField.setLayoutData(gridData);

		tipInfo = "Files to be placed on the java classpath; The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)";
		Label referencedJarsLabel = new Label(container, SWT.LEFT);
		referencedJarsLabel.setText("Referenced Jars");
		referencedJarsLabel.setToolTipText(tipInfo);
		referencedJarsTextField = new Text(container, SWT.BORDER | SWT.LEFT);
		referencedJarsTextField.setToolTipText(tipInfo);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		referencedJarsTextField.setLayoutData(gridData);

		tipInfo = "Files to be placed in executor working directory. The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ";
		Label referencedFilesLabel = new Label(container, SWT.LEFT);
		referencedFilesLabel.setText("Referenced Files");
		referencedFilesLabel.setToolTipText(tipInfo);
		referencedFilesTextField = new Text(container, SWT.BORDER | SWT.LEFT);
		referencedFilesTextField.setToolTipText(tipInfo);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		referencedFilesTextField.setLayoutData(gridData);

		return super.createContents(parent);
	}

	private class JobConfigurationContentProvider implements IStructuredContentProvider {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object arg0) {
			return jobConfigMap.entrySet().toArray();
		}
	}

	private class JobConfigurationLabelProvider implements ITableLabelProvider {

		@Override
		public void removeListener(ILabelProviderListener arg0) {
		}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void addListener(ILabelProviderListener arg0) {
		}

		@Override
		public String getColumnText(Object element, int colIndex) {
			Map.Entry<String, Object> keyValue = (Map.Entry<String, Object>) element;
			switch (colIndex) {
			case 0:
				return keyValue.getKey();
			case 1:
				return "" + keyValue.getValue();
			default:
				return "";
			}
		}

		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}
	}

	private class JobConfigurationCellModifier implements ICellModifier {
		@Override
		public void modify(Object waEndpoint, String columnName, Object modifiedVal) {
			TableItem tblItem = (TableItem) waEndpoint;
			Map.Entry<String, Object> keyValue = (Map.Entry<String, Object>) tblItem.getData();
			if (columnName.equals(COLUMN_NAMES[1])) {
				String value = modifiedVal.toString();
				keyValue.setValue(value);
			}
			tableViewer.refresh();
		}

		public Object getValue(Object element, String property) {
			Map.Entry<String, Object> keyValue = (Map.Entry<String, Object>) element;

			if (property.equals(COLUMN_NAMES[0])) {
				return keyValue.getKey();
			} else if (property.equals(COLUMN_NAMES[1])) {
				return keyValue.getValue();
			}
			return "";
		}

		/**
		 * Determines whether a particular cell can be modified or not.
		 * 
		 * @return boolean
		 */
		@Override
		public boolean canModify(Object element, String property) {
			return property.equals(COLUMN_NAMES[1]);
		}
	}

	private void initializeTable() {
		for (int i = 0; i < SparkSubmissionParameter.defaultParameters.length; ++i) {
			jobConfigMap.put(SparkSubmissionParameter.defaultParameters[i].first(), "");
		}
		tableViewer.setInput(jobConfigMap.entrySet());
	}

	private SparkSubmissionParameter constructSubmissionParameter() {
		IClusterDetail selectedClusterDetail = (IClusterDetail) clustersListComboBox
				.getData(clustersListComboBox.getText());

		String selectedArtifactName = selectedArtifactComboBox.getText();
		String className = mainClassCombo.getText().trim();
		String commandLine = commandLineTextField.getText().trim();
		String localArtifactPath = selectedArtifactTextField.getText();
		String selectedClusterName = selectedClusterDetail.getName();

		List<String> referencedFileList = new ArrayList<>();
		for (String singleReferencedFile : referencedFilesTextField.getText().split(";")) {
			singleReferencedFile = singleReferencedFile.trim();
			if (!StringHelper.isNullOrWhiteSpace(singleReferencedFile)) {
				referencedFileList.add(singleReferencedFile);
			}
		}

		List<String> uploadedFilePathList = new ArrayList<>();
		for (String singleReferencedJars : referencedJarsTextField.getText().split(";")) {
			singleReferencedJars = singleReferencedJars.trim();
			if (!StringHelper.isNullOrWhiteSpace(singleReferencedJars)) {
				uploadedFilePathList.add(singleReferencedJars);
			}
		}

		List<String> argsList = new ArrayList<>();
		for (String singleArs : commandLine.split(" ")) {
			if (!StringHelper.isNullOrWhiteSpace(singleArs)) {
				argsList.add(singleArs.trim());
			}
		}

		return new SparkSubmissionParameter(selectedClusterName, localArtifactRadioButton.getSelection(),
				selectedArtifactName, localArtifactPath, null, className, referencedFileList, uploadedFilePathList,
				argsList, jobConfigMap);
	}

	private String[] getProjects() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		String[] projects = null;
		ArrayList<String> projList = new ArrayList<String>();
		try {
			for (IProject wRoot : root.getProjects()) {
				if (wRoot.hasNature(HDInsightProjectNature.NATURE_ID)) {
					projList.add(wRoot.getProject().getName());
				}
			}
			projects = new String[projList.size()];
			projects = projList.toArray(projects);
		} catch (Exception e) {
			PluginUtil.displayErrorDialogAndLog(this.getShell(), "Project Selection Error",
					"Error occurred while selecting the project.", e);
		}
		return projects;
	}

	private java.util.Set<String> getClassesWithMainMethod() throws CoreException {
		java.util.Set<String> filterClassSet = new HashSet<String>();
		if (myProject != null && myProject.isNatureEnabled(JAVA_NATURE_ID)) {
			IJavaProject javaProject = JavaCore.create(myProject);
			
			if (javaProject == null) {
				return filterClassSet;
			}

			IPackageFragment[] packages = javaProject.getPackageFragments();
			if (packages == null || packages.length == 0) {
				return filterClassSet;
			}

			for (IPackageFragment mypackage : packages) {

				// get source type
				if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
					for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
						IType[] allTypes = unit.getAllTypes();
						final String unitName = unit.getElementName();
						for (IType type : allTypes) {
							IMethod[] methods = type.getMethods();
							for (IMethod method : methods) {
								if (method.getElementName().equalsIgnoreCase("main")) {

									String simpleClassName = type.getElementName();
									if (simpleClassName == null || simpleClassName.isEmpty()) {
										// remove .class suffix
										simpleClassName = unitName.substring(0, unitName.length() - 6);
									}
									
									// Handle duplicated Scala class
									if (simpleClassName.endsWith("$") && simpleClassName.length() > 1) {
										simpleClassName = simpleClassName.substring(0, simpleClassName.length() - 1);
									}
									
									if (mypackage.isDefaultPackage()) {
										filterClassSet.add(simpleClassName);
									} else {
										final String className = String.format("%s.%s", mypackage.getElementName(),
												simpleClassName);
										filterClassSet.add(className);
									}
								}
							}
						}
					}
				}
			}
		}
		return filterClassSet;
	}

	@Override
	protected void okPressed() {
		AppInsightsClient.create(Messages.SparkSubmissionButtonClickEvent, null);
		EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionButtonClickEvent, null);
		submitModel.action(constructSubmissionParameter());
		super.okPressed();
	}

}
