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
package com.microsoft.azuretools.azurecommons.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Class has utility methods to work with certificate files .cer and .pfx
 */
public class CerPfxUtil {
    /**
     * Method returns X509Certificate certificate object.
     *
     * @param certURL
     * @return
     */
    public static X509Certificate getCert(String certURL, String password) {
        InputStream inputStream = null;
        String url = certURL;
        X509Certificate cert = null;
        try {
            url = getCertificatePath(url);
            if (url == null || url.isEmpty())
                return null;
            File file = new File(url);
            if (!file.exists())
                return null;
            inputStream = new FileInputStream(url);
            if (url.endsWith(".cer") || url.endsWith(".CER")) {
                CertificateFactory certificateFactory = CertificateFactory
                        .getInstance("X.509");
                cert = (X509Certificate) certificateFactory
                        .generateCertificate(inputStream);
            } else if ((url.endsWith(".pfx") || url.endsWith(".PFX")) && (password != null)) {
                KeyStore ks = KeyStore.getInstance("PKCS12", "SunJSSE");
                ks.load(inputStream, password.toCharArray());
                String alias = ks.aliases().nextElement();
                cert = (X509Certificate) ks.getCertificate(alias);
            }
            return cert;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception e) {
                // Die silently,no need to throw any error
            }
        }
    }

    /**
     * This method resolves environment variables in path. Format of env
     * variables is ${env.VAR_NAME} e.g. ${env.JAVA_HOME}
     *
     * @param rawPath
     * @return
     */
    public static String getCertificatePath(String rawPath) {
        String certPath = null;
        String pathToUse = rawPath;
        if (pathToUse != null && pathToUse.length() > 0) {
            pathToUse = pathToUse.replace('\\', '/');
            String[] result = pathToUse.split("/");
            StringBuilder path = new StringBuilder();

            for (int x = 0; x < result.length; x++) {
                if (result[x].startsWith("${env")) {
                    String envValue = System.getenv(result[x].substring(
                            "${env.".length(), (result[x].length() - 1)));
                    path.append(envValue).append(File.separator);
                } else {
                    path.append(result[x]).append(File.separator);
                }
            }
            // Delete last trailing slash
            path = path.deleteCharAt(path.length() - 1);
            certPath = path.toString();
        }
        return certPath;
    }

    /**
     * Returns thunbprint associated with the certificate
     *
     * @param cert
     * @return thumbprint of the certificate. returns null if cert is null
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    public static String getThumbPrint(X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {

        if (cert == null) {
            return null;
        }

        MessageDigest mdigest = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        mdigest.update(der);
        byte[] digest = mdigest.digest();
        return hexify(digest).toUpperCase();
    }

    public static String getThumbPrint(String cerCertPath)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        return getThumbPrint(getCert(cerCertPath, null)).toUpperCase();
    }

    public static String hexify(byte bytes[]) {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }
        return buf.toString();
    }

    /**
     * Validate pfx password entered for pfx file.
     *
     * @return boolean
     */
    public static boolean validatePfxPwd(String path, String pwd) {
        boolean retval = true;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream input = new FileInputStream(new File(path));
            ks.load(input, pwd.toCharArray());
        } catch (Exception e) {
            retval = false;
        }
        return retval;
    }

    /**
     * This method creates a certificate for given password.
     *
     * @param certPath
     *            :- location of certificate file.
     * @param pfxPath
     *            :- location of pfx file.
     * @param alias
     *            :- User alias.
     * @param password
     *            :- alias password.
     * @return output of command.
     * @throws Exception
     * @throws IOException
     */
    public static void createCertificate(String certPath, String pfxPath,
            String alias, String password, String cnName, String jdkPath) throws Exception,
            IOException {

        String validityInDays = "3650";
        String keyAlg = "RSA";
        String keySize = "2048";
        String storeType = "pkcs12";
        String command = "keytool";
        if (jdkPath != null && !jdkPath.isEmpty()) {
            jdkPath = jdkPath.concat("\\bin");
        }
        if (new File(jdkPath).isDirectory()) {
            command = String.format("%s%s%s", jdkPath, File.separator, command);
        }

        // Create Pfx file
        String[] commandArgs = { command, "-genkey", "-alias", alias,
                "-keystore", pfxPath, "-storepass", password, "-validity",
                validityInDays, "-keyalg", keyAlg, "-keysize", keySize,
                "-storetype", storeType, "-dname", "CN="+cnName };
        Utils.cmdInvocation(commandArgs, false);

        // Create cer file i.e. extract public key from pfx
        File pfxFile = new File(pfxPath);
        if (pfxFile.exists()) {
            String[] certCommandArgs = { command, "-export", "-alias", alias,
                    "-storetype", storeType, "-keystore", pfxPath,
                    "-storepass", password, "-rfc", "-file", certPath };
            // output of keytool export command is going to error stream
            // although command is
            // executed successfully, hence ignoring error stream in this case
            Utils.cmdInvocation(certCommandArgs, true);

            // Check if file got created or not
            File cerFile = new File(pfxPath);
            if (!cerFile.exists()) {
                throw new IOException(
                        "Error occurred while creating certificate"
                                + certCommandArgs);
            }
        } else {
            throw new IOException("Error occurred while creating certificates"
                    + commandArgs);
        }
    }
}
