/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azuretools.utils.Pair;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class SubmissionTableModelTest extends TestCase {
    private SubmissionTableModel tableModel;

    @Before
    public void setUp() throws Exception {
        tableModel = new SubmissionTableModel();

        //add three empty row for test
        tableModel.addEmptyRow();
        tableModel.addEmptyRow();
        tableModel.addEmptyRow();
    }

    @Test
    public void testSetValueAt() throws Exception {
        tableModel.setValueAt("test", 0, 1);
        assertEquals("test", tableModel.getValueAt(0, 1));

        tableModel.setValueAt("test2", 1, 0);
        assertEquals("test2", tableModel.getValueAt(1, 0));

        //set value to no-exist row.
        tableModel.setValueAt("test3", 4, 4);
        assertEquals(null, tableModel.getValueAt(4, 4));
    }

    @Test
    public void testAddRow() throws Exception {
        int rows = tableModel.getRowCount();
        tableModel.addRow("test1", "test2");
        assertEquals("test1", tableModel.getValueAt(rows - 1, 0));
    }

    @Test
    public void testGetJobConfigMap() throws Exception {
        List<Pair<String, String>> maps = tableModel.getJobConfigMap();
        assertEquals(maps.size(), 0);

        tableModel.setValueAt("test", 0, 0);
        maps = tableModel.getJobConfigMap();
        assertEquals(maps.size(), 1);
    }
}
