/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.inspection;

import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaUpgradeIssuesInspectionTest {

    @Test
    public void createsJavaIssueFromPomJavaVersion() {
        final JavaUpgradeIssue issue = JavaUpgradeProblemLocator.createJavaVersionIssue("8");

        assertEquals("8", issue.getCurrentVersion());
        assertEquals(JavaUpgradeIssue.UpgradeReason.JRE_TOO_OLD, issue.getUpgradeReason());
    }

    @Test
    public void acceptsLegacyJavaVersionSyntax() {
        final JavaUpgradeIssue issue = JavaUpgradeProblemLocator.createJavaVersionIssue("1.8");

        assertEquals("8", issue.getCurrentVersion());
    }

    @Test
    public void ignoresSupportedOrPropertyReferenceVersions() {
        assertNull(JavaUpgradeProblemLocator.createJavaVersionIssue("21"));
        assertNull(JavaUpgradeProblemLocator.createJavaVersionIssue("${java.version}"));
    }

    @Test
    public void resolvesJavaVersionFromAnotherMavenProperty() {
        final XmlTag properties = mock(XmlTag.class);
        final XmlTag javaVersion = valueTag("${jdk.version}");
        final XmlTag jdkVersion = valueTag("8");
        when(properties.getName()).thenReturn("properties");
        when(properties.findFirstSubTag("jdk.version")).thenReturn(jdkVersion);
        when(javaVersion.getParentTag()).thenReturn(properties);

        final JavaUpgradeIssue issue =
            JavaUpgradeProblemLocator.createJavaVersionIssue(javaVersion);

        assertEquals("8", issue.getCurrentVersion());
    }

    @Test
    public void resolvesJavaVersionFromEffectiveParentProperties() {
        final XmlTag javaVersion = valueTag("${parent.java.version}");
        final Properties effectiveProperties = new Properties();
        effectiveProperties.setProperty("parent.java.version", "8");

        final JavaUpgradeIssue issue =
            JavaUpgradeProblemLocator.createJavaVersionIssue(javaVersion, effectiveProperties);

        assertEquals("8", issue.getCurrentVersion());
    }

    private static XmlTag valueTag(String value) {
        final XmlTag tag = mock(XmlTag.class);
        final XmlTagValue tagValue = mock(XmlTagValue.class);
        when(tag.getValue()).thenReturn(tagValue);
        when(tagValue.getText()).thenReturn(value);
        return tag;
    }
}
