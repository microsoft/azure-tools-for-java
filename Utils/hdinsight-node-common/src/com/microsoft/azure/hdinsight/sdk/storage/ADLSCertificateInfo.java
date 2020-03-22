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
package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.cluster.ClusterIdentity;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class ADLSCertificateInfo {
    private final String resourceUri;
    private final String clientId;
    private final X509Certificate certificate;
    private final PrivateKey key;
    private final String aadTenantId;

    public ADLSCertificateInfo(ClusterIdentity clusterIdentity) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        this.resourceUri = clusterIdentity.getClusterIdentityresourceUri();
        this.clientId = clusterIdentity.getClusterIdentityapplicationId();
        this.aadTenantId = clusterIdentity.getClusterIdentityaadTenantId();

        // load pfx certificate from Base64 string
        byte[] certificateBytes = Base64.decodeBase64(clusterIdentity.getClusterIdentitycertificate());
        final String certificatePassword = clusterIdentity.getClusterIdentitycertificatePassword();
        KeyStore pkcs12Cert = KeyStore.getInstance("pkcs12");
        pkcs12Cert.load(new ByteArrayInputStream(certificateBytes), certificatePassword.toCharArray());
        // the pfx certificate has only one alias and it's a X509 certificate
        final String alias = pkcs12Cert.aliases().nextElement();
        this.key = (PrivateKey) pkcs12Cert.getKey(alias, certificatePassword.toCharArray());
        this.certificate = (X509Certificate) pkcs12Cert.getCertificate(alias);
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public String getClientId() {
        return clientId;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PrivateKey getKey() {
        return key;
    }

    public String getAadTenantId() {
        return aadTenantId;
    }
}
