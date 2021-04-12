/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.rest;


import com.microsoft.azuretools.Constants;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by vlashch on 8/29/16.
 */
public abstract class RestHelperBase {

    private IRequestFactory requestFactory = null;
    protected IRequestFactory getRequestFactory() {
        return requestFactory;
    }
    protected void setRequestFactory(IRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    protected static String action(IRequestFactory factory, String verb, String request, String params, String body) throws IOException {
        String urlSrtring = (params == null)
                ? String.format(factory.getUrlPatternParamless(), request)
                : String.format(factory.getUrlPattern(), request, params);

        URL url = new URL(urlSrtring);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.addRequestProperty("User-Agent", "shch");
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("Authorization", "Bearer " + factory.getAccessToken());

        conn.setRequestMethod(verb);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setReadTimeout(Constants.connection_read_timeout_ms);

        if(body != null) {
            conn.addRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream output = conn.getOutputStream();
            try {
                output.write(body.getBytes());
            } finally {
                output.close();
            }
        }

        int statusCode = conn.getResponseCode();
        if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            InputStream errorStream = null;
            InputStreamReader errorReader = null;
            StringBuilder err = new StringBuilder();
            try {
                errorStream = conn.getErrorStream();
                errorReader = new InputStreamReader(errorStream);

                int data;
                while((data = errorReader.read()) != -1) {
                    err.append((char)data);
                }
            } finally {
                if(errorStream != null) {
                    errorStream.close();
                }
            }
            String errString = err.toString();
            // sometimes error stream starts wiht unacceptable symbols and jackson json fails to parse it.
            int start = errString.indexOf("{");
            if (start != 0) {
                errString = errString.substring(start);
            }
            System.out.println("Response: " + errString);
            throw factory.newAzureException(errString);
        }

        InputStream resposeBodyStream = null;

        try {
            resposeBodyStream = conn.getInputStream();
            if(resposeBodyStream == null) return null;

            java.io.BufferedReader br = new java.io.BufferedReader(new InputStreamReader(resposeBodyStream));
            StringBuilder resposeBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                resposeBody.append(line);
            }
            System.out.println("Response: " + resposeBody.toString());

            return resposeBody.length() == 0 ? null : resposeBody.toString();

        } finally {
            resposeBodyStream.close();
        }
    }

    public String doGet(String request, String params) throws IOException {
        return action(getRequestFactory(), "GET", request, params, null);
    }

    public String doPost(String request, String params, String body) throws IOException {
        return action(getRequestFactory(), "POST", request, params, body);
    }

    public String doPut(String request, String params, String body) throws IOException {
        return action(getRequestFactory(), "PUT", request, params, body);
    }

    public String doDelete(String request, String params, String body) throws IOException {
        return action(getRequestFactory(), "DELETE", request, params, body);
    }
}
