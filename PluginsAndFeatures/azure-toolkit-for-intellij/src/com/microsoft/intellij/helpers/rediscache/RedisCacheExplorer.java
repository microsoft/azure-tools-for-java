/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.helpers.rediscache;

import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;

import com.microsoft.azuretools.azurecommons.helpers.RedisKeyType;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisScanResult;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisValueData;
import com.microsoft.icons.CommonIcons;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.intellij.ui.components.AzureActionListenerWrapper;
import com.microsoft.intellij.ui.components.AzureListSelectionListenerWrapper;
import com.microsoft.intellij.ui.components.EnvironmentUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisExplorerMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisExplorerPresenter;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Collections;


public class RedisCacheExplorer extends BaseEditor implements RedisExplorerMvpView {

    public static final String ID = "com.microsoft.intellij.helpers.rediscache.RedisCacheExplorer";
    public static final String INSIGHT_NAME = "AzurePlugin.IntelliJ.Editor.RedisCacheExplorer";

    private String currentCursor;
    private String lastChosenKey;

    private final RedisExplorerPresenter<RedisCacheExplorer> redisExplorerPresenter;

    private static final String[] LIST_TITLE = new String[]{" Index", " Item"};
    private static final String[] SET_TITLE = new String[]{" Member"};
    private static final String[] ZSET_TITLE = new String[]{" Score", " Member"};
    private static final String[] HASH_TITLE = new String[]{" Field", " Value"};

    private static final String TABLE_HEADER_FONT = "Segoe UI";
    private static final int TABLE_HEADER_FONT_SIZE = 16;
    private static final int SPLIT_PANE_DIVIDER_SIZE = 2;
    private static final double SPLIT_PANE_WEIGHT = 0.4;

    private static final String DEFAULT_SCAN_PATTERN = "*";
    private static final String ACTION_GET = "GET";
    private static final String ACTION_SCAN = "SCAN";

    private JPanel pnlMain;
    private JComboBox cbDatabase;
    private JComboBox cbActionType;
    private JTextField txtKeyPattern;
    private JButton btnSearch;
    private JList lstKey;
    private JButton btnScanMore;
    private JTable tblInnerValue;
    private JTextArea txtStringValue;
    private JLabel lblTypeValue;
    private JLabel lblKeyValue;
    private JProgressBar progressBar;
    private JScrollPane pnlInnerValue;
    private JPanel pnlStringValue;
    private JSplitPane splitPane;
    private JPanel pnlProgressBar;

    /**
     *
     * @param sid String, subscription id.
     * @param id String, resource id.
     */
    public RedisCacheExplorer(String sid, String id) {
        redisExplorerPresenter = new RedisExplorerPresenter<>();
        redisExplorerPresenter.onAttachView(this);
        redisExplorerPresenter.initializeResourceData(sid, id);
        currentCursor = SCAN_POINTER_START;
        lastChosenKey = "";

        cbActionType.addItem(ACTION_SCAN);
        cbActionType.addItem(ACTION_GET);

        splitPane.setResizeWeight(SPLIT_PANE_WEIGHT);
        splitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);

