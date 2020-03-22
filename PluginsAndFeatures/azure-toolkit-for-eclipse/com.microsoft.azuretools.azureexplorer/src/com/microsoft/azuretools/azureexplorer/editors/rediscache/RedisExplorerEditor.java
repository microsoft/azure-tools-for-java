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
package com.microsoft.azuretools.azureexplorer.editors.rediscache;

import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;

import com.microsoft.azuretools.azurecommons.helpers.RedisKeyType;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.core.components.AzureListenerWrapper;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisScanResult;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisValueData;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisExplorerMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisExplorerPresenter;

import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

public class RedisExplorerEditor extends EditorPart implements RedisExplorerMvpView {

    // Identifier of this class
    public static final String ID = "com.microsoft.azuretools.azureexplorer.editors.rediscache.RedisExplorerEditor";
    public static final String INSIGHT_NAME = "AzurePlugin.Eclipse.Editor.RedisExplorerEditor";

    // Icon path for the search button
    private static final String SEARCH_ICON_PATH = "icons/search.png";

    // Presenter instance
    private final RedisExplorerPresenter<RedisExplorerEditor> redisExplorerPresenter;

    // Local variables
    private String currentCursor;
    private String lastChosenKey;

    // Constants
    private static final String[] LIST_TITLE = new String[] { "Index", "Item" };
    private static final String[] SET_TITLE = new String[] { "Member" };
    private static final String[] ZSET_TITLE = new String[] { "Score", "Member" };
    private static final String[] HASH_TITLE = new String[] { "Field", "Value" };

    private static final String DEFAULT_SCAN_PATTERN = "*";
    private static final String ACTION_GET = "GET";
    private static final String ACTION_SCAN = "SCAN";

    private static final int NO_MARGIN = 0;
    private static final int PROGRESS_MARGIN_TOP = 2;
    private static final int PROGRESS_MARGIN_BOTTOM = 3;
    private static final int PROGRESS_WIDTH = 75;

    // Widgets
    private Combo cbDatabase;
    private Text txtKeyPattern;
    private Button btnSearch;
    private List lstKey;
    private Composite cmpoInnerValue;
    private Composite cmpoStringValue;
    private Table tblInnerValue;
    private Text txtStringValue;
    private Button btnScanMoreKey;
    private Combo cbActionType;
    private ProgressBar progressBar;
    private Label lblKeyValue;
    private Label lblTypeValue;
    private Composite cmpoValue;

    /**
     * Constructor.
     */
    public RedisExplorerEditor() {
        this.redisExplorerPresenter = new RedisExplorerPresenter<RedisExplorerEditor>();
        this.redisExplorerPresenter.onAttachView(this);
        currentCursor = SCAN_POINTER_START;
        lastChosenKey = "";
    }

