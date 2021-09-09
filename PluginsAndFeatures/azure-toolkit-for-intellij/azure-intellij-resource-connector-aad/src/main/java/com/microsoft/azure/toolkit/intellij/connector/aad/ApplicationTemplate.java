package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.util.io.StreamUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * Template for a single code file.
 * It manages placeholders {{...}} in the code template's content.
 */
class ApplicationTemplate {
    @NotNull
    private final String resourcePath;

    // placeholder values
    @NotNull
    private final String tenantID;
    @NotNull
    private final String clientID;
    @NotNull
    private final String clientSecret;
    @NotNull
    private final String groupNames;

    ApplicationTemplate(@NotNull String resourcePath,
                        @NotNull String tenantID,
                        @NotNull String clientID,
                        @NotNull String clientSecret,
                        @NotNull String groupNames) {
        this.resourcePath = resourcePath;
        this.tenantID = tenantID;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.groupNames = groupNames;
    }

    /**
     * @return The template's content with placeholders replaced with actual values.
     */
    String content() throws IOException {
        try (var stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("unable to locate code template " + resourcePath);
            }

            var code = StreamUtil.readText(new InputStreamReader(stream));
            code = code.replaceAll(Pattern.quote("{{tenantID}}"), tenantID);
            code = code.replaceAll(Pattern.quote("{{clientID}}"), clientID);
            code = code.replaceAll(Pattern.quote("{{clientSecret}}"), clientSecret);
            code = code.replaceAll(Pattern.quote("{{groupNames}}"), groupNames);

            return code;
        }
    }
}
