/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.explorer;

import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisException;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;


public class RedisCacheExplorer extends BaseEditor {

    public static final String ID = "com.microsoft.intellij.helpers.rediscache.RedisCacheExplorer";
    public static final String INSIGHT_NAME = "AzurePlugin.IntelliJ.Editor.RedisCacheExplorer";
    private final RedisCache redis;
    private Jedis jedis;

    private String currentCursor;
    private String lastChosenKey;

    private static final String[] LIST_TITLE = new String[]{" Index", " Item"};
    private static final String[] SET_TITLE = new String[]{" Member"};
    private static final String[] ZSET_TITLE = new String[]{" Score", " Member"};
    private static final String[] HASH_TITLE = new String[]{" Field", " Value"};

    private static final String TABLE_HEADER_FONT = "Segoe UI";
    private static final int TABLE_HEADER_FONT_SIZE = 16;
    private static final int SPLIT_PANE_DIVIDER_SIZE = 2;
    private static final double SPLIT_PANE_WEIGHT = 0.4;
    private static final int DEFAULT_KEY_COUNT = 50;
    private static final int DEFAULT_REDIS_DB_NUMBER = 16;
    private static final int MAX_DATABASE_NUMBER = 64;
    private static final long DEFAULT_RANGE_START = 0;
    private static final int DEFAULT_VAL_COUNT = 500;

    private static final String DEFAULT_SCAN_PATTERN = "*";
    private static final String ACTION_GET = "GET";
    private static final String ACTION_SCAN = "SCAN";

    private JPanel pnlMain;
    private JComboBox<String> cbDatabase;
    private JComboBox<String> cbActionType;
    private JTextField txtKeyPattern;
    private JButton btnSearch;
    private JList<String> lstKey;
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
     * @param sid String, subscription id.
     * @param id  String, resource id.
     */
    public RedisCacheExplorer(String sid, String id, @NotNull final VirtualFile virtualFile) {
        super(virtualFile);
        this.redis = Azure.az(AzureRedis.class).subscription(sid).get(id);
        final AzureTaskManager manager = AzureTaskManager.getInstance();

        currentCursor = SCAN_POINTER_START;
        lastChosenKey = "";

        cbActionType.addItem(ACTION_SCAN);
        cbActionType.addItem(ACTION_GET);

        splitPane.setResizeWeight(SPLIT_PANE_WEIGHT);
        splitPane.setDividerSize(SPLIT_PANE_DIVIDER_SIZE);

        final Font valueFont = new Font(TABLE_HEADER_FONT, Font.PLAIN, TABLE_HEADER_FONT_SIZE);
        tblInnerValue.getTableHeader().setFont(valueFont);
        txtStringValue.setFont(valueFont);
        final DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer) tblInnerValue.getTableHeader()
                .getDefaultRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        pnlInnerValue.setBackground(lstKey.getBackground());

        progressBar.setIndeterminate(true);

        cbDatabase.addActionListener(event -> {
            if (Objects.equals(cbActionType.getSelectedItem(), ACTION_GET)) {
                return;
            }
            RedisCacheExplorer.this.setWidgetEnableStatus(false);
            txtKeyPattern.setText(DEFAULT_SCAN_PATTERN);
            RedisCacheExplorer.this.onDataBaseSelect();
        });

        lstKey.addListSelectionListener(event -> {
            final String selectedKey = lstKey.getSelectedValue();
            if (selectedKey == null || selectedKey.equals(lastChosenKey)) {
                return;
            }
            RedisCacheExplorer.this.setWidgetEnableStatus(false);
            lastChosenKey = selectedKey;
            manager.runOnPooledThread(() -> {
                final Pair<String, ArrayList<String[]>> data = getValueByKey(this.jedis, selectedKey);
                manager.runLater(() -> RedisCacheExplorer.this.showContent(data));
            });
        });

        btnSearch.addActionListener(event -> RedisCacheExplorer.this.onBtnSearchClick());

