/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertTrue;

public class JavaUpgradeIssuesCacheTest {

    @Test
    public void serializesFullProjectScans() throws NoSuchMethodException {
        assertTrue(Modifier.isSynchronized(
            JavaUpgradeIssuesCache.class.getMethod("refresh").getModifiers()
        ));
    }
}
