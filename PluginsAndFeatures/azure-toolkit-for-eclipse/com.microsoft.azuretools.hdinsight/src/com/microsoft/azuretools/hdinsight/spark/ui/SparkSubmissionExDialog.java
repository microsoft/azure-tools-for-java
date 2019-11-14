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
import static org.eclipse.swt.SWT.CENTER;
import static org.eclipse.swt.SWT.FILL;
import static org.eclipse.swt.SWT.TOP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.azureexplorer.forms.AddNewHDInsightReaderClusterForm;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.hdinsight.Activator;
import com.microsoft.azuretools.hdinsight.projects.HDInsightProjectNature;
import com.microsoft.azuretools.hdinsight.spark.common2.SparkSubmitModel;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.Pair;

public class SparkSubmissionExDialog extends Dialog {
	static class GridDataBuilder {
		// Default Grid Data settings
		private GridData gridData = new GridData(FILL, CENTER, true, false, 1, 1);

		public GridDataBuilder widthHint(final int hint) {
			gridData.widthHint = hint;
			return this;
		}

		public GridDataBuilder heightHint(final int hint) {
			gridData.heightHint = hint;
			return this;
		}

		public GridDataBuilder horizontalIndent(final int indent) {
			gridData.horizontalIndent = indent;
			return this;
		}

		public GridDataBuilder verticalAlignment(final int align) {
			gridData.verticalAlignment = align;
			return this;
		}

		public GridDataBuilder span(final int horizontalSpan, final int verticalSpan) {
			gridData.horizontalSpan = horizontalSpan;
			gridData.verticalSpan = verticalSpan;
			return this;
		}

		public GridData build() {
			return gridData;
		}
	}

	private final int margin = 10;
	private static final String DIALOG_TITLE = "Spark Submission";
	private static final String[] COLUMN_NAMES = { "Key", "Value" };
	private static final String JAVA_NATURE_ID = "org.eclipse.jdt.core.javanature";
	
	private Combo clustersListComboBox;
	private Label hdiReaderErrorMsgLabel;
	private Link hdiReaderLink;

	private Button projectArtifactRadioButton;
	private Button localArtifactRadioButton;
	private Combo projectArtifactSelectComboBox;
	private Text localArtifactInput;
	private Button localArtifactBrowseButton;

	private TableViewer jobConfigTableViewer;
	private Combo mainClassCombo;
	private Text commandLineTextField;
	private Text referencedJarsTextField;
	private Text referencedFilesTextField;

