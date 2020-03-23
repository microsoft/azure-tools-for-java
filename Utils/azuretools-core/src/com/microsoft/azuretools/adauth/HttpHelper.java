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

package com.microsoft.azuretools.adauth;

import com.microsoft.azuretools.Constants;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

class HttpHelper {
    final static Logger log = Logger.getLogger(HttpHelper.class.getName());

    public static <T> T sendPostRequestAndDeserializeJsonResponse(final String uri,
            final Map<String, String> requestParameters, final CallState callState, final Class<T> cls)
            throws IOException {
        log.log(Level.FINEST, "sendPostRequestAndDeserializeJsonResponseAsync...");
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        addCorrelationIdToRequestHeader(connection, callState);
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded; charset=" + StandardCharsets.UTF_8.name());
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setReadTimeout(Constants.connection_read_timeout_ms);

        byte[] requestData = UriUtils.toQueryString(requestParameters).getBytes(StandardCharsets.UTF_8);
        OutputStream output = connection.getOutputStream();
        output.write(requestData);
        output.close();

        int statusCode = connection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            InputStream errorStream = null;
            InputStreamReader errorReader = null;
            StringBuilder err = new StringBuilder();
            try {
                errorStream = connection.getErrorStream();
                errorReader = new InputStreamReader(errorStream);

                int data;
                while ((data = errorReader.read()) != -1) {
                    err.append((char) data);
                }
            } finally {
                if (errorStream != null) {
                    errorStream.close();
                }
                if (errorReader != null) {
                    errorReader.close();
                }
            }
            String message = "AD Auth token endpoint returned HTTP status code " + Integer.toString(statusCode)
                    + ". Error info: " + err.toString();
            log.log(Level.SEVERE, message);

            TokenResponse r = JsonHelper.deserialize(TokenResponse.class, err.toString());
            if (r.error.equals(AuthError.InvalidGrant)) {
                throw new AuthException(AuthError.InvalidGrant, message);
            } else if (r.error.equals(AuthError.InteractionRequired)) {
                throw new AuthException(AuthError.InteractionRequired, message);
            } else {
                throw new IOException(message);
            }
        }

