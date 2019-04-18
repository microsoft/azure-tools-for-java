/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.sdk.storage.adlsgen2;

import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.SharedKeyHttpObservable;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import rx.Observable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class ADLSGen2FSOperation {
    private SharedKeyHttpObservable http;

    @NotNull
    private List<NameValuePair> createDirReqParams;

    @NotNull
    private List<NameValuePair> createFileReqParams;

    @NotNull
    private List<NameValuePair> appendReqParams;

    @NotNull
    private ADLSGen2ParamsBuilder flushReqParamsBuilder;

    public ADLSGen2FSOperation(@NotNull HttpObservable http) {
        this.http = (SharedKeyHttpObservable) http;
        this.createDirReqParams = new ADLSGen2ParamsBuilder()
                .setResource("directory")
                .build();

        this.createFileReqParams = new ADLSGen2ParamsBuilder()
                .setResource("file")
                .build();

        this.appendReqParams = new ADLSGen2ParamsBuilder()
                .setAction("append")
                .setPosition(0)
                .build();

        this.flushReqParamsBuilder = new ADLSGen2ParamsBuilder()
                .setAction("flush");
    }

    public Observable<Boolean> createDir(String dirpath) {
        HttpPut req = new HttpPut(dirpath);
        return http.executeReqAndCheckStatus(req, 201, this.createDirReqParams,
                http.setAuthorization(req, this.createDirReqParams).getHeaderList())
                .map(ignore -> true);
    }

    public Observable<Boolean> createFile(String filePath) {
        HttpPut req = new HttpPut(filePath);
        return http.executeReqAndCheckStatus(req, 201, this.createFileReqParams,
                http.setAuthorization(req, this.createFileReqParams).getHeaderList())
                .map(ignore -> true);
    }

    public Observable<Boolean> uploadData(String destFilePath, File src) {
        return appendData(destFilePath, src)
                .flatMap(len -> flushData(destFilePath, len));
    }

    private Observable<Long> appendData(String filePath, File src) {
        try {
            InputStreamEntity reqEntity = new InputStreamEntity(
                    new FileInputStream(src),
                    -1,
                    ContentType.APPLICATION_OCTET_STREAM);
            BufferedHttpEntity entity = new BufferedHttpEntity(reqEntity);
            long len = entity.getContentLength();

            HttpPatch req = new HttpPatch(filePath);
            req.setEntity(entity);

            // adls gen2 deployable needs set content-length to generate shared key
            // but httpclient auto adds this header and calculates length when executing
            // so remove this header after key generation otherwise header already exists exp happens
            return http.executeReqAndCheckStatus(req, 202, this.appendReqParams,
                    http.setContentType("application/octet-stream")
                            .setContentLength(String.valueOf(len))
                            .setAuthorization(req, this.appendReqParams)
                            .removeContentLength()
                            .getHeaderList())
                    .map(ignore -> len);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(new IllegalArgumentException("Can not find the aritifact"));
        } catch (IOException e) {
            throw new RuntimeException(new IllegalArgumentException("Can not read the aritfact"));
        }
    }

    private Observable<Boolean> flushData(String filePath, long flushLen) {
        HttpPatch req = new HttpPatch(filePath);
        List<NameValuePair> flushReqParams = this.flushReqParamsBuilder.setPosition(flushLen).build();

        return http.executeReqAndCheckStatus(req, 200, flushReqParams,
                http.setContentType("application/json")
                        .setContentLength("0")
                        .setAuthorization(req, flushReqParams)
                        .removeContentLength()
                        .getHeaderList())
                .map(ignore -> true);
    }
}
