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

package com.microsoft.azuretools.azureexplorer.editors.rediscache;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.RedisScanResult;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisExplorerMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisExplorerPresenter;

public class RedisExplorerEditor extends EditorPart implements RedisExplorerMvpView {
    
    public static final String ID = "com.microsoft.azuretools.azureexplorer.editors.rediscache.RedisExplorerEditor";
    
    private final RedisExplorerPresenter<RedisExplorerEditor> redisExplorerPresenter;
    
    private static final String DEFAULT_SCAN_PATTERN = "*";
    private static final String DBNameFormat = "DB<%s>";
    
    private String currentCursor;
    private String sid;
    private String id;
    
    private ScrolledComposite scrolledComposite;
    private Composite cmpoMain;
    private Combo cbDatabase;
    private SashForm sashForm;
    private Composite cmpoKeyArea;
    private Composite cmpoValueArea;
    private Text txtKeyScanPattern;
    private Button btnScanKey;
    private Label lblType;
    private List lstKey;
    private Composite cmpoInnerValue;
    private Composite cmpoStringValue;
    private Table tblInnerValue;
    private Text txtStringValue;
    private Button btnScanMoreKey;
    private Text txtKeyName;
    
    public RedisExplorerEditor() {
        this.redisExplorerPresenter = new RedisExplorerPresenter<RedisExplorerEditor>();
        this.redisExplorerPresenter.onAttachView(this);
        currentCursor = DEFAULT_SCAN_PATTERN;
    }

    /**
     * Create contents of the editor part.
     * @param parent
     */
    @Override
    public void createPartControl(Composite parent) {
        scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        
        cmpoMain = new Composite(scrolledComposite, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        cmpoMain.setLayout(gridLayout);
        
        Label lblChooseDb = new Label(cmpoMain, SWT.NONE);
        lblChooseDb.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblChooseDb.setText("Choose Database");
        
        cbDatabase = new Combo(cmpoMain, SWT.NONE);
        cbDatabase.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        
        sashForm = new SashForm(cmpoMain, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
        
        cmpoKeyArea = new Composite(sashForm, SWT.BORDER);
        cmpoKeyArea.setLayout(new GridLayout(3, false));
        
        txtKeyScanPattern = new Text(cmpoKeyArea, SWT.BORDER);
        txtKeyScanPattern.setText(DEFAULT_SCAN_PATTERN);
        txtKeyScanPattern.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        btnScanKey = new Button(cmpoKeyArea, SWT.NONE);
        btnScanKey.setText("Scan Keys");
        
        btnScanMoreKey = new Button(cmpoKeyArea, SWT.NONE);
        btnScanMoreKey.setText("Scan More Keys");
        
        lstKey = new List(cmpoKeyArea, SWT.BORDER | SWT.V_SCROLL);
        lstKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 3, 1));

        cmpoValueArea = new Composite(sashForm, SWT.NONE);
        cmpoValueArea.setLayout(new GridLayout(2, false));
        
        lblType = new Label(cmpoValueArea, SWT.NONE);
        lblType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblType.setText("type");
        
        txtKeyName = new Text(cmpoValueArea, SWT.BORDER);
        txtKeyName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        cmpoInnerValue = new Composite(cmpoValueArea, SWT.NONE);
        cmpoInnerValue.setLayout(new GridLayout(1, false));
        cmpoInnerValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        
        tblInnerValue = new Table(cmpoInnerValue, SWT.BORDER | SWT.FULL_SELECTION);
        tblInnerValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
        tblInnerValue.setHeaderVisible(true);
        tblInnerValue.setLinesVisible(true);
        
        cmpoStringValue = new Composite(cmpoValueArea, SWT.NONE);
        cmpoStringValue.setLayout(new GridLayout(1, false));
        cmpoStringValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
        
        Label lblStringValue = new Label(cmpoStringValue, SWT.NONE);
        lblStringValue.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        lblStringValue.setText("Value");
        
        txtStringValue = new Text(cmpoStringValue, SWT.BORDER | SWT.MULTI);
        txtStringValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        sashForm.setWeights(new int[] {1, 1});
        scrolledComposite.setContent(cmpoMain);
        scrolledComposite.setMinSize(cmpoMain.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
        cbDatabase.addListener(SWT.Selection, event -> {
            onDataBaseSelect();
        });
    }
    
    @Override
    public void onReadRedisDatabaseNum(String sid, String id) {
        redisExplorerPresenter.onReadDbNum(sid, id);
    }
    
    @Override
    public void renderDbCombo(int num) {
        for(int i = 0; i < num; i++) {
            cbDatabase.add(String.format(DBNameFormat, String.valueOf(i)));
        }
        if (num > 0) {
            cbDatabase.select(0);
            onDataBaseSelect();
        }
    }
    
    @Override
    public void showScanResult(RedisScanResult result) {
        lstKey.removeAll();
        for (String key: result.getKeys()) {
            lstKey.add(key);
        }
        currentCursor = result.getNextCursor();
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
            this.sid = redisInput.getSubscriptionId();
            this.id = redisInput.getId();
            this.onReadRedisDatabaseNum(this.sid, this.id);
            this.setPartName(redisInput.getRedisName());
        }
        
        IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
        workbench.addWorkbenchListener( new IWorkbenchListener() {
            public boolean preShutdown( IWorkbench workbench, boolean forced )     {                            
                activePage.closeEditor(RedisExplorerEditor.this, true);
                return true;
            }
         
            public void postShutdown( IWorkbench workbench ) { }
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
        txtKeyScanPattern.setText(DEFAULT_SCAN_PATTERN);
        redisExplorerPresenter.onDbSelect(sid, id, cbDatabase.getSelectionIndex(), DEFAULT_SCAN_PATTERN);
    }
}