        verifyCorrelationIdInReponseHeader(connection, callState);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        // parse the JSON
        String response = sb.toString();
        // String response =
        // "{\"token_type\":\"Bearer\",\"scope\":\"user_impersonation\",\"expires_in\":\"3599\",\"expires_on\":\"1460677695\",\"not_before\":\"1460673795\",\"resource\":\"https://management.core.windows.net/\",\"pwd_exp\":\"1036800\",\"pwd_url\":\"https://sspm.microsoft.com\",\"access_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik1uQ19WWmNBVGZNNXBPWWlKSE1iYTlnb0VLWSIsImtpZCI6Ik1uQ19WWmNBVGZNNXBPWWlKSE1iYTlnb0VLWSJ9.eyJhdWQiOiJodHRwczovL21hbmFnZW1lbnQuY29yZS53aW5kb3dzLm5ldC8iLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwiaWF0IjoxNDYwNjczNzk1LCJuYmYiOjE0NjA2NzM3OTUsImV4cCI6MTQ2MDY3NzY5NSwiX2NsYWltX25hbWVzIjp7Imdyb3VwcyI6InNyYzEifSwiX2NsYWltX3NvdXJjZXMiOnsic3JjMSI6eyJlbmRwb2ludCI6Imh0dHBzOi8vZ3JhcGgud2luZG93cy5uZXQvNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3L3VzZXJzL2I5ZDdkYTkwLTJkMjUtNDU3NS05YjI2LTJkODM1Y2MzZmEzOC9nZXRNZW1iZXJPYmplY3RzIn19LCJhY3IiOiIxIiwiYW1yIjpbInB3ZCIsIm1mYSJdLCJhcHBpZCI6IjYxZDY1ZjVhLTZlM2ItNDY4Yi1hZjczLWEwMzNmNTA5OGM1YyIsImFwcGlkYWNyIjoiMCIsImZhbWlseV9uYW1lIjoiU2hjaGVyYmFrb3YiLCJnaXZlbl9uYW1lIjoiVmxhZGltaXIiLCJpbl9jb3JwIjoidHJ1ZSIsImlwYWRkciI6IjEzMS4xMDcuMTc0LjEzMCIsIm5hbWUiOiJWbGFkaW1pciBTaGNoZXJiYWtvdiIsIm9pZCI6ImI5ZDdkYTkwLTJkMjUtNDU3NS05YjI2LTJkODM1Y2MzZmEzOCIsIm9ucHJlbV9zaWQiOiJTLTEtNS0yMS0yMTI3NTIxMTg0LTE2MDQwMTI5MjAtMTg4NzkyNzUyNy0xNjQxNzk4MSIsInB1aWQiOiIxMDAzMDAwMDkyRjAzNjBDIiwic2NwIjoidXNlcl9pbXBlcnNvbmF0aW9uIiwic3ViIjoiQzI4cl9PdnlfT3U5UXZmZnpRdDRZcHlZUFJJRng2cW9GTlczOFptMHhFayIsInRpZCI6IjcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0NyIsInVuaXF1ZV9uYW1lIjoidmxhc2hjaEBtaWNyb3NvZnQuY29tIiwidXBuIjoidmxhc2hjaEBtaWNyb3NvZnQuY29tIiwidmVyIjoiMS4wIn0.KZdeETx2oKTyPruXxjXOJ19p385P3I97LMSw9TGs_njSdihn4sXBcPFlwMEaENvCXE1mqxi1Y0cl_5RoRpd2puiqda7TY-ByecArdfYfWg5GQCoVljHG4M6MLIoI-xQFp4Xt65LtpsPYej3PC-ZPWjpA3KiVRuBvpAkCRW2gtQE0E28_OlF0XRqCuuHHXvuFOlRRQQPr0vE4eD115zmu425NL_nZYJ0A4K3E6I1FAuDHLHish0tmdAJy6p-Z6sRqYJyORvhFwD25TA6V7gwnBEqGHb_bKjpKunbVsUbSFrXyFe87l3I037bdeHRP2HRHLXrJmlw0t2efVmBOK4xIIg\",\"refresh_token\":\"AAABAAAAiL9Kn2Z27UubvWFPbm0gLQMpgEboPmAqHrfc4K0VjY8dlXYnROl1lQ8KLVyLroTHrI1Lm3bTAZvjiP7Cdd9K-rN1uXBzaVdHeWE8fpQ69M7j1lJOqQGwvfUeZz5kqs7aFyknVUGi1uyjulkTeMLBd-6wOUd9D0yb4_VED1P3P2O8bGMnVy748_Iodb58Q8BX-whw--A9QPgcpFlTaQhJ4AG8r-RTNeBSmlfaDeWqcn7EC9dS-WqNXKYkdo1cwto-YLEumDJjLgFgC91getsrNmc3KMus18JiJszrEDLwRUIs2XJWf0Jrqq8jyXjpYN7x5IRiZO5hPgsnq5DAGQhrMPd9YOYotW--AtlmDHbtTm3QOSDKDpZLyYzUifErMmzfEpyc-n5wK7B4cqFrI-hg7Oi2V0mR62LFFUQjuEUU-NiN85Js_xlDS1PWCz-m54XyB7ppg3gXudxpGDeSzcnSZ9GlezYl_b5HLKbj53PndhqWHjmlRTBSy1IeOZDMuXVlaOFISWK56L7PKSAGQtqBiqXqvN0u8ZVpDrkkg_PUlNpAm-tuMS5yUk42WQ00gOb4aKSdaSGahaS_H25XZREs_9ameqjx3rfyW_oD-qqyb3rUE8QnlOpbK-QarpfdUlFhBmpXAWHbBgNTAPgUvdn8jRmYD2Fh2cAvN6ZbqyE3garRulSjReFCCVAB4esIchZsGsGpN6eUgy4l0s_fCdO_wV6RJVRLA7mFGAJOd3lpPb8V_heeLCUxJVkVKavfjoCb6aqqkhvbsLer9UG9fwGVP7bDm4IgKc-4DANxGqcQNX3vPXNhQobMerUELHt4NvOr4b-IRxzK2k7nWr5Gj3OEGA3GX6cPrgCUxaAmGOrXb0sdPcfszjZJprcTXD_t4j08FLe8PFAC7jNza2fiTI5RpSAA\",\"id_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiI2MWQ2NWY1YS02ZTNiLTQ2OGItYWY3My1hMDMzZjUwOThjNWMiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwiaWF0IjoxNDYwNjczNzk1LCJuYmYiOjE0NjA2NzM3OTUsImV4cCI6MTQ2MDY3NzY5NSwiYW1yIjpbInB3ZCIsIm1mYSJdLCJmYW1pbHlfbmFtZSI6IlNoY2hlcmJha292IiwiZ2l2ZW5fbmFtZSI6IlZsYWRpbWlyIiwiaW5fY29ycCI6InRydWUiLCJpcGFkZHIiOiIxMzEuMTA3LjE3NC4xMzAiLCJuYW1lIjoiVmxhZGltaXIgU2hjaGVyYmFrb3YiLCJvaWQiOiJiOWQ3ZGE5MC0yZDI1LTQ1NzUtOWIyNi0yZDgzNWNjM2ZhMzgiLCJvbnByZW1fc2lkIjoiUy0xLTUtMjEtMjEyNzUyMTE4NC0xNjA0MDEyOTIwLTE4ODc5Mjc1MjctMTY0MTc5ODEiLCJwd2RfZXhwIjoiMTAzNjgwMCIsInB3ZF91cmwiOiJodHRwczovL3NzcG0ubWljcm9zb2Z0LmNvbSIsInN1YiI6Im40RWFFNmh3dTlSNUl6ckFfeGRiR2FleEN6Ny1rcVhNUmVPdGYyeWdMVWMiLCJ0aWQiOiI3MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDciLCJ1bmlxdWVfbmFtZSI6InZsYXNoY2hAbWljcm9zb2Z0LmNvbSIsInVwbiI6InZsYXNoY2hAbWljcm9zb2Z0LmNvbSIsInZlciI6IjEuMCJ9.\"}";
        log.log(Level.FINEST, "==> token response string: " + response);
        if (StringUtils.isNullOrWhiteSpace(response)) {
            throw new IOException("response is empty or null");
            //return cls.newInstance();
        }

