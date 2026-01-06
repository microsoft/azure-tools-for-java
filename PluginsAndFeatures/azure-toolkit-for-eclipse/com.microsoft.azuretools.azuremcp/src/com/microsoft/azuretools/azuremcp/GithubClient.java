package com.microsoft.azuretools.azuremcp;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.core.runtime.ILog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * The GithubClient class provides methods for interacting with the GitHub API. It includes operations
 * for retrieving release information, downloading files, and executing HTTP requests with retry logic.
 * This client is designed to handle transient errors by implementing exponential backoff and retrying
 * failed requests up to a maximum limit.
 *
 * This class manages an HTTP client internally and implements the Closeable interface to ensure proper
 * cleanup of resources.
 */
public class GithubClient implements Closeable {
	
    private static final ILog log = ILog.of(GithubClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final String AZURE_MCP_RELEASE_URL = "https://api.github.com/repos/microsoft/mcp/releases";
    private static final TypeReference<List<GithubRelease>> GITHUB_RELEASE_LIST_TYPE = new TypeReference<List<GithubRelease>>() {
    };

    private static final String AZURE_MCP_SERVER = "Azure.Mcp.Server";
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * Executes the given HTTP request with retry logic. The request will be retried a maximum
     * number of times specified by the constant MAX_RETRIES if failures occur. Exponential
     * backoff with jitter is applied between retries. If all attempts fail, an error telemetry
     * event is logged, and the method returns null.
     *
     * @param request the {@link HttpUriRequest} to be executed
     * @param handler a {@link Function} to process the {@link CloseableHttpResponse} and return a result
     * @return the result of the handler function applied to the HTTP response, or null if all retries fail
     */
    public <T> T executeWithRetry(HttpUriRequest request, Function<CloseableHttpResponse, T> handler) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try (final CloseableHttpResponse response = httpClient.execute(request)) {
                return handler.apply(response);
            } catch (final IOException ex) {
                attempt++;
                log.warn("Attempt " + attempt + " failed", ex);
                if (attempt == MAX_RETRIES) {
                    log.error("Max retries reached.", ex);
                    break;
                }
                final long jitter = (long) (Math.random() * 10);
                final long delay = (BASE_DELAY_MS * attempt) + jitter;
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the most recent Azure MCP release from the GitHub releases based on a predefined URL.
     * Filters the releases to find the latest one whose name starts with the specified Azure MCP server prefix.
     * Executes the request with retry logic in case of failures.
     *
     * @return the latest {@link GithubRelease} matching the Azure MCP server prefix, or null if no such release is found
     *         or an error occurs during the process.
     */
    public GithubRelease getLatestAzureMcpRelease() {
        final HttpUriRequest request = RequestBuilder.get().setUri(AZURE_MCP_RELEASE_URL).build();
        return executeWithRetry(request, response -> {
            try {
                final List<GithubRelease> releases = OBJECT_MAPPER.readValue(response.getEntity().getContent(), GITHUB_RELEASE_LIST_TYPE);
                return releases.stream()
                        .filter(release -> release.getName() != null && release.getName().startsWith(AZURE_MCP_SERVER))
                        .findFirst()
                        .orElse(null);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Downloads content from the specified URL and writes it to a given file.
     * This method uses retry logic to handle transient failures during the download process.
     *
     * @param downloadUrl the URL of the file to be downloaded
     * @param downloadFile the File object representing the destination file where the content will be written
     * @return true if the download and file write operation were successful, otherwise false
     */
    public boolean downloadToFile(String downloadUrl, File downloadFile) {
        final HttpUriRequest downloadRequest = RequestBuilder.get().setUri(downloadUrl).build();
        final Boolean result = executeWithRetry(downloadRequest, response -> {
            try (final FileOutputStream fos = new FileOutputStream(downloadFile)) {
                response.getEntity().getContent().transferTo(fos);
                return true;
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return result != null && result;
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
    }
}