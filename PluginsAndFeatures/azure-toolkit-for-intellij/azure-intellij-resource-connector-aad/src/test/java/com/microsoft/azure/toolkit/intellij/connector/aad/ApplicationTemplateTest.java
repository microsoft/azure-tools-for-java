package com.microsoft.azure.toolkit.intellij.connector.aad;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ApplicationTemplateTest {
    @Test
    public void applicationPropertiesTemplate() throws IOException {
        var template = new ApplicationTemplate("/code-templates/application.properties",
                "actual-tenant-id",
                "actual-client-id",
                "actual-client-secret",
                "actual-group-names");

        var replaced = template.content();
        Assert.assertTrue(replaced.contains("azure.activedirectory.tenant-id=actual-tenant-id"));
        Assert.assertTrue(replaced.contains("azure.activedirectory.client-id=actual-client-id"));
        Assert.assertTrue(replaced.contains("azure.activedirectory.client-secret=actual-client-secret"));
        Assert.assertTrue(replaced.contains("azure.activedirectory.user-group.allowed-group-names=actual-group-names"));
    }
}