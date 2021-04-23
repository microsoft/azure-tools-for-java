package com.microsoft.azuretools.authmanage;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Data
public class AuthFile {
    private String tenant;
    private String client;
    private String key;
    private String certificate;
    private String certificatePassword;
    private String subscription;
    private String authURL;
    private String managementURI;
    private String filePath;

    private enum CredentialSettings {
        /** The subscription GUID. */
        SUBSCRIPTION_ID("subscription"),
        /** The tenant GUID or domain. */
        TENANT_ID("tenant"),
        /** The client id for the client application. */
        CLIENT_ID("client"),
        /** The client secret for the service principal. */
        CLIENT_KEY("key"),
        /** The client certificate for the service principal. */
        CLIENT_CERT("certificate"),
        /** The password for the client certificate for the service principal. */
        CLIENT_CERT_PASS("certificatePassword"),
        /** The management endpoint. */
        MANAGEMENT_URI("managementURI"),
        /** The base URL to the current Azure environment. */
        BASE_URL("baseURL"),
        /** The URL to Active Directory authentication. */
        AUTH_URL("authURL");

        /** The name of the key in the properties file. */
        private final String name;

        CredentialSettings(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static AuthFile fromFile(String credFilePath) throws IOException {
        if (StringUtils.isBlank(credFilePath)) {
            throw new IllegalArgumentException("This credential file path is blank");
        }
        final Path filePath = Paths.get(credFilePath);
        if (!filePath.toFile().isFile()) {
            throw new IllegalArgumentException("This credential file doesn't exist: " + filePath.toString());
        }
        String content = FileUtils.readFileToString(filePath.toFile(), "utf-8");
        //String content = FileUtils.(filePath.toFile()).trim();
        StringReader credentialsReader = new StringReader(content);
        Properties authSettings = new Properties();
        authSettings.load(credentialsReader);
        credentialsReader.close();
        AuthFile authFile = new AuthFile();
        authFile.setFilePath(credFilePath);
        authFile.setTenant(authSettings.getProperty(CredentialSettings.TENANT_ID.toString()));
        authFile.setKey(authSettings.getProperty(CredentialSettings.CLIENT_KEY.toString()));
        authFile.setClient(authSettings.getProperty(CredentialSettings.CLIENT_ID.toString()));
        authFile.setCertificate(authSettings.getProperty(CredentialSettings.CLIENT_CERT.toString()));
        authFile.setCertificatePassword(authSettings.getProperty(CredentialSettings.CLIENT_CERT_PASS.toString()));
        authFile.setSubscription(authSettings.getProperty(CredentialSettings.SUBSCRIPTION_ID.toString()));
        authFile.setManagementURI(authSettings.getProperty(CredentialSettings.MANAGEMENT_URI.toString()));
        authFile.setAuthURL(authSettings.getProperty(CredentialSettings.AUTH_URL.toString()));

        return authFile;
    }
}