    /**
     * Create contents of the editor part.
     *
     * @param parent.
     */
    @Override
    public void createPartControl(Composite parent) {
        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        Composite cmpoMain = new Composite(scrolledComposite, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        cmpoMain.setLayout(gridLayout);

        Label lblChooseDb = new Label(cmpoMain, SWT.NONE);
        lblChooseDb.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblChooseDb.setText("Database :");

        cbDatabase = new Combo(cmpoMain, SWT.READ_ONLY);
        cbDatabase.setEnabled(false);
        GridData cbDataBaseLayout = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        cbDataBaseLayout.minimumWidth = 50;
        cbDatabase.setLayoutData(cbDataBaseLayout);

        SashForm sashForm = new SashForm(cmpoMain, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));

        Composite cmpoKeyArea = new Composite(sashForm, SWT.BORDER);
        cmpoKeyArea.setLayout(new GridLayout(3, false));

        cbActionType = new Combo(cmpoKeyArea, SWT.READ_ONLY);
        cbActionType.setEnabled(false);
        cbActionType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        cbActionType.add(ACTION_SCAN);
        cbActionType.add(ACTION_GET);
        cbActionType.select(0);

        txtKeyPattern = new Text(cmpoKeyArea, SWT.BORDER);
        txtKeyPattern.setEditable(false);
        txtKeyPattern.setText(DEFAULT_SCAN_PATTERN);
        txtKeyPattern.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        btnSearch = new Button(cmpoKeyArea, SWT.NONE);
        btnSearch.setEnabled(false);
        btnSearch.setImage(Activator.getImageDescriptor(SEARCH_ICON_PATH).createImage());

        lstKey = new List(cmpoKeyArea, SWT.BORDER | SWT.V_SCROLL);
        lstKey.setEnabled(false);
        lstKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 3, 1));

        btnScanMoreKey = new Button(cmpoKeyArea, SWT.NONE);
        btnScanMoreKey.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 3, 1));
        btnScanMoreKey.setEnabled(false);
        btnScanMoreKey.setText("Scan More");

        Composite cmpoValueArea = new Composite(sashForm, SWT.NONE);
        GridLayout cmpoValueAreaLayout = new GridLayout(2, false);
        cmpoValueAreaLayout.marginWidth = NO_MARGIN;
        cmpoValueAreaLayout.marginHeight = NO_MARGIN;
        cmpoValueArea.setLayout(cmpoValueAreaLayout);

        cmpoValue = new Composite(cmpoValueArea, SWT.BORDER);
        cmpoValue.setLayout(new GridLayout(2, false));
        cmpoValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        GridLayout cmpoKeyInfoLayout = new GridLayout(2, false);
        cmpoKeyInfoLayout.verticalSpacing = 8;
        cmpoKeyInfoLayout.marginHeight = NO_MARGIN;
        cmpoKeyInfoLayout.marginTop = NO_MARGIN;
        cmpoKeyInfoLayout.marginWidth = NO_MARGIN;
        Composite cmpoKeyInfo = new Composite(cmpoValue, SWT.NONE);
        cmpoKeyInfo.setLayout(cmpoKeyInfoLayout);
        cmpoKeyInfo.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 2, 1));

        Label lblType = new Label(cmpoKeyInfo, SWT.NONE);
        lblType.setText("Type :");

        lblTypeValue = new Label(cmpoKeyInfo, SWT.NONE);
        lblTypeValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblKey = new Label(cmpoKeyInfo, SWT.NONE);
        lblKey.setText("Key :");

        lblKeyValue = new Label(cmpoKeyInfo, SWT.NONE);
        lblKeyValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblValue = new Label(cmpoKeyInfo, SWT.NONE);
        lblValue.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblValue.setText("Value :");

        cmpoInnerValue = new Composite(cmpoValue, SWT.NONE);
        cmpoInnerValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        cmpoInnerValue.setVisible(false);
        GridLayout cmpoInnerValueLayout = new GridLayout(1, false);
        cmpoInnerValueLayout.marginHeight = NO_MARGIN;
        cmpoInnerValueLayout.marginWidth = NO_MARGIN;
        cmpoInnerValue.setLayout(cmpoInnerValueLayout);

        tblInnerValue = new Table(cmpoInnerValue, SWT.BORDER | SWT.FULL_SELECTION);
        tblInnerValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tblInnerValue.setHeaderVisible(true);
        tblInnerValue.setLinesVisible(true);

        cmpoStringValue = new Composite(cmpoValue, SWT.NONE);
        cmpoStringValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        cmpoStringValue.setVisible(false);
        GridLayout cmpoStringValueLayout = new GridLayout(1, false);
        cmpoStringValueLayout.marginHeight = NO_MARGIN;
        cmpoStringValueLayout.marginWidth = NO_MARGIN;
        cmpoStringValue.setLayout(cmpoStringValueLayout);

        txtStringValue = new Text(cmpoStringValue, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI);
        txtStringValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        txtStringValue.setBackground(tblInnerValue.getBackground());

        GridLayout cmpoProgressLayout = new GridLayout(1, false);
        cmpoProgressLayout.marginWidth = NO_MARGIN;
        cmpoProgressLayout.marginTop = PROGRESS_MARGIN_TOP;
        cmpoProgressLayout.marginBottom = PROGRESS_MARGIN_BOTTOM;
        Composite cmpoProgress = new Composite(cmpoValueArea, SWT.NONE);
        cmpoProgress.setLayout(cmpoProgressLayout);
        cmpoProgress.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));

        progressBar = new ProgressBar(cmpoProgress, SWT.HORIZONTAL | SWT.INDETERMINATE);
        GridData progressBarLayout = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false, 1, 1);
        progressBarLayout.widthHint = PROGRESS_WIDTH;
        progressBar.setLayoutData(progressBarLayout);
        sashForm.setWeights(new int[] { 1, 1 });

        scrolledComposite.setContent(cmpoMain);
        scrolledComposite.setMinSize(cmpoMain.computeSize(SWT.DEFAULT, SWT.DEFAULT));


        cbDatabase.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "cbDatabase", null) {
            @Override
            protected void handleEventFunc(Event event) {
                if (cbActionType.getText().equals(ACTION_GET)) {
                    return;
                }
                setWidgetEnableStatus(false);
                txtKeyPattern.setText(DEFAULT_SCAN_PATTERN);
                onDataBaseSelect();
            }
        });

        lstKey.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "lstKey", null) {
            @Override
            protected void handleEventFunc(Event event) {
                String selectedKey = lstKey.getItem(lstKey.getSelectionIndex());
                if (selectedKey.equals(lastChosenKey)) {
                    return;
                }
                setWidgetEnableStatus(false);
                lastChosenKey = selectedKey;
                redisExplorerPresenter.onkeySelect(cbDatabase.getSelectionIndex(), selectedKey);
            }
        });

        btnSearch.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "btnSearch", null) {
            @Override
            protected void handleEventFunc(Event event) {
                onBtnSearchClick();
            }
        });

        btnScanMoreKey.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "btnScanMoreKey", null) {
            @Override
            protected void handleEventFunc(Event event) {
                setWidgetEnableStatus(false);
                redisExplorerPresenter.onKeyList(cbDatabase.getSelectionIndex(),
                        currentCursor, txtKeyPattern.getText());
            }
        });

        txtKeyPattern.addListener(SWT.KeyDown, event -> {
            // Enter key pressed
            if (event.keyCode == SWT.CR) {
                onBtnSearchClick();
            }
        });

        cbActionType.addListener(SWT.Selection, new AzureListenerWrapper(INSIGHT_NAME, "cbActionType", null) {
            @Override
            protected void handleEventFunc(Event event) {
                String selected = cbActionType.getText();
                if (selected.equals(ACTION_GET)) {
                    btnScanMoreKey.setEnabled(false);
                } else if (selected.equals(ACTION_SCAN)) {
                    btnScanMoreKey.setEnabled(true);
                }
            }
        });

    }

    @Override
    public void renderDbCombo(int num) {
        for (int i = 0; i < num; i++) {
            cbDatabase.add(String.valueOf(i));
        }
        if (num > 0) {
            cbDatabase.select(0);
            onDataBaseSelect();
        }
    }

    @Override
    public void showScanResult(RedisScanResult result) {
        lstKey.removeAll();
        java.util.List<String> keys = result.getKeys();
        Collections.sort(keys);
        for (String key : keys) {
            lstKey.add(key);
        }
        currentCursor = result.getNextCursor();
        setWidgetEnableStatus(true);
        clearValueArea();
    }

    @Override
    public void updateKeyList() {
        lstKey.removeAll();
        lstKey.add(txtKeyPattern.getText());
        lstKey.select(0);
    }

    @Override
    public void showContent(RedisValueData val) {
        RedisKeyType type = val.getKeyType();
        lblTypeValue.setText(type.toString());
        lblKeyValue.setText(lstKey.getItem(lstKey.getSelectionIndex()));
        if (type.equals(RedisKeyType.STRING)) {
            if (val.getRowData().size() > 0 && val.getRowData().get(0).length > 0) {
                txtStringValue.setText(val.getRowData().get(0)[0]);
            }
            setValueCompositeVisiable(false);
        } else {
            String[] columnNames;
            switch (type) {
                case LIST:
                    columnNames = LIST_TITLE;
                    break;
                case SET:
                    columnNames = SET_TITLE;
                    break;
                case ZSET:
                    columnNames = ZSET_TITLE;
                    break;
                case HASH:
                    columnNames = HASH_TITLE;
                    break;
                default:
                    return;
            }
            tblInnerValue.setRedraw(false);
            // remove all the columns
            while (tblInnerValue.getColumnCount() > 0) {
                tblInnerValue.getColumns()[0].dispose();
            }
            // remove all the items
            tblInnerValue.removeAll();
            // set column title and the number of columns
            TableColumn[] cols = new TableColumn[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                cols[i] = new TableColumn(tblInnerValue, SWT.LEFT);
                cols[i].setText(columnNames[i]);
            }
            // fill in the data for each row
            for (String[] data : val.getRowData()) {
                TableItem item = new TableItem(tblInnerValue, SWT.NONE);
                item.setText(data);
            }
            // after all the data are filled in, call pack() to tell the table
            // calculate each column's width
            for (int i = 0; i < tblInnerValue.getColumnCount(); i++) {
                cols[i].pack();
            }
            tblInnerValue.setRedraw(true);
            setValueCompositeVisiable(true);
        }
        setWidgetEnableStatus(true);
    }

    @Override
    public void onErrorWithException(String message, Exception ex) {
        RedisExplorerMvpView.super.onErrorWithException(message, ex);
        setWidgetEnableStatus(true);
    }

    @Override
    public void onError(String message) {
        RedisExplorerMvpView.super.onError(message);
        setWidgetEnableStatus(true);
    }

    @Override
    public void getKeyFail() {
        lstKey.removeAll();
        setWidgetEnableStatus(true);
        clearValueArea();
    }

    @Override
    public void setFocus() {
        // Set the focus
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // Do the Save operation
    }

    @Override
    public void doSaveAs() {
        // Do the Save As operation
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        if (input instanceof RedisExplorerEditorInput) {
            RedisExplorerEditorInput redisInput = (RedisExplorerEditorInput) input;
            this.redisExplorerPresenter.initializeResourceData(redisInput.getSubscriptionId(), redisInput.getId());
            this.setPartName(redisInput.getRedisName());
            this.redisExplorerPresenter.onReadDbNum();
        }

        IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
        workbench.addWorkbenchListener(new IWorkbenchListener() {
            public boolean preShutdown(IWorkbench workbench, boolean forced) {
                activePage.closeEditor(RedisExplorerEditor.this, true);
                return true;
            }

            public void postShutdown(IWorkbench workbench) {
            }
        });
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
    public void dispose() {
        this.redisExplorerPresenter.onDetachView();
        RedisExplorerEditorInput redisInput = (RedisExplorerEditorInput) this.getEditorInput();
        this.redisExplorerPresenter.onRelease(redisInput.getId());
        super.dispose();
    }

    private void onDataBaseSelect() {
        redisExplorerPresenter.onDbSelect(cbDatabase.getSelectionIndex());
    }

    private void setValueCompositeVisiable(boolean showTable) {
        ((GridData) cmpoInnerValue.getLayoutData()).exclude = !showTable;
        ((GridData) cmpoStringValue.getLayoutData()).exclude = showTable;
        cmpoInnerValue.setVisible(showTable);
        cmpoStringValue.setVisible(!showTable);
        cmpoValue.layout();
    }

    private void setWidgetEnableStatus(boolean enabled) {
        progressBar.setVisible(!enabled);
        cbDatabase.setEnabled(enabled);
        txtKeyPattern.setEditable(enabled);
        btnSearch.setEnabled(enabled);
        lstKey.setEnabled(enabled);
        cbActionType.setEnabled(enabled);
        btnScanMoreKey.setEnabled(enabled && cbActionType.getText().equals(ACTION_SCAN));
    }

    private void clearValueArea() {
        lblKeyValue.setText("");
        lblTypeValue.setText("");
        cmpoInnerValue.setVisible(false);
        cmpoStringValue.setVisible(false);
    }

    private void onBtnSearchClick() {
        setWidgetEnableStatus(false);
        String actionType = cbActionType.getText();
        String key = txtKeyPattern.getText();
        int dbIdx = cbDatabase.getSelectionIndex();
        if (actionType.equals(ACTION_GET)) {
            redisExplorerPresenter.onGetKeyAndValue(dbIdx, key);
        } else if (actionType.equals(ACTION_SCAN)) {
            redisExplorerPresenter.onKeyList(dbIdx, SCAN_POINTER_START, key);
            currentCursor = SCAN_POINTER_START;
        }
        lastChosenKey = "";
    }
}