        return deserializeResponse(response, cls);
    }

    public static <T> T deserializeResponse(String response, Class<T> cls) throws IOException {
        return JsonHelper.deserialize(cls, response);
    }

    public static void addCorrelationIdToRequestHeader(HttpURLConnection request, CallState callState) {
        if (callState == null || callState.correlationId == null) {
            return;
        }
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(OAuthHeader.CorrelationId, callState.correlationId.toString());
        headers.put(OAuthHeader.RequestCorrelationIdInResponse, "true");
        addHeadersToRequest(request, headers);
    }

    public static void addHeadersToRequest(HttpURLConnection request, Map<String, String> headers) {
        if (headers != null) {
            for (String key : headers.keySet()) {
                request.addRequestProperty(key, headers.get(key));
            }
        }
    }

    public static void verifyCorrelationIdInReponseHeader(HttpURLConnection response, CallState callState) {
        if (callState == null || callState.correlationId == null) {
            return;
        }

        Map<String, List<String>> headers = response.getHeaderFields();
        if (headers.containsKey(OAuthHeader.CorrelationId)) {
            String correlationIdHeader = (headers.get(OAuthHeader.CorrelationId)).get(0).trim();
            try {
                UUID correlationId = UUID.fromString(correlationIdHeader);
                if (!correlationId.equals(callState.correlationId)) {
                    log.log(Level.WARNING, "Returned correlation id '" + correlationId
                            + "' does not match the sent correlation id '" + callState.correlationId + "'");
                }
            } catch (IllegalArgumentException ex) {
                log.log(Level.WARNING, "Returned correlation id '" + correlationIdHeader + "' is not in GUID format.");
            }
        }
    }
}
