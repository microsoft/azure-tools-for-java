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

package com.microsoft.azuretools.azureexplorer.editors;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.BLOB_COPYURL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BLOB_DELETE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BLOB_SAVEAS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BLOB_SEARCH;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BLOB_UPLOAD;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.CallableSingleArg;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.azureexplorer.forms.UploadBlobFileForm;
import com.microsoft.azuretools.azureexplorer.helpers.UIHelperImpl;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.BlobDirectory;
import com.microsoft.tooling.msservices.model.storage.BlobFile;
import com.microsoft.tooling.msservices.model.storage.BlobItem;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import org.apache.commons.io.IOUtils;

public class BlobExplorerFileEditor extends EditorPart implements TelemetryProperties {
    private static final String COPY_URL = "Copy URL";
    private static final String SAVE_AS = "Save As";
    private static final String DELETE = "Delete";
    private static final String SEARCH = "Search";
    private static final String REFRESH = "Refresh";
    private static final String UPLOAD_BLOB = "Upload Blob";
    private static final String DELETE_SELECTED_BLOB = "Delete Selected Blob";

    private Text queryTextField;
    private Button queryButton;
    private Button refreshButton;
    private Button uploadButton;
    private Button deleteButton;
//    private Button openButton;
    private Button saveAsButton;
    private Button backButton;
    private Label pathLabel;
    private Table blobListTable;
    private TableViewer tableViewer;

    private String storageAccount;
    private BlobContainer blobContainer;
    private String connectionString;

    private LinkedList<BlobDirectory> directoryQueue = new LinkedList<BlobDirectory>();
    private java.util.List<BlobItem> blobItems = new ArrayList<BlobItem>();
    private FileEditorVirtualNode<EditorPart> fileEditorVirtualNode;

    @Override
    public void doSave(IProgressMonitor iProgressMonitor) {
    }

    @Override
    public void doSaveAs() {
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        storageAccount = ((StorageEditorInput) input).getStorageAccount();
        connectionString = ((StorageEditorInput) input).getConnectionString();
        blobContainer = (BlobContainer) ((StorageEditorInput) input).getItem();
        setPartName(blobContainer.getName() + " [Container]");
        fileEditorVirtualNode = createVirtualNode(blobContainer.getName());
//        fillGrid();
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite composite) {
        composite.setLayout(new GridLayout());
        createToolbar(composite);
        createTable(composite);
        createTablePopup(composite);
    }

    private FileEditorVirtualNode<EditorPart> createVirtualNode(final String name){
        final FileEditorVirtualNode<EditorPart> node = new FileEditorVirtualNode<EditorPart>(this, name);
        node.addAction(COPY_URL, new WrappedTelemetryNodeActionListener(STORAGE, BLOB_COPYURL, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                copyURLSelectedFile();
            }
        }));

        node.addAction(SAVE_AS, new WrappedTelemetryNodeActionListener(STORAGE, BLOB_SAVEAS, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                saveAsSelectedFile();
            }
        }));

        node.addAction(DELETE, new WrappedTelemetryNodeActionListener(STORAGE, BLOB_DELETE, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                deleteSelectedFile();
            }
        }));

        node.addAction(SEARCH, new WrappedTelemetryNodeActionListener(STORAGE, BLOB_SEARCH, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                fillGrid();
            }
        }));

        node.addAction(UPLOAD_BLOB, new WrappedTelemetryNodeActionListener(STORAGE, BLOB_UPLOAD, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                uploadFile();
            }
        }));

        return node;
    }

    private void createTablePopup(Composite composite) {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                if (tableViewer.getSelection().isEmpty() || getFileSelection() == null) {
                    return;
                }
                if (tableViewer.getSelection() instanceof IStructuredSelection) {
                    manager.add(fileEditorVirtualNode.createPopupAction(COPY_URL));
                    manager.add(fileEditorVirtualNode.createPopupAction(SAVE_AS));
                    manager.add(fileEditorVirtualNode.createPopupAction(DELETE));
                }
            }
        });
        Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
        tableViewer.getControl().setMenu(menu);
    }

    private void createToolbar(Composite parent) {
        GridLayout gridLayout = new GridLayout(2, false);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, 0);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        queryTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        queryTextField.setLayoutData(gridData);

        createButtons(container);
        createBackButton(container);
    }

    private void createButtons(Composite parent) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.type = SWT.HORIZONTAL;
        rowLayout.wrap = false;

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(rowLayout);

        queryButton = new Button(container, SWT.PUSH);
        queryButton.setImage(Activator.getImageDescriptor("icons/Start.png").createImage());
        queryButton.setToolTipText(SEARCH);

        refreshButton = new Button(container, SWT.PUSH);
        refreshButton.setImage(Activator.getImageDescriptor("icons/storagerefresh.png").createImage());
        refreshButton.setToolTipText(REFRESH);

        uploadButton = new Button(container, SWT.PUSH);
        uploadButton.setImage(Activator.getImageDescriptor("icons/storageupload.png").createImage());
        uploadButton.setToolTipText(UPLOAD_BLOB);

        deleteButton = new Button(container, SWT.PUSH);
        deleteButton.setImage(Activator.getImageDescriptor("icons/storagedelete.png").createImage());
        deleteButton.setToolTipText(DELETE_SELECTED_BLOB);
        deleteButton.setEnabled(false);

