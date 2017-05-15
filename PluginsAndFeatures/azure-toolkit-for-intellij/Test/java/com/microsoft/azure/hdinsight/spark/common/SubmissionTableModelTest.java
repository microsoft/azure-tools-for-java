package com.microsoft.azure.hdinsight.spark.common;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class SubmissionTableModelTest extends TestCase{
    private SubmissionTableModel tableModel;
    private String[] columns = new String[]{"col1", "col2", "col3"};

    @Before
    public void setUp() throws Exception {
        tableModel = new SubmissionTableModel(columns);

        //add three empty row for test
        tableModel.addEmptyRow();
        tableModel.addEmptyRow();
        tableModel.addEmptyRow();
    }

    @Test
    public void testSetValueAt() throws Exception {
        tableModel.setValueAt("test", 0, 1);
        assertEquals("test", tableModel.getValueAt(0,1));

        tableModel.setValueAt("test2", 1, 0);
        assertEquals("test2", tableModel.getValueAt(1,0));

        //set value to no-exist row.
        tableModel.setValueAt("test3", 4, 4);
        assertEquals(null, tableModel.getValueAt(4,4));
    }

    @Test
    public void testAddRow() throws Exception {
        int rows = tableModel.getRowCount();
        tableModel.addRow("test1", "test2");
        assertEquals("test1", tableModel.getValueAt(rows, 0));
    }

    @Test
    public void testGetJobConfigMap() throws Exception {
        Map<String,Object> maps = tableModel.getJobConfigMap();
        assertEquals(maps.size(), 0);

        tableModel.setValueAt("test", 0, 0);
        maps = tableModel.getJobConfigMap();
        assertEquals(maps.size(), 1);
    }
}