        Font valueFont = new Font(TABLE_HEADER_FONT, Font.PLAIN, TABLE_HEADER_FONT_SIZE);
        tblInnerValue.getTableHeader().setFont(valueFont);
        txtStringValue.setFont(valueFont);
        DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) tblInnerValue.getTableHeader()
                .getDefaultRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        pnlInnerValue.setBackground(lstKey.getBackground());

        progressBar.setIndeterminate(true);

        cbDatabase.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "cbDatabase", null) {
            @Override
            public void actionPerformedFunc(ActionEvent event) {
                if (cbActionType.getSelectedItem().equals(ACTION_GET)) {
                    return;
                }
                RedisCacheExplorer.this.setWidgetEnableStatus(false);
                txtKeyPattern.setText(DEFAULT_SCAN_PATTERN);
                RedisCacheExplorer.this.onDataBaseSelect();
            }
        });

        lstKey.addListSelectionListener(new AzureListSelectionListenerWrapper(INSIGHT_NAME, "lstKey", null) {
            @Override
            public void valueChangedFunc(ListSelectionEvent event) {
                String selectedKey = (String) lstKey.getSelectedValue();
                if (selectedKey == null || selectedKey.equals(lastChosenKey)) {
                    return;
                }
                RedisCacheExplorer.this.setWidgetEnableStatus(false);
                lastChosenKey = selectedKey;
                redisExplorerPresenter.onkeySelect(cbDatabase.getSelectedIndex(), selectedKey);
            }
        });

        btnSearch.setIcon(CommonIcons.INSTANCE.getSearch());
        btnSearch.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "btnSearch", null) {
            @Override
            public void actionPerformedFunc(ActionEvent event) {
                RedisCacheExplorer.this.onBtnSearchClick();
            }
        });

        btnScanMore.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "btnScanMore", null) {
            @Override
            public void actionPerformedFunc(ActionEvent event) {
                RedisCacheExplorer.this.setWidgetEnableStatus(false);
                redisExplorerPresenter.onKeyList(cbDatabase.getSelectedIndex(),
                        currentCursor, txtKeyPattern.getText());
            }
        });

        txtKeyPattern.addActionListener(event -> onBtnSearchClick());

        cbActionType.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "cbActionType", null) {
            @Override
            public void actionPerformedFunc(ActionEvent event) {
                String selected = (String) cbActionType.getSelectedItem();
                if (selected.equals(ACTION_GET)) {
                    btnScanMore.setEnabled(false);
                } else if (selected.equals(ACTION_SCAN)) {
                    btnScanMore.setEnabled(true);
                }
            }
        });

        redisExplorerPresenter.onReadDbNum();

    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return pnlMain;
    }

    @NotNull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        redisExplorerPresenter.onDetachView();
    }

    @Override
    public void renderDbCombo(int num) {
        for (int i = 0; i < num; i++) {
            cbDatabase.addItem(String.valueOf(i));
        }
        if (num > 0) {
            onDataBaseSelect();
        }
    }

    @Override
    public void showScanResult(RedisScanResult result) {
        lstKey.removeAll();
        DefaultListModel listModel = new DefaultListModel();
        List<String> keys = result.getKeys();
        Collections.sort(keys);
        for (String key : keys) {
            listModel.addElement(key);
        }
        lstKey.setModel(listModel);
        currentCursor = result.getNextCursor();
        setWidgetEnableStatus(true);
        clearValueArea();
    }

    @Override
    public void showContent(RedisValueData val) {
        RedisKeyType type = val.getKeyType();
        lblTypeValue.setText(type.toString());
        lblKeyValue.setText((String) lstKey.getSelectedValue());
        if (type == RedisKeyType.STRING) {
            if (val.getRowData().size() > 0 && val.getRowData().get(0).length > 0) {
                txtStringValue.setText(val.getRowData().get(0)[0]);
            }
            setValueCompositeVisible(false);
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
            String[][] data = new String[val.getRowData().size()][columnNames.length];
            data = val.getRowData().toArray(data);

            ReadOnlyTableModel tableModel = new ReadOnlyTableModel(data, columnNames);
            setValueCompositeVisible(true);
            tblInnerValue.setModel(tableModel);
        }
        setWidgetEnableStatus(true);
    }

    @Override
    public void updateKeyList() {
        DefaultListModel listModel = (DefaultListModel) lstKey.getModel();
        listModel.removeAllElements();
        listModel.addElement(txtKeyPattern.getText());
        lstKey.setModel(listModel);
        lstKey.setSelectedIndex(0);
    }

    @Override
    public void getKeyFail() {
        DefaultListModel listModel = (DefaultListModel) lstKey.getModel();
        listModel.removeAllElements();
        setWidgetEnableStatus(true);
        clearValueArea();
    }

    @Override
    public void onErrorWithException(String message, Exception ex) {
        setWidgetEnableStatus(true);
        DefaultLoader.getUIHelper().logError(
                "Redis cache exception: " + message + System.lineSeparator() + ex.getMessage(), ex);
    }

    private void onDataBaseSelect() {
        redisExplorerPresenter.onDbSelect(cbDatabase.getSelectedIndex());
    }

    private void setWidgetEnableStatus(boolean enabled) {
        pnlProgressBar.setVisible(!enabled);
        cbDatabase.setEnabled(enabled);
        txtKeyPattern.setEnabled(enabled);
        btnSearch.setEnabled(enabled);
        lstKey.setEnabled(enabled);
        cbActionType.setEnabled(enabled);
        String actionType = (String) cbActionType.getSelectedItem();
        btnScanMore.setEnabled(enabled && actionType.equals(ACTION_SCAN));
    }

    private void clearValueArea() {
        lblKeyValue.setText("");
        lblTypeValue.setText("");
        pnlInnerValue.setVisible(false);
        pnlStringValue.setVisible(false);
    }

    private void setValueCompositeVisible(boolean showTable) {
        pnlInnerValue.setVisible(showTable);
        pnlStringValue.setVisible(!showTable);

    }

    private void onBtnSearchClick() {
        setWidgetEnableStatus(false);
        String actionType = (String) cbActionType.getSelectedItem();
        String key = txtKeyPattern.getText();
        int dbIdx = cbDatabase.getSelectedIndex();
        if (actionType.equals(ACTION_GET)) {
            redisExplorerPresenter.onGetKeyAndValue(dbIdx, key);
        } else if (actionType.equals(ACTION_SCAN)) {
            redisExplorerPresenter.onKeyList(dbIdx, SCAN_POINTER_START, key);
            currentCursor = SCAN_POINTER_START;
        }
        lastChosenKey = "";
    }

    private class ReadOnlyTableModel extends DefaultTableModel {
        ReadOnlyTableModel(Object[][] data, String[] columnNames) {
            super(data, columnNames);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