        btnScanMore.addActionListener(event -> {
            RedisCacheExplorer.this.setWidgetEnableStatus(false);
            manager.runOnPooledThread(() -> {
                final ScanResult<String> r = this.jedis.scan(currentCursor, new ScanParams().match(txtKeyPattern.getText()).count(DEFAULT_KEY_COUNT));
                manager.runLater(() -> RedisCacheExplorer.this.showScanResult(r));
            });
        });

        txtKeyPattern.addActionListener(event -> onBtnSearchClick());

        cbActionType.addActionListener(event -> {
            final String selected = (String) cbActionType.getSelectedItem();
            if (Objects.equals(selected, ACTION_GET)) {
                btnScanMore.setEnabled(false);
            } else if (Objects.equals(selected, ACTION_SCAN)) {
                btnScanMore.setEnabled(true);
            }
        });

        manager.runOnPooledThread(() -> {
            this.jedis = this.redis.getJedisPool().getResource();
            final int num = getDbNumber(this.jedis);
            AzureTaskManager.getInstance().runLater(() -> this.renderDbCombo(num));
        });
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

    public void dispose() {
        if (Objects.nonNull(this.jedis)) {
            this.jedis.close();
        }
    }

    public void renderDbCombo(int num) {
        for (int i = 0; i < num; i++) {
            cbDatabase.addItem(String.valueOf(i));
        }
        if (num > 0) {
            onDataBaseSelect();
        }
    }

    public void showScanResult(ScanResult<String> result) {
        lstKey.removeAll();
        final DefaultListModel<String> listModel = new DefaultListModel<>();
        final List<String> keys = result.getResult();
        Collections.sort(keys);
        for (final String key : keys) {
            listModel.addElement(key);
        }
        lstKey.setModel(listModel);
        currentCursor = result.getCursor();
        setWidgetEnableStatus(true);
        clearValueArea();
    }

    public void showContent(Pair<String, ArrayList<String[]>> val) {
        final String type = val.getKey();
        final ArrayList<String[]> value = val.getValue();
        lblTypeValue.setText(type);
        lblKeyValue.setText(lstKey.getSelectedValue());
        if (Objects.equals(type, "STRING")) {
            if (value.size() > 0 && value.get(0).length > 0) {
                txtStringValue.setText(value.get(0)[0]);
            }
            setValueCompositeVisible(false);
        } else {
            final String[] columnNames;
            switch (type) {
                case "LIST":
                    columnNames = LIST_TITLE;
                    break;
                case "SET":
                    columnNames = SET_TITLE;
                    break;
                case "ZSET":
                    columnNames = ZSET_TITLE;
                    break;
                case "HASH":
                    columnNames = HASH_TITLE;
                    break;
                default:
                    return;
            }
            String[][] data = new String[value.size()][columnNames.length];
            data = value.toArray(data);
            final ReadOnlyTableModel tableModel = new ReadOnlyTableModel(data, columnNames);
            setValueCompositeVisible(true);
            tblInnerValue.setModel(tableModel);
        }
        setWidgetEnableStatus(true);
    }

    public void updateKeyList() {
        final DefaultListModel<String> listModel = (DefaultListModel<String>) lstKey.getModel();
        listModel.removeAllElements();
        listModel.addElement(txtKeyPattern.getText());
        lstKey.setModel(listModel);
        lstKey.setSelectedIndex(0);
    }

    public void getKeyFail() {
        final DefaultListModel<String> listModel = (DefaultListModel<String>) lstKey.getModel();
        listModel.removeAllElements();
        setWidgetEnableStatus(true);
        clearValueArea();
    }

    private void onDataBaseSelect() {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runOnPooledThread(() -> {
            this.jedis.select(cbDatabase.getSelectedIndex());
            final ScanResult<String> r = this.jedis.scan(SCAN_POINTER_START, new ScanParams().match(DEFAULT_SCAN_PATTERN).count(DEFAULT_KEY_COUNT));
            manager.runLater(() -> RedisCacheExplorer.this.showScanResult(r));
        });
    }

