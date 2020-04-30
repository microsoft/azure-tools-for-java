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

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azuretools.utils.Pair;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class SubmissionTableModelTest extends TestCase{
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
        List<Pair<String, String>> maps = tableModel.getJobConfigMap();
        assertEquals(maps.size(), 0);

        tableModel.setValueAt("test", 0, 0);
        maps = tableModel.getJobConfigMap();
        assertEquals(maps.size(), 1);
    }
}