	private SparkSubmitModel submitModel;
	private IProject myProject;
	private CallBack callBack;
	private List<IClusterDetail> cachedClusterDetails;
	private ImmutableList<Pair<String, String>> jobConfigs = ImmutableList.of();

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
		final Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(2, false));
		root.setLayoutData(new GridDataBuilder().widthHint(600).build());

		final Label clusterListLabel = new Label(root, SWT.LEFT);
		clusterListLabel.setText("Cluster Name:");

		clustersListComboBox = new Combo(root, SWT.READ_ONLY);
		clustersListComboBox.setLayoutData(new GridDataBuilder().build());
		clustersListComboBox.setToolTipText(
				"The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
		clustersListComboBox.addModifyListener(event -> {
			IClusterDetail clusterDetail = getSelectedCluster(clustersListComboBox.getText());
			if (clusterDetail != null && ClusterManagerEx.getInstance().isHdiReaderCluster(clusterDetail)) {
				showHdiReaderErrors(true);
			} else {
				showHdiReaderErrors(false);
			}
		});

		// Execute "select the first item" operation after the dialog opened
		Display.getDefault().asyncExec(() -> {
			for (IClusterDetail clusterDetail : cachedClusterDetails) {
	            clustersListComboBox.add(clusterDetail.getTitle());
	            clustersListComboBox.setData(clusterDetail.getTitle(), clusterDetail);
			}
			if (cachedClusterDetails.size() > 0) {
				// Send SWT.Modify event after select the first item
				clustersListComboBox.select(0);
			}
		});

		//Add blank label as a placeholder
		final Label placeholderLabel = new Label(root, SWT.LEFT);
		placeholderLabel.setVisible(false);
		final Composite warningWithLink = new Composite(root, SWT.NONE);
		warningWithLink.setLayout(new GridLayout(2, false));
		warningWithLink.setLayoutData(new GridDataBuilder().build());
		
		// Add warning message and link cluster button composite
		hdiReaderErrorMsgLabel = new Label(warningWithLink, SWT.LEFT);
		hdiReaderErrorMsgLabel.setText(
				"No Ambari permission to submit job to the selected cluster...");
		hdiReaderErrorMsgLabel.setToolTipText(
				"No Ambari permission to submit job to the selected cluster. Please ask the cluster owner or user "
				+ "access administrator to upgrade your role to HDInsight Cluster Operator in the Azure Portal, or "
				+ "link to the selected cluster.");
		hdiReaderLink = new Link(warningWithLink, SWT.NONE);
		hdiReaderLink.setText("<a href=\" \">Link this cluster</a>");
		hdiReaderLink.addListener(SWT.Selection, e -> {
			IClusterDetail selectedClusterDetail = getSelectedCluster(clustersListComboBox.getText());
			if (selectedClusterDetail != null && ClusterManagerEx.getInstance().isHdiReaderCluster(selectedClusterDetail)) {
				String defaultStorageRootPath = ((ClusterDetail) selectedClusterDetail).getDefaultStorageRootPath();
				
				AddNewHDInsightReaderClusterForm linkClusterForm = new AddNewHDInsightReaderClusterForm(
						PluginUtil.getParentShell(), null, (ClusterDetail) selectedClusterDetail) {
					protected void afterOkActionPerformed() {
						showHdiReaderErrors(false);
						HDInsightAdditionalClusterDetail linkedCluster = 
								(HDInsightAdditionalClusterDetail) ClusterManagerEx.getInstance().findClusterDetail(clusterDetail ->
										getSelectedLinkedHdiCluster(clusterDetail, selectedClusterDetail.getName()), true);
						if (linkedCluster != null) {
							linkedCluster.setDefaultStorageRootPath(defaultStorageRootPath);
							ClusterManagerEx.getInstance().updateHdiAdditionalClusterDetail(linkedCluster);
							
							// Display the HDI reader cluster as linked cluster
							Display.getDefault().asyncExec(() -> {
								int selectedClusterIndex = clustersListComboBox.indexOf(selectedClusterDetail.getTitle());
								clustersListComboBox.setItem(selectedClusterIndex, linkedCluster.getTitle());
								clustersListComboBox.setData(linkedCluster.getTitle(), linkedCluster);
							});
						}							
					}
				};
				linkClusterForm.open();
			}
		});

		// Hide HDInsight reader cluster error when dialog open at first time
		showHdiReaderErrors(false);
		
		// Radio button group for the artifact selection
		final Group artifactSelectGroup = new Group(root, SWT.NONE);
		artifactSelectGroup.setText("Select an Artifact to submit");
		artifactSelectGroup.setLayout(new GridLayout(2, false));
		artifactSelectGroup.setLayoutData(new GridDataBuilder().span(2, 2).build());

		// Project artifacts Radio button
		projectArtifactRadioButton = new Button(artifactSelectGroup, SWT.RADIO);
		projectArtifactRadioButton.setText("Artifact from Eclipse project:");
		projectArtifactRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ignored) {
				switchToProjectArtifactSelection(true);
			}
		});

		final String selectProjectArtifactTip = "The Artifact you want to use.";
		projectArtifactSelectComboBox = new Combo(artifactSelectGroup, SWT.READ_ONLY);
		projectArtifactSelectComboBox.setToolTipText(selectProjectArtifactTip);
		projectArtifactSelectComboBox.setLayoutData(new GridDataBuilder().widthHint(400).build());
		projectArtifactSelectComboBox.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent event) {
				event.result = projectArtifactRadioButton.getText();
			}
		});
		final String[] projects = getProjects();
		projectArtifactSelectComboBox.setItems(projects);
		if (myProject != null) {
			final String selectedProjectName = myProject.getName();
			for (int i = 0; i < projects.length; ++i) {
				if (projects[i].equalsIgnoreCase(selectedProjectName)) {
					projectArtifactSelectComboBox.select(i);
				}
			}
		} else if (projects.length > 0) {
			projectArtifactSelectComboBox.select(0);
		}

		// Local artifact
		localArtifactRadioButton = new Button(artifactSelectGroup, SWT.RADIO);
		localArtifactRadioButton.setText("Artifact from hard disk:");
		localArtifactRadioButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ignored) {
				switchToProjectArtifactSelection(false);
			}
		});

		final Composite localArtifactSelection = new Composite(artifactSelectGroup, SWT.NONE);
		localArtifactSelection.setLayout(new GridLayout(2, false));
		localArtifactSelection.setLayoutData(new GridDataBuilder().build());

		final String localArtifactInputTip = "Input the local artifact path.";
		localArtifactInput = new Text(localArtifactSelection, SWT.LEFT | SWT.BORDER);
		localArtifactInput.setToolTipText(localArtifactInputTip);
		localArtifactInput.setLayoutData(new GridDataBuilder().build());
		localArtifactInput.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent event) {
				event.result = localArtifactRadioButton.getText();
			}
		});

		// Browser button to open file selection dialog
		localArtifactBrowseButton = new Button(localArtifactSelection, SWT.PUSH);
		localArtifactBrowseButton.setText("Browse");
		localArtifactBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				FileDialog dialog = new FileDialog(SparkSubmissionExDialog.this.getShell());
				String[] extensions = { "*.jar", "*.JAR" };
				dialog.setFilterExtensions(extensions);
				String file = dialog.open();
				if (file != null) {
					localArtifactInput.setText(file);
				}
			}
		});

		localArtifactBrowseButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent event) {
				event.result = localArtifactRadioButton.getText() + " open System File dialog to select artifact";
			}
		});

		// Default selection
		projectArtifactRadioButton.setSelection(true);
		switchToProjectArtifactSelection(true);

		// Main class input
		final Label sparkMainClassLabel = new Label(root, SWT.LEFT);
		sparkMainClassLabel.setText("Main class name");

		mainClassCombo = new Combo(root, SWT.DROP_DOWN);
		mainClassCombo.setToolTipText("Application's java/spark main class");
		mainClassCombo.setLayoutData(new GridDataBuilder().build());
		try {
			java.util.Set<String> classes = getClassesWithMainMethod();
		    String [] names = new String[classes.size()];
		    classes.toArray(names);
		    mainClassCombo.setItems(names);
		    mainClassCombo.select(0);
		} catch (CoreException e1) {
			Activator.getDefault().log("get main class list error", e1);
		}
	    
		// Job configuration
		final Label jobConfigurationLabel = new Label(root, SWT.LEFT);
		jobConfigurationLabel.setText("Job configurations");
		jobConfigurationLabel.setLayoutData(new GridDataBuilder().verticalAlignment(TOP).build());

		jobConfigTableViewer = new TableViewer(root, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		jobConfigTableViewer.setUseHashlookup(true);
		jobConfigTableViewer.setColumnProperties(COLUMN_NAMES);

		final Table jobConfigurationTable = jobConfigTableViewer.getTable();
		jobConfigurationTable.setHeaderVisible(true);
		jobConfigurationTable.setLinesVisible(true);

		jobConfigurationTable.setLayout(new GridLayout(1, false));
		jobConfigurationTable.setLayoutData(new GridDataBuilder().heightHint(75).build());

		final TableViewerColumn keyCol = new TableViewerColumn(jobConfigTableViewer, SWT.NONE);
		keyCol.getColumn().setText(COLUMN_NAMES[0]);
		keyCol.getColumn().setWidth(150);
		keyCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return asConfigRow(element).first();
			}
		});

		final TableViewerColumn valueCol = new TableViewerColumn(jobConfigTableViewer, SWT.NONE);
		valueCol.getColumn().setText(COLUMN_NAMES[1]);
		valueCol.getColumn().setWidth(80);
		valueCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return asConfigRow(element).second();
			}
		});

		final CellEditor[] editors = new CellEditor[] {
			new TextCellEditor(jobConfigurationTable),
			new TextCellEditor(jobConfigurationTable)
		};

		jobConfigTableViewer.setCellEditors(editors);
		jobConfigTableViewer.setContentProvider(new JobConfigurationContentProvider());
		jobConfigTableViewer.setLabelProvider(new JobConfigurationLabelProvider());
		jobConfigTableViewer.setCellModifier(new JobConfigurationCellModifier());
		
		// Enable navigate the table cells with arrow keys
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(jobConfigTableViewer,new FocusCellOwnerDrawHighlighter(jobConfigTableViewer));
		ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(jobConfigTableViewer) {
		    protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
		        // Enable editor only with mouse double click or space key press
		        if (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
		        		|| event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.character == SWT.SPACE) {
		            EventObject source = event.sourceEvent;
		            // Take double right click as invalid mouse event
		            if (source instanceof MouseEvent && ((MouseEvent)source).button == 3)
		                return false;

		            return true;
		        }

		        return false;
		    }
		};

		TableViewerEditor.create(jobConfigTableViewer, focusCellManager, activationSupport, ColumnViewerEditor.TABBING_HORIZONTAL | 
		    ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | 
		    ColumnViewerEditor.TABBING_VERTICAL |
		    ColumnViewerEditor.KEYBOARD_ACTIVATION);

		initializeTable();
		// Make the table focus at the first cell when it get focus at the first time
		jobConfigTableViewer.editElement(jobConfigurationTable.getItem(0).getData(), 0);

		final Label label = new Label(root, SWT.LEFT);
		label.setText("Command line arguments");
		final String commandLineTip = "Command line arguments used in your main class; multiple arguments should be split by space.";
		commandLineTextField = new Text(root, SWT.LEFT | SWT.BORDER);
		commandLineTextField.setToolTipText(commandLineTip);
		commandLineTextField.setLayoutData(new GridDataBuilder().build());

		final String refJarsTip = "Files to be placed on the java classpath; The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)";
		final Label referencedJarsLabel = new Label(root, SWT.LEFT);
		referencedJarsLabel.setText("Referenced Jars");
		referencedJarsTextField = new Text(root, SWT.BORDER | SWT.LEFT);
		referencedJarsTextField.setToolTipText(refJarsTip);
		referencedJarsTextField.setLayoutData(new GridDataBuilder().build());

		final Label referencedFilesLabel = new Label(root, SWT.LEFT);
		referencedFilesLabel.setText("Referenced Files");
		final String refFilesTip = "Files to be placed in executor working directory. The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ";
		referencedFilesTextField = new Text(root, SWT.BORDER | SWT.LEFT);
		referencedFilesTextField.setToolTipText(refFilesTip);
		referencedFilesTextField.setLayoutData(new GridDataBuilder().build());

		return super.createContents(parent);
	}
	
	private void switchToProjectArtifactSelection(final boolean isProjectArtifact) {
		projectArtifactSelectComboBox.setEnabled(isProjectArtifact);
		localArtifactInput.setEnabled(!isProjectArtifact);
		localArtifactBrowseButton.setEnabled(!isProjectArtifact);
	}

	private void showHdiReaderErrors(boolean isVisible) {
		hdiReaderErrorMsgLabel.setVisible(isVisible);
		hdiReaderLink.setVisible(isVisible);
	}

	private class JobConfigurationContentProvider implements IStructuredContentProvider {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return jobConfigs.toArray();
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
			if (element == null) {
				return null;
			}

			final Pair<String, String> row = asConfigRow(element);

			switch (colIndex) {
			case 0:
				return row.first();
			case 1:
				return row.second();
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
		public void modify(Object element, String property, Object value) {
			final int columnIndex = getPropertyColumnIndex(property);
			
			if (!(element instanceof TableItem)) {
				return;
			}
			
			final TableItem item = (TableItem) element;
			final Pair<String, String> row = asConfigRow(item.getData());
			
			switch (columnIndex) {
			case 0: // Key
				if (StringUtils.isNotBlank(String.valueOf(value))) {
					// Add or update
					final String keyToChange = String.valueOf(value).trim();

					if (containsConfigKey(keyToChange)) {
						// Update
						jobConfigs = ImmutableList.copyOf(jobConfigs.stream()
								.map(conf -> conf.first().equals(keyToChange)
										? new Pair<>(keyToChange, conf.second())
										: conf)
								.iterator());
					} else {
						// Add
						jobConfigs = ImmutableList.<Pair<String, String>>builder()
								.addAll(jobConfigs)
								.add(new Pair<>(keyToChange, ""))
								.build();
					}
				} else {
					// Delete
					jobConfigs = ImmutableList.copyOf(jobConfigs.stream()
							.filter(conf -> !conf.first().equals(row.first()))
							.iterator());
				}
				
				// TODO: item.setText() for Key changes
				break;
			case 1: // Value
				// Update only
				final Pair<String, String> toUpdate = new Pair<>(row.first(), String.valueOf(value));
				jobConfigs = ImmutableList.copyOf(jobConfigs.stream()
						.map(conf -> conf.first().equals(row.first()) ? toUpdate : conf)
						.iterator());
				
				// Change the value displayed on the cell
				item.setText(1, String.valueOf(value));
				
				break;
			default:
			}
		}

		public Object getValue(Object element, String property) {
			final int columnIndex = getPropertyColumnIndex(property);
			final Pair<String, String> row = asConfigRow(element);

			switch (columnIndex) {
			case 0: // Key
				// TODO: find key from immutable jobConfigs list when Key is changed
				return row.first();
			case 1: // Value
				return jobConfigs.stream()
						.filter(conf -> conf.first().equals(row.first()))
						.findFirst()
						.map(Pair::second)
						.orElse("");
			default:
				return "UNKNOWN_PROPERTY: " + property;
			}
		}

		/**
		 * Determines whether a particular cell can be modified or not.
		 * 
		 * @return boolean
		 */
		@Override
		public boolean canModify(Object element, String property) {
			final int columnIndex = getPropertyColumnIndex(property);
			final Pair<String, String> row = asConfigRow(element);

			switch (columnIndex) {
			case 0: // Key
				return !SparkSubmissionParameter.isSubmissionParameter(row.first());
			default:
				return true;
			}
		}
	}

	private int getPropertyColumnIndex(final String property) {
		return Arrays.asList(jobConfigTableViewer.getColumnProperties()).indexOf(property);
	}

	private Pair<String, String> asConfigRow(final Object element) {
		@SuppressWarnings("unchecked")
		final Pair<String, String> row = (Pair<String, String>) element;

		return row;
	}

	private boolean containsConfigKey(final String keyToFind) {
		return jobConfigs.stream().anyMatch(conf -> conf.first().equals(keyToFind));
	}

	private void initializeTable() {
		jobConfigs = ImmutableList.copyOf(Stream.of(SparkSubmissionParameter.defaultParameters)
				.map(defaultParam -> new Pair<>(defaultParam.first(), String.valueOf(defaultParam.second())))
				.iterator());
		jobConfigTableViewer.setInput(jobConfigs);
	}

	@Nullable
	private IClusterDetail getSelectedCluster(@NotNull String title) {
		return (IClusterDetail) clustersListComboBox.getData(title);
	}
	
	private SparkSubmissionParameter constructSubmissionParameter() {
		IClusterDetail selectedClusterDetail = getSelectedCluster(clustersListComboBox.getText());

		String selectedArtifactName = projectArtifactSelectComboBox.getText();
		String className = mainClassCombo.getText().trim();
		String commandLine = commandLineTextField.getText().trim();
		String localArtifactPath = localArtifactInput.getText();
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

		// FIXME: need a duplicated keys check when creating a new row is allowed
		final Map<String, Object> jobConfigMap = jobConfigs.stream()
				.collect(Collectors.toMap(Pair::first, Pair::second));

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
		if (StringUtils.isBlank(clustersListComboBox.getText())) {
			MessageDialog.openError(this.getShell(), "Can't submit the Spark job", "No cluster is selected to submit Spark jobs.");

			return;
		}

		submitModel.action(constructSubmissionParameter());
		super.okPressed();
	}

}