//        openButton = new Button(container, SWT.PUSH);
//        openButton.setImage(Activator.getImageDescriptor("icons/storageopen.png").createImage());
//        openButton.setToolTipText("Open Blob");
//        openButton.setEnabled(false);

        saveAsButton = new Button(container, SWT.PUSH);
        saveAsButton.setImage(Activator.getImageDescriptor("icons/storagesaveas.png").createImage());
        saveAsButton.setToolTipText(SAVE_AS);
        saveAsButton.setEnabled(false);

        SelectionListener queryAction = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fileEditorVirtualNode.doAction(SEARCH);
            }
        };

        refreshButton.addSelectionListener(queryAction);
        queryButton.addSelectionListener(queryAction);

        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fileEditorVirtualNode.doAction(DELETE);
            }
        });

        saveAsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fileEditorVirtualNode.doAction(SAVE_AS);
            }
        });

//        openButton.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                downloadSelectedFile(true);
//            }
//        });

        uploadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fileEditorVirtualNode.doAction(UPLOAD_BLOB);
            }
        });
    }

    private void createBackButton(Composite parent) {
        GridLayout gridLayout = new GridLayout(2, false);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        backButton = new Button(container, SWT.PUSH);
        backButton.setImage(Activator.getImageDescriptor("icons/storageback.png").createImage());
        backButton.setToolTipText("Open Parent Directory");
        backButton.setEnabled(false);

        pathLabel = new Label(container, SWT.LEFT);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        pathLabel.setLayoutData(gridData);

        backButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                directoryQueue.pollLast();
                fillGrid();
            }
        });
    }


    private Table createTable(Composite parent) {
        blobListTable = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);

        blobListTable.setHeaderVisible(true);
        blobListTable.setLinesVisible(true);

        GridData gridData = new GridData();
//        gridData.heightHint = 75;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.grabExcessHorizontalSpace = true;

        GridLayout gridLayoutTable = new GridLayout();
        gridLayoutTable.numColumns = 6;
        gridLayoutTable.marginRight = 0;
        blobListTable.setLayout(gridLayoutTable);
        blobListTable.setLayoutData(gridData);
        for (int i = 0; i < 6; i++) {
            new TableColumn(blobListTable, SWT.FILL);
        }

        blobListTable.getColumn(0).setText("");
        blobListTable.getColumn(1).setText("Name");
        blobListTable.getColumn(2).setText("Size");
        blobListTable.getColumn(3).setText("Last Modified (UTC)");
        blobListTable.getColumn(4).setText("Content Type");
        blobListTable.getColumn(5).setText("URL");

        blobListTable.getColumn(0).setWidth(25);
        blobListTable.getColumn(1).setWidth(280);
        blobListTable.getColumn(2).setWidth(60);
        blobListTable.getColumn(3).setWidth(110);
        blobListTable.getColumn(4).setWidth(140);
        blobListTable.getColumn(5).setWidth(250);

        tableViewer = new TableViewer(blobListTable);
        tableViewer.setUseHashlookup(true);
        tableViewer.setContentProvider(new BlobListContentProvider());
        tableViewer.setLabelProvider(new BlobListLabelProvider());
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
                boolean directorySelected = tableViewer.getTable().getSelectionIndex() > -1 && isDirectorySelected();

                deleteButton.setEnabled(!directorySelected);
