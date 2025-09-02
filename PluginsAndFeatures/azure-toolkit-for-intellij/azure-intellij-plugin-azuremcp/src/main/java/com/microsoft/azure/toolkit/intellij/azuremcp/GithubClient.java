package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class GithubClient implements Closeable {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final String AZURE_MCP_RELEASE_URL = "https://aka.ms/azmcp/releases";
    private static final TypeReference<List<GithubRelease>> GITHUB_RELEASE_LIST_TYPE = new TypeReference<>() {
    };

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public GithubRelease getLatestAzureMcpRelease() {
        final HttpUriRequest request = RequestBuilder.get().setUri(AZURE_MCP_RELEASE_URL).build();
        try (final CloseableHttpResponse response = httpClient.execute(request)) {
            final List<GithubRelease> releases = OBJECT_MAPPER.readValue(response.getEntity().getContent(), GITHUB_RELEASE_LIST_TYPE);
            return releases.stream().findFirst().orElse(null);
        } catch (final IOException exception) {
            return null;
        }
    }

    public boolean downloadToFile(String downloadUrl, File downloadFile) {
        final HttpUriRequest downloadRequest = RequestBuilder.get().setUri(downloadUrl).build();
        try (final CloseableHttpResponse downloadResponse = httpClient.execute(downloadRequest);
             final FileOutputStream fos = new FileOutputStream(downloadFile)) {
            downloadResponse.getEntity().getContent().transferTo(fos);
            return true;
        } catch (final IOException exception) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
    }
}
