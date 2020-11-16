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

package com.microsoft.intellij.helpers.storage;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.ISubscriptionSelectionListener;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.forms.UploadBlobFileForm;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.CallableSingleArg;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.BlobDirectory;
import com.microsoft.tooling.msservices.model.storage.BlobFile;
import com.microsoft.tooling.msservices.model.storage.BlobItem;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class BlobExplorerFileEditor implements FileEditor, TelemetryProperties {
    static final String OPEN = "Open";
    static final String COPY_URL = "Copy URL";
    static final String SAVE_AS = "Save As";
    static final String DELETE = "Delete";
    static final String UPLOAD = "Upload";
    static final String QUERY = "Query";

    private JPanel mainPanel;
    private JTextField queryTextField;
    private JTable blobListTable;
    private JButton queryButton;
    private JButton refreshButton;
    private JButton uploadButton;
    private JButton deleteButton;
    private JButton openButton;
    private JButton saveAsButton;
    private JButton backButton;
    private JLabel pathLabel;

    private String connectionString;
    private BlobContainer blobContainer;
    private String storageAccount;
    private Project project;

    private LinkedList<BlobDirectory> directoryQueue = new LinkedList<BlobDirectory>();
    private List<BlobItem> blobItems;

    private ISubscriptionSelectionListener subscriptionListener;
    private FileEditorVirtualNode fileEditorVirtualNode;

    public BlobExplorerFileEditor(Project project) {
        this.project = project;
        blobListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }

            public Class getColumnClass(int column) {
                return (column == 0) ? Icon.class : String.class;
            }
        };

        model.addColumn("");
        model.addColumn("Name");
        model.addColumn("Size");
        model.addColumn("Last Modified (UTC)");
        model.addColumn("Content Type");
        model.addColumn("URL");

        blobListTable.setModel(model);
        blobListTable.getColumnModel().getColumn(0).setMinWidth(20);
        blobListTable.getColumnModel().getColumn(0).setMaxWidth(20);
        blobListTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        blobListTable.getColumnModel().getColumn(2).setPreferredWidth(10);
        blobListTable.getColumnModel().getColumn(3).setPreferredWidth(15);
        blobListTable.getColumnModel().getColumn(4).setPreferredWidth(40);

        JTableHeader tableHeader = blobListTable.getTableHeader();
        Dimension headerSize = tableHeader.getPreferredSize();
        headerSize.setSize(headerSize.getWidth(), 18);
        tableHeader.setPreferredSize(headerSize);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        fileEditorVirtualNode = createVirtualNode("");

        blobListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                boolean directorySelected = isDirectorySelected() && blobListTable.getSelectedRow() >= 0;

                deleteButton.setEnabled(!directorySelected);
                openButton.setEnabled(!directorySelected);
                saveAsButton.setEnabled(!directorySelected);
            }
        });

        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);

        sorter.setComparator(2, new Comparator<String>() {
            @Override
            public int compare(String f, String s) {
                String first = f;
                String second = s;
                if (first == null || first.isEmpty()) {
                    first = "0";
                }

                if (second == null || second.isEmpty()) {
                    second = "0";
                }

                return getValue(first).compareTo(getValue(second));
            }

            private Long getValue(String strValue) {
                if (strValue.endsWith("kB")) {
                    double l = Double.parseDouble(strValue.substring(0, strValue.length() - 2));
                    return (long) (l * 1024);
                } else if (strValue.endsWith("MB")) {
                    double l = Double.parseDouble(strValue.substring(0, strValue.length() - 2));
                    return (long) (l * 1024 * 1024);
                } else if (strValue.endsWith("GB")) {
                    double l = Double.parseDouble(strValue.substring(0, strValue.length() - 2));
                    return (long) (l * 1024 * 1024 * 1024);
                } else if (strValue.endsWith("TB")) {
                    double l = Double.parseDouble(strValue.substring(0, strValue.length() - 2));
                    return (long) (l * 1024 * 1024 * 1024 * 1024);
                } else {
                    String value = strValue.substring(0, strValue.length() - 1);

                    if (value.isEmpty()) {
                        return 0L;
                    }

                    double l = Double.parseDouble(value);
                    return (long) l;
                }
            }
        });

        blobListTable.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();

        sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                directoryQueue.pollLast();

                fillGrid();
            }
        });

        blobListTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getComponent() instanceof JTable) {
                    int r = blobListTable.rowAtPoint(me.getPoint());

                    if (r >= 0 && r < blobListTable.getRowCount()) {
                        blobListTable.setRowSelectionInterval(r, r);
                    } else {
                        blobListTable.clearSelection();
                    }

                    int rowIndex = blobListTable.getSelectedRow();

                    if (rowIndex < 0) {
                        return;
                    }

                    if (me.getClickCount() == 2) {
                        tableSelection();
                    }

                    if (me.getButton() == 3) {
                        BlobFile fileSelection = getFileSelection();

                        if (fileSelection != null) {
                            JPopupMenu popup = createTablePopUp();
                            popup.show(me.getComponent(), me.getX(), me.getY());
                        }
                    }
                }
            }
        });

        blobListTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    tableSelection();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });

        ActionListener queryAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(QUERY).fireNodeActionEvent();
            }
        };

        refreshButton.addActionListener(queryAction);
        queryButton.addActionListener(queryAction);

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(DELETE).fireNodeActionEvent();
            }
        });

        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(SAVE_AS).fireNodeActionEvent();
            }
        });

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(OPEN).fireNodeActionEvent();
            }
        });

        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(UPLOAD).fireNodeActionEvent();
            }
        });

        addSubscriptionSelectionListener();
    }

    public void fillGrid() {
        setUIState(true);

        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Loading blobs...", false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            try {
                progressIndicator.setIndeterminate(true);

                if (directoryQueue.peekLast() == null) {
                    directoryQueue.addLast(StorageClientSDKManager.getManager().getRootDirectory(connectionString, blobContainer));
                }

                blobItems = StorageClientSDKManager.getManager().getBlobItems(connectionString, directoryQueue.peekLast());

                if (!queryTextField.getText().isEmpty()) {
                    for (int i = blobItems.size() - 1; i >= 0; i--) {
                        BlobItem blobItem = blobItems.get(i);

                        if (blobItem instanceof BlobFile && !blobItem.getName().startsWith(queryTextField.getText())) {
                            blobItems.remove(i);
                        }
                    }
                }

                ApplicationManager.getApplication().invokeLater(() -> {

                    pathLabel.setText(directoryQueue.peekLast().getPath());
                    DefaultTableModel model = (DefaultTableModel) blobListTable.getModel();

                    while (model.getRowCount() > 0) {
                        model.removeRow(0);
                    }

                    for (BlobItem blobItem : blobItems) {
                        if (blobItem instanceof BlobDirectory) {
                            model.addRow(new Object[]{
                                UIHelperImpl.loadIcon("storagefolder.png"),
                                blobItem.getName(),
                                "",
                                "",
                                "",
                                blobItem.getUri()
                            });
                        } else {
                            BlobFile blobFile = (BlobFile) blobItem;

                            model.addRow(new String[]{
                                "",
                                blobFile.getName(),
                                UIHelperImpl.readableFileSize(blobFile.getSize()),
                                new SimpleDateFormat().format(blobFile.getLastModified().getTime()),
                                blobFile.getContentType(),
                                blobFile.getUri()
                            });
                        }
                    }

                    setUIState(false);

                    blobListTable.clearSelection();
                });
            } catch (AzureCmdException ex) {
                String msg = "An error occurred while attempting to query blob list." + "\n" + String.format(message("webappExpMsg"), ex.getMessage());
                PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, ex);
            }
        }));
    }

    private void setUIState(boolean loading) {
        if (loading) {
            blobListTable.setEnabled(false);
            backButton.setEnabled(false);
            queryButton.setEnabled(false);
            refreshButton.setEnabled(false);
            uploadButton.setEnabled(false);
            deleteButton.setEnabled(false);
            openButton.setEnabled(false);
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
        if (blobListTable.getSelectedRow() >= 0) {
            String name = blobListTable.getValueAt(blobListTable.getSelectedRow(), 1).toString();

            for (BlobItem item : blobItems) {
                if (item instanceof BlobDirectory && item.getName().equals(name)) {
                    return (BlobDirectory) item;
                }
            }
        }

        return null;
    }

    private BlobFile getFileSelection() {
        if (blobListTable.getSelectedRow() >= 0) {
            String name = blobListTable.getValueAt(blobListTable.getSelectedRow(), 1).toString();

            for (BlobItem item : blobItems) {
                if (item instanceof BlobFile && item.getName().equals(name)) {
                    return (BlobFile) item;
                }
            }
        }

        return null;
    }

    private boolean isDirectorySelected() {
        int selectedRow = blobListTable.getSelectedRow();

        if (selectedRow < 0) {
            return false;
        } else {
            Object icon = blobListTable.getValueAt(selectedRow, 0);
            return icon instanceof Icon;
        }
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

    private FileEditorVirtualNode createVirtualNode(final String name) {
        final FileEditorVirtualNode node = new FileEditorVirtualNode(this, name);
        node.addAction(OPEN, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                downloadSelectedFile(true);
            }
        });

        node.addAction(SAVE_AS, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                saveAsSelectedFile();
            }
        });

        node.addAction(COPY_URL, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                copyURLSelectedFile();
            }
        });

        node.addAction(DELETE, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                deleteSelectedFile();
            }
        });

        node.addAction(UPLOAD, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                uploadFile();
            }
        });

        node.addAction(QUERY, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                fillGrid();
            }
        });

        return node;
    }

    private JPopupMenu createTablePopUp() {
        final JPopupMenu menu = new JPopupMenu();
        menu.add(fileEditorVirtualNode.createJMenuItem(OPEN));
        menu.add(fileEditorVirtualNode.createJMenuItem(SAVE_AS));
        menu.add(fileEditorVirtualNode.createJMenuItem(COPY_URL));
        menu.add(fileEditorVirtualNode.createJMenuItem(DELETE));

        return menu;
    }

    private void deleteSelectedFile() {
        final BlobFile blobItem = getFileSelection();

        if (blobItem != null) {
            boolean isConfirm = DefaultLoader.getUIHelper().showYesNoDialog(mainPanel, "Are you sure you want to "
                    + "delete this blob?", "Delete Blob", null);
            if (isConfirm) {
                setUIState(true);

                AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Deleting blob...", false, () -> {
                    final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
                    progressIndicator.setIndeterminate(true);
                    try {
                        StorageClientSDKManager.getManager().deleteBlobFile(connectionString, blobItem);

                        if (blobItems.size() <= 1) {
                            directoryQueue.clear();
                            directoryQueue.addLast(StorageClientSDKManager.getManager().getRootDirectory(connectionString, blobContainer));

                            queryTextField.setText("");
                        }

                        ApplicationManager.getApplication().invokeLater(this::fillGrid);
                    } catch (AzureCmdException ex) {
                        String msg = "An error occurred while attempting to delete blob." + "\n" + String.format(message("webappExpMsg"), ex.getMessage());
                        PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, ex);
                    }
                }));
            }
        }
    }

    private void copyURLSelectedFile() {
        BlobFile fileSelection = getFileSelection();

        if (fileSelection != null) {
            StringSelection selection = new StringSelection(fileSelection.getUri());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }

    private void saveAsSelectedFile() {
        BlobFile fileSelection = getFileSelection();

        assert fileSelection != null;

        FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(SAVE_AS, "Select location to save blob file.");
        final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, this.project);
        final VirtualFileWrapper save = dialog.save(LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")), "");

        if (save != null) {
            downloadSelectedFile(save.getFile(), false);
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
            AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Downloading blob...", true, () -> {
                final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
                try {
                    progressIndicator.setIndeterminate(false);

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
                            progressIndicator.setFraction(progress);
                            progressIndicator.setText2(String.format("%s%% downloaded", (int) (progress * 100)));
                        }
                    };

                    try {
                        Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    StorageClientSDKManager.getManager().downloadBlobFileContent(connectionString, fileSelection, bufferedOutputStream);

                                    if (open && targetFile.exists()) {
                                        Desktop.getDesktop().open(targetFile);
                                    }
                                } catch (AzureCmdException e) {
                                    Throwable connectionFault = e.getCause().getCause();

                                    progressIndicator.setText("Error downloading Blob");
                                    progressIndicator.setText2((connectionFault instanceof SocketTimeoutException) ?
                                                               "Connection timed out" : connectionFault.getMessage());
                                } catch (IOException ex) {
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
                                        progressIndicator.setText("Error openning file");
                                        progressIndicator.setText2(ex.getMessage());
                                    }
                                }
                            }
                        });

                        while (!future.isDone()) {
                            progressIndicator.checkCanceled();

                            if (progressIndicator.isCanceled()) {
                                future.cancel(true);
                            }
                        }
                    } finally {
                        bufferedOutputStream.close();
                    }
                } catch (IOException e) {
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), "An error occurred while attempting to download Blob.", e);
                }
            }));
        }
    }

    private void uploadFile() {
        final UploadBlobFileForm form = new UploadBlobFileForm(project);
        form.setUploadSelected(new Runnable() {
            @Override
            public void run() {
                String path = form.getFolder();
                File selectedFile = form.getSelectedFile();

                if (!path.endsWith("/")) {
                    path = path + "/";
                }

                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                path = path + selectedFile.getName();

                uploadFile(path, selectedFile);
            }
        });

        form.show();
    }

    private void uploadFile(final String path, final File selectedFile) {
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Uploading blob...", true, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            try {
                final BlobDirectory blobDirectory = directoryQueue.peekLast();

                final BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(selectedFile));

                progressIndicator.setIndeterminate(false);
                progressIndicator.setText("Uploading blob...");
                progressIndicator.setText2("0% uploaded");

                try {
                    final CallableSingleArg<Void, Long> callable = new CallableSingleArg<Void, Long>() {
                        @Override
                        public Void call(Long uploadedBytes) throws Exception {
                            double progress = ((double) uploadedBytes) / selectedFile.length();

                            progressIndicator.setFraction(progress);
                            progressIndicator.setText2(String.format("%s%% uploaded", (int) (progress * 100)));

                            return null;
                        }
                    };

                    Future<Void> future = ApplicationManager.getApplication().executeOnPooledThread(new Callable<Void>() {
                        @Override
                        public Void call() throws AzureCmdException {
                            try {
                                StorageClientSDKManager.getManager().uploadBlobFileContent(
                                    connectionString,
                                    blobContainer,
                                    path,
                                    bufferedInputStream,
                                    callable,
                                    1024 * 1024,
                                    selectedFile.length());
                            } finally {
                                try {
                                    bufferedInputStream.close();
                                } catch (IOException ignored) {
                                }
                            }

                            return null;
                        }
                    });

                    while (!future.isDone()) {
                        Thread.sleep(500);
                        progressIndicator.checkCanceled();

                        if (progressIndicator.isCanceled()) {
                            future.cancel(true);
                            bufferedInputStream.close();

                            for (BlobItem blobItem : StorageClientSDKManager.getManager().getBlobItems(connectionString, blobDirectory)) {
                                if (blobItem instanceof BlobFile && blobItem.getPath().equals(path)) {
                                    StorageClientSDKManager.getManager().deleteBlobFile(connectionString, (BlobFile) blobItem);
                                }
                            }
                        }
                    }

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
                        String msg = "An error occurred while attempting to show new blob." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                        PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                    }

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
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

                    progressIndicator.setText("Error uploading Blob");
                    String message = realFault == null ? null : realFault.getMessage();

                    if (connectionFault != null && message == null) {
                        message = "Error type " + connectionFault.getClass().getName();
                    }

                    progressIndicator.setText2((connectionFault instanceof SocketTimeoutException) ? "Connection timed out" : message);
                }
            } catch (IOException e) {
                PluginUtil.displayErrorDialogAndLog(message("errTtl"), "An error occurred while attempting to upload Blob.", e);
            }
        }));
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return queryButton;
    }

    @NotNull
    @Override
    public String getName() {
        return blobContainer.getName();
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public void dispose() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null || subscriptionListener == null) {
                return;
            }
            azureManager.getSubscriptionManager().removeListener(subscriptionListener);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setBlobContainer(BlobContainer blobContainer) {
        this.blobContainer = blobContainer;
        this.fileEditorVirtualNode.setName(blobContainer.getName());
    }

    public void setStorageAccount(String storageAccountName) {
        this.storageAccount = storageAccountName;
    }

    private void addSubscriptionSelectionListener() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }

            azureManager.getSubscriptionManager().addListener(new ISubscriptionSelectionListener() {
                @Override
                public void update(boolean isSignedOut) {
                    if (isSignedOut) {
                        Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(project, storageAccount, blobContainer);

                        if (openedFile != null) {
                            DefaultLoader.getIdeHelper().closeFile(project, openedFile);
                        }
                    }
                }
            });
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("Container", this.getName());
        return properties;
    }
}