//                openButton.setEnabled(!directorySelected);
                saveAsButton.setEnabled(!directorySelected);
            }
        });
        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                tableSelection();
            }
        });

        fillGrid();

        return blobListTable;
    }

    public void fillGrid() {
        setUIState(true);

        DefaultLoader.getIdeHelper().runInBackground(null, "Loading blobs...", false, true, "Loading blobs...", new Runnable() {
            @Override
            public void run() {
                try {
                    if (directoryQueue.peekLast() == null) {
                        directoryQueue.addLast(StorageClientSDKManager.getManager().getRootDirectory(connectionString, blobContainer));
                    }

                    blobItems = StorageClientSDKManager.getManager().getBlobItems(connectionString, directoryQueue.peekLast());

                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (!queryTextField.getText().isEmpty()) {
                                for (int i = blobItems.size() - 1; i >= 0; i--) {
                                    BlobItem blobItem = blobItems.get(i);

                                    if (blobItem instanceof BlobFile && !blobItem.getName().startsWith(queryTextField.getText())) {
                                        blobItems.remove(i);
                                    }
                                }
                            }

                            pathLabel.setText(directoryQueue.peekLast().getPath());
                            tableViewer.setInput(blobItems);
                            tableViewer.refresh();
                            setUIState(false);
//
//                            blobListTable.clearSelection();
                        }
                    });
                } catch (AzureCmdException ex) {
                    DefaultLoader.getUIHelper().showException("Error querying blob list.", ex, "Error querying blobs", false, true);
                }
            }
        });
    }

    private void setUIState(boolean loading) {
        if (loading) {
            blobListTable.setEnabled(false);
            backButton.setEnabled(false);
            queryButton.setEnabled(false);
            refreshButton.setEnabled(false);
            uploadButton.setEnabled(false);
            deleteButton.setEnabled(false);
//            openButton.setEnabled(false);
            saveAsButton.setEnabled(false);

            blobListTable.setEnabled(false);
        } else {
            blobListTable.setEnabled(true);
            queryButton.setEnabled(true);
            refreshButton.setEnabled(true);
            uploadButton.setEnabled(true);
            blobListTable.setEnabled(true);

            backButton.setEnabled(directoryQueue.size() > 1);
        }
    }

    private BlobDirectory getFolderSelection() {
        Object blobItem = ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
        return blobItem instanceof BlobDirectory ? (BlobDirectory) blobItem : null;
    }

    private BlobFile getFileSelection() {
        Object blobItem = ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
        return blobItem instanceof BlobFile ? (BlobFile) blobItem : null;
    }

    private boolean isDirectorySelected() {
        Object blobItem = ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
        return blobItem instanceof BlobDirectory;
    }

    private void tableSelection() {
        if (isDirectorySelected()) {
            BlobDirectory item = getFolderSelection();

            if (item != null) {
                directoryQueue.addLast(item);
                fillGrid();
            }
        }
    }

    private void deleteSelectedFile() {
        final BlobFile blobItem = getFileSelection();

        if (blobItem != null) {
            if (DefaultLoader.getUIHelper().showConfirmation("Are you sure you want to delete this blob?", "Delete Blob", new String[]{"Yes", "No"}, null)) {
                setUIState(true);

                DefaultLoader.getIdeHelper().runInBackground(null, "Deleting blob...", false, true, "Deleting blob...", new Runnable() {
                    @Override
                    public void run() {
                        try {
                            StorageClientSDKManager.getManager().deleteBlobFile(connectionString, blobItem);

                            if (blobItems.size() <= 1) {
                                directoryQueue.clear();
                                directoryQueue.addLast(StorageClientSDKManager.getManager().getRootDirectory(connectionString, blobContainer));
                            }

                            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (blobItems.size() <= 1) {
                                        queryTextField.setText("");
                                    }
                                    fillGrid();
                                }
                            });
                        } catch (AzureCmdException ex) {
                            DefaultLoader.getUIHelper().showException("Error deleting blob.", ex, "Error deleting blob", false, true);
                        }
                    }
                });
            }
        }
    }

    private void copyURLSelectedFile() {
        BlobFile fileSelection = getFileSelection();
        if (fileSelection != null) {
            final Clipboard cb = new Clipboard(PlatformUI.getWorkbench().getDisplay());
            cb.setContents(new Object[]{fileSelection.getUri()}, new Transfer[]{TextTransfer.getInstance()});
        }
    }

    private void saveAsSelectedFile() {
        BlobFile fileSelection = getFileSelection();

        assert fileSelection != null;
        File file = DefaultLoader.getUIHelper().showFileChooser("Save As");
        if (file != null) {
            downloadSelectedFile(file, false);
        }
    }

    private void downloadSelectedFile(boolean open) {
        String defaultFolder = System.getProperty("user.home") + File.separator + "Downloads";
        BlobFile fileSelection = getFileSelection();

        if (fileSelection != null) {
            downloadSelectedFile(new File(defaultFolder + File.separator + fileSelection.getName()), open);
        }
    }

    private void downloadSelectedFile(final File targetFile, final boolean open) {
        final BlobFile fileSelection = getFileSelection();

        if (fileSelection != null) {
            Job job = new Job("Downloading blob...") {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    monitor.beginTask("Downloading blob...", IProgressMonitor.UNKNOWN);
                    try {
                        if (!targetFile.exists()) {
                            if (!targetFile.createNewFile()) {
                                throw new IOException("File not created");
                            }
                        }

                        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetFile), 65536) {
                            private long runningCount = 0;

                            @Override
                            public synchronized void write(@NotNull byte[] bytes, int i, int i1) throws IOException {
                                super.write(bytes, i, i1);

                                runningCount += i1;

                                double progress = (double) runningCount / fileSelection.getSize();
                                monitor.worked((int) (100 * progress));
                                monitor.subTask(String.format("%s%% downloaded", (int) (progress * 100)));
                            }
                        };

                        try {
//                            Future<?> future = DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
//                                @Override
//                                public void run() {
                            try {
                                StorageClientSDKManager.getManager().downloadBlobFileContent(connectionString, fileSelection, bufferedOutputStream);

                                if (open && targetFile.exists()) {
                                    try {
                                        final Process p;
                                        Runtime runtime = Runtime.getRuntime();
                                        p = runtime.exec(
                                                new String[]{"open", "-R", targetFile.getName()},
                                                null,
                                                targetFile.getParentFile());

                                        InputStream errorStream = p.getErrorStream();
                                        String errResponse = new String(IOUtils.readFully(errorStream, -1));

                                        if (p.waitFor() != 0) {
                                            throw new Exception(errResponse);
                                        }
                                    } catch (Exception e) {
                                        monitor.setTaskName("Error opening file");
                                        monitor.subTask(e.getMessage());
                                    }
//                                            Desktop.getDesktop().open(targetFile);
                                }
                            } catch (AzureCmdException e) {
                                Throwable connectionFault = e.getCause().getCause();

                                monitor.setTaskName("Error downloading Blob");
                                monitor.subTask((connectionFault instanceof SocketTimeoutException) ? "Connection timed out" : connectionFault.getMessage());
                                return Status.CANCEL_STATUS;
                            }
                        } finally {
                            bufferedOutputStream.close();
                        }
                    } catch (IOException e) {
                        DefaultLoader.getUIHelper().showException("Error downloading Blob", e, "Error downloading Blob", false, true);
                        return Status.CANCEL_STATUS;
                    } finally {
                        monitor.done();
                    }
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }
    }

    private void uploadFile() {
        final UploadBlobFileForm form = new UploadBlobFileForm(PluginUtil.getParentShell());
        form.setUploadSelected(new Runnable() {
            @Override
            public void run() {
                String path = form.getFolder();
                File selectedFile = form.getSelectedFile();

                if (!path.endsWith("/"))
                    path = path + "/";

                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                path = path + selectedFile.getName();

                uploadFile(path, selectedFile);
            }
        });

        form.open();
    }

    private void uploadFile(final String path, final File selectedFile) {
        Job job = new Job("Uploading blob...") {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                monitor.beginTask("Uploading blob...", IProgressMonitor.UNKNOWN);
                try {
                    final BlobDirectory blobDirectory = directoryQueue.peekLast();
                    final BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(selectedFile));
                    monitor.subTask("0% uploaded");
                    try {
                        final CallableSingleArg<Void, Long> callable = new CallableSingleArg<Void, Long>() {
                            @Override
                            public Void call(Long uploadedBytes) throws Exception {
                                double progress = ((double) uploadedBytes) / selectedFile.length();

                                monitor.worked((int) (100 * progress));
                                monitor.subTask(String.format("%s%% uploaded", (int) (progress * 100)));

                                return null;
                            }
                        };
                        try {
                            StorageClientSDKManager.getManager().uploadBlobFileContent(
                                    connectionString,
                                    blobContainer,
                                    path,
                                    bufferedInputStream,
                                    callable,
                                    1024 * 1024,
                                    selectedFile.length());
                        } catch (AzureCmdException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                bufferedInputStream.close();
                            } catch (IOException ignored) {
                            }
                        }
//                        while (!future.isDone()) {
//                            Thread.sleep(500);
//                            progressIndicator.checkCanceled();

                        if (monitor.isCanceled()) {
//                                future.cancel(true);
                            bufferedInputStream.close();

                            for (BlobItem blobItem : StorageClientSDKManager.getManager().getBlobItems(connectionString, blobDirectory)) {
                                if (blobItem instanceof BlobFile && blobItem.getPath().equals(path)) {
                                    StorageClientSDKManager.getManager().deleteBlobFile(connectionString, (BlobFile) blobItem);
                                }
                            }
                        }
//                        }

                        try {
                            directoryQueue.clear();
                            directoryQueue.addLast(StorageClientSDKManager.getManager().getRootDirectory(connectionString, blobContainer));

                            for (String pathDir : path.split("/")) {
                                for (BlobItem blobItem : StorageClientSDKManager.getManager().getBlobItems(connectionString, directoryQueue.getLast())) {
                                    if (blobItem instanceof BlobDirectory && blobItem.getName().equals(pathDir)) {
                                        directoryQueue.addLast((BlobDirectory) blobItem);
                                    }
                                }
                            }
                        } catch (AzureCmdException e) {
                            DefaultLoader.getUIHelper().showException("Error showing new blob", e, "Error showing new blob", false, true);
                        }

                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fillGrid();
                            }
                        });
                    } catch (Exception e) {
                        Throwable connectionFault = e.getCause();
                        Throwable realFault = null;

                        if (connectionFault != null) {
                            realFault = connectionFault.getCause();
                        }

                        monitor.setTaskName("Error uploading Blob");
                        String message = realFault == null ? null : realFault.getMessage();

                        if (connectionFault != null && message == null) {
                            message = "Error type " + connectionFault.getClass().getName();
                        }

                        monitor.subTask((connectionFault instanceof SocketTimeoutException) ? "Connection timed out" : message);
                    }
                } catch (Exception e) {
                    DefaultLoader.getUIHelper().showException("Error uploading Blob", e, "Error uploading Blob", false, true);
                    return Status.CANCEL_STATUS;
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    @Override
    public void setFocus() {
    }

    private class BlobListContentProvider implements IStructuredContentProvider {
        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object o, Object o1) {
        }

        @Override
        public Object[] getElements(Object o) {
            return blobItems.toArray();
        }
    }

    private class BlobListLabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(Object o, int i) {
            if (i == 0 && o instanceof BlobDirectory) {
                return Activator.getImageDescriptor("icons/storagefolder.png").createImage();
            }
            return null;
        }

        @Override
        public String getColumnText(Object o, int colIndex) {
            BlobItem blobItem = (BlobItem) o;
            if (blobItem instanceof BlobDirectory) {
                switch (colIndex) {
                    case 1:
                        return blobItem.getName();
                    case 5:
                        return blobItem.getUri();
                    default:
                        return "";
                }
            } else {
                BlobFile blobFile = (BlobFile) blobItem;
                switch (colIndex) {
                    case 1:
                        return blobFile.getName();
                    case 2:
                        return UIHelperImpl.readableFileSize(blobFile.getSize());
                    case 3:
                        return new SimpleDateFormat().format(blobFile.getLastModified().getTime());
                    case 4:
                        return blobFile.getContentType();
                    case 5:
                        return blobItem.getUri();
                    default:
                        return "";
                }
            }
        }

        @Override
        public void addListener(ILabelProviderListener iLabelProviderListener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object o, String s) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener iLabelProviderListener) {
        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("Container", this.blobContainer.getName());
        properties.put("Storage", this.storageAccount);
        return properties;
    }
}
