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

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AzureManagerFactory;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.SubscriptionManagerPersist;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.azuretools.Constants.FILE_NAME_SUBSCRIPTIONS_DETAILS_SP;

public class ServicePrincipalAzureManager extends AzureManagerBase {
    private static final Logger LOGGER = Logger.getLogger(ServicePrincipalAzureManager.class.getName());

    private String defaultTenantId;
    private final File credFile;
    private ApplicationTokenCredentials credentials;
    private Environment environment = null;

    static {
        settings.setSubscriptionsDetailsFileName(FILE_NAME_SUBSCRIPTIONS_DETAILS_SP);
    }

    public static void cleanPersist() throws IOException {
        String subscriptionsDetailsFileName = settings.getSubscriptionsDetailsFileName();
        SubscriptionManagerPersist.deleteSubscriptions(subscriptionsDetailsFileName);
    }

    public ServicePrincipalAzureManager(String tid, String appId, String appKey) {
        this.credFile = null;
        this.credentials = new ApplicationTokenCredentials(appId, tid, appKey, null);
        this.init();
    }

    public ServicePrincipalAzureManager(File credFile) {
        this.credFile = credFile;
        this.init();
    }

    @Override
    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        String uri = getManagementURI();
        return this.credentials.getToken(uri);
    }

    @Override
    public String getCurrentUserId() throws IOException {
        return this.credentials.clientId();
    }

    @Override
    protected String getCurrentTenantId() throws IOException {
        return this.defaultTenantId;
    }

    @Override
    protected AzureTokenCredentials getCredentials(String tenantId) {
        return this.credentials;
    }

    protected boolean isSignedIn() {
        return Objects.nonNull(this.credentials) && Objects.nonNull(this.defaultTenantId);
    }

    @Override
    public String getManagementURI() throws IOException {
        // default to global cloud
        return this.credentials.environment() == null ?
                AzureEnvironment.AZURE.resourceManagerEndpoint() : this.credentials.environment().resourceManagerEndpoint();
    }

    private void init() {
        if (this.credentials == null && credFile != null) {
            try {
                this.credentials = ApplicationTokenCredentials.fromFile(credFile);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failed to init credential from specified file", e);
                this.credentials = null;
            }
        }
        if (Objects.nonNull(this.credentials)) {
            final Azure.Authenticated authenticated = Azure.configure()
                    .withInterceptor(new TelemetryInterceptor())
                    .withUserAgent(CommonSettings.USER_AGENT)
                    .authenticate(this.credentials);
            this.defaultTenantId = authenticated.tenantId();
            initEnv();
        }
    }

    private void initEnv() {
        if (this.environment != null) {
            return;
        }
        try {
            String managementURI = getManagementURI().toLowerCase();
            if (managementURI.endsWith("/")) {
                managementURI = managementURI.substring(0, managementURI.length() - 1);
            }

            if (AzureEnvironment.AZURE.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                this.environment = Environment.GLOBAL;
            } else if (AzureEnvironment.AZURE_CHINA.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                this.environment = Environment.CHINA;
            } else if (AzureEnvironment.AZURE_GERMANY.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                this.environment = Environment.GERMAN;
            } else if (AzureEnvironment.AZURE_US_GOVERNMENT.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                this.environment = Environment.US_GOVERNMENT;
            } else {
                this.environment = Environment.GLOBAL;
            }
        } catch (Exception e) {
            this.environment = Environment.GLOBAL;
        }
        CommonSettings.setUpEnvironment(this.environment);
    }

    public static class ServicePrincipalAzureManagerFactory implements AzureManagerFactory {
        @Override
        public AzureManager factory(final AuthMethodDetails authMethodDetails) {
            final String credFilePath = authMethodDetails.getCredFilePath();
            if (StringUtils.isBlank(credFilePath)) {
                throw new IllegalArgumentException("This credential file path is blank");
            }

            final Path filePath = Paths.get(credFilePath);
            if (!Files.exists(filePath)) {
                final INotification nw = CommonSettings.getUiFactory().getNotificationWindow();
                nw.deliver("Credential File Error", "File doesn't exist: " + filePath.toString());
                throw new IllegalArgumentException("This credential file doesn't exist: " + filePath.toString());
            }

            return new ServicePrincipalAzureManager(new File(credFilePath));
        }
    }
}
