/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