    private void setWidgetEnableStatus(boolean enabled) {
        pnlProgressBar.setVisible(!enabled);
        cbDatabase.setEnabled(enabled);
        txtKeyPattern.setEnabled(enabled);
        btnSearch.setEnabled(enabled);
        lstKey.setEnabled(enabled);
        cbActionType.setEnabled(enabled);
        final String actionType = (String) cbActionType.getSelectedItem();
        btnScanMore.setEnabled(enabled && Objects.equals(actionType, ACTION_SCAN));
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
        final String actionType = (String) cbActionType.getSelectedItem();
        final String key = txtKeyPattern.getText();
        if (Objects.equals(actionType, ACTION_GET)) {
            final AzureTaskManager manager = AzureTaskManager.getInstance();
            manager.runOnPooledThread(() -> {
                final boolean isExist = this.jedis.exists(key);
                final Pair<String, ArrayList<String[]>> result = isExist ? getValueByKey(this.jedis, key) : null;
                this.updateKeyList();
                this.showContent(result);
            });
        } else if (Objects.equals(actionType, ACTION_SCAN)) {
            final AzureTaskManager manager = AzureTaskManager.getInstance();
            manager.runOnPooledThread(() -> {
                final ScanResult<String> r = this.jedis.scan(SCAN_POINTER_START, new ScanParams().match(key).count(DEFAULT_KEY_COUNT));
                manager.runLater(() -> RedisCacheExplorer.this.showScanResult(r));
            });
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

    private static int getDbNumber(Jedis jedis) {
        try {
            final List<String> dbs = jedis.configGet("databases");
            if (dbs.size() > 0) {
                return Integer.parseInt(dbs.get(1));
            }
            return DEFAULT_REDIS_DB_NUMBER;
        } catch (final JedisException e) {
            // Use binary search to determine the number of database the Redis has.
            int start = 0, end = MAX_DATABASE_NUMBER, mid;
            while (start < end) {
                mid = start + (end - start) / 2;
                if (canConnect(jedis, mid)) {
                    start = mid + 1;
                } else {
                    end = mid;
                }
            }
            return start;
        }
    }

    private static boolean canConnect(Jedis jedis, int index) {
        try {
            jedis.select(index);
            return true;
        } catch (final JedisException ex) {
            return false;
        }
    }

    @Nullable
    private static Pair<String, ArrayList<String[]>> getValueByKey(Jedis jedis, String key) {
        final String type = jedis.type(key).toUpperCase();
        final ArrayList<String[]> columnData = new ArrayList<>();
        switch (type) {
            case "STRING":
                final String stringVal = jedis.get(key);
                columnData.add(new String[]{stringVal});
                return Pair.of("STRING", columnData);
            case "LIST":
                final long listLength = jedis.llen(key);
                final List<String> listVal = jedis.lrange(key, DEFAULT_RANGE_START,
                        listLength < DEFAULT_VAL_COUNT ? listLength : DEFAULT_VAL_COUNT);
                for (int i = 0; i < listVal.size(); i++) {
                    columnData.add(new String[]{String.valueOf(i + 1), listVal.get(i)});
                }
                return Pair.of("LIST", columnData);
            case "SET":
                final ScanResult<String> setVal = jedis.sscan(key, SCAN_POINTER_START, new ScanParams().count(DEFAULT_VAL_COUNT));
                for (final String row : setVal.getResult()) {
                    columnData.add(new String[]{row});
                }
                return Pair.of("SET", columnData);
            case "ZSET":
                final long zsetLength = jedis.zcard(key);
                final Set<Tuple> zsetVal = jedis.zrangeWithScores(key, DEFAULT_RANGE_START,
                        zsetLength < DEFAULT_VAL_COUNT ? zsetLength : DEFAULT_VAL_COUNT);
                for (final Tuple tuple : zsetVal) {
                    columnData.add(new String[]{String.valueOf(tuple.getScore()), tuple.getElement()});
                }
                return Pair.of("ZSET", columnData);
            case "HASH":
                final ScanResult<Map.Entry<String, String>> hashVal = jedis.hscan(key, SCAN_POINTER_START, new ScanParams().count(DEFAULT_VAL_COUNT));
                for (final Map.Entry<String, String> hash : hashVal.getResult()) {
                    columnData.add(new String[]{hash.getKey(), hash.getValue()});
                }
                return Pair.of("HASH", columnData);
            default:
                return null;
        }
    }
}
