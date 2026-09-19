/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service;

import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertSame;

public class JavaVersionNotificationServiceTest {

    @Test
    public void prioritizesUnsupportedJdkThenCveThenOtherUpgrades() {
        final JavaUpgradeIssue dependencyIssue = issue(
            "spring-boot",
            JavaUpgradeIssue.UpgradeReason.END_OF_LIFE
        );
        final JavaUpgradeIssue cveIssue = issue(
            "vulnerable-dependency",
            JavaUpgradeIssue.UpgradeReason.CVE
        );
        final JavaUpgradeIssue jdkIssue = issue(
            "jdk",
            JavaUpgradeIssue.UpgradeReason.JRE_TOO_OLD
        );

        assertSame(
            jdkIssue,
            JavaVersionNotificationService.selectNotificationIssue(
                List.of(dependencyIssue, cveIssue, jdkIssue)
            )
        );
        assertSame(
            cveIssue,
            JavaVersionNotificationService.selectNotificationIssue(
                List.of(dependencyIssue, cveIssue)
            )
        );
        assertSame(
            dependencyIssue,
            JavaVersionNotificationService.selectNotificationIssue(List.of(dependencyIssue))
        );
    }

    private static JavaUpgradeIssue issue(
        String packageId,
        JavaUpgradeIssue.UpgradeReason reason
    ) {
        return JavaUpgradeIssue.builder()
            .packageId(packageId)
            .packageDisplayName(packageId)
            .upgradeReason(reason)
            .severity(JavaUpgradeIssue.Severity.WARNING)
            .message(packageId)
            .build();
    }
}
