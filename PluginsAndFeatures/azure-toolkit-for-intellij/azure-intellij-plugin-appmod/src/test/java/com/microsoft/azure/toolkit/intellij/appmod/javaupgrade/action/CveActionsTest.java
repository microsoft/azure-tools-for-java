/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CveActionsTest {

    @Test
    public void exposesFrameworkUpgradeAsTopLevelProblemsViewAction() {
        final String description =
            "Your project uses Spring Boot 2.0.1.RELEASE. " +
                "Consider upgrading Spring Boot to 3.5, the latest LTS version, " +
                "for better performance and support";

        final UpgradeInProblemsViewAction.UpgradeDescription upgrade =
            UpgradeInProblemsViewAction.parseUpgradeDescription(description);

        assertNotNull(upgrade);
        assertEquals("Spring Boot", upgrade.packageDisplayName());
        assertEquals("2.0.1.RELEASE", upgrade.currentVersion());
        assertEquals("3.5", upgrade.suggestedVersion());
        assertEquals(
            "Upgrade Spring Boot with Copilot",
            UpgradeInProblemsViewAction.getActionName(upgrade)
        );
    }

    @Test
    public void doesNotExposeTopLevelUpgradeActionForCveProblems() {
        assertNull(UpgradeInProblemsViewAction.parseUpgradeDescription(
            "Security vulnerability CVE-2020-36518 detected in " +
                "maven:com.fasterxml.jackson.core:jackson-databind:2.9.4 Upgrade required"
        ));
    }
}
