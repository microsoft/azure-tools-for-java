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
package com.microsoft.intellij.ui.libraries;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by shenwe on 2017/6/8.
 */
public class AddLibraryUtilityTest {
    @Test
    public void testExtractArtifactName() throws Exception {
        String snapshotName = "ab-c-d-1.2.3-snapshot.jar";
        String nonVersionName = "ab.jar";
        String nonVersionNameWithDelimetor = "a-b-c.jar";
        String upperSizeName = "AB-C_D-1.2.3-snapshot.jar";

        assertEquals("ab-c-d", AddLibraryUtility.extractArtifactName(snapshotName));
        assertEquals("ab", AddLibraryUtility.extractArtifactName(nonVersionName));
        assertEquals("a-b-c", AddLibraryUtility.extractArtifactName(nonVersionNameWithDelimetor));
        assertEquals("AB-C_D", AddLibraryUtility.extractArtifactName(upperSizeName));
    }

}