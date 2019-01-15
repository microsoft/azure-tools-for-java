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

package com.microsoft.intellij.secure

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.util.net.ssl.CertificateManager
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable
import com.microsoft.intellij.util.PluginUtil
import org.apache.http.ssl.TrustStrategy
import sun.security.validator.ValidatorException
import java.security.cert.X509Certificate

object IdeaTrustStrategy : TrustStrategy, ILogger {
    private const val AcceptTitle = "Certificate Accepted"
    private const val helpLink = "https://www.jetbrains.com/help/idea/settings-tools-server-certificates.html"

    @JvmStatic
    val UserAcceptCAMsg = """<html><body>
            <br>The cluster certificate has been added into IntelliJ's Server Certificates store.</br>
            <br>Please access IntelliJ's Server Certificates setting</br>
            <br><a href='$helpLink'>$helpLink</a></br>
            <br>to manage the certificates trusted</br>
            </body></html>
             """.trimIndent()


    override fun isTrusted(chain: Array<out X509Certificate>?, authType: String?): Boolean {
        try {
            // There are four scenarios
            // 1. The cluster with proper certificate.
            // 2. The cluster with accepted certificate.
            // 3. The user forces bypassing all SSL certificate verification
            // 4. The user rejects a certificate when linking a cluster(Aris), not bypass SSL certificate verification

            // Check the certificate without asking users for case 1 & 2
            val sysMgr = CertificateManager.getInstance().trustManager
            try {
                sysMgr.checkServerTrusted(chain, authType, true, false)
                return true
            } catch (err: Exception) {
                log().warn("First check untrusted X509 certificates chain $chain for authentication type $authType", err)
            }

            // Check the global bypass SSL certificate validation option for case 3
            if (HttpObservable.isSSLCertificateValidationDisabled()) {
                val isAdded = CertificateManager.getInstance().customTrustManager.addCertificate(chain!![0])
                if (!isAdded) {
                    log().warn("Fail to add untrusted X509 certificates chain $chain for authentication type $authType")
                    return false
                }

                return true
            }

            // Check the certificate with prompt dialog for untrusted 
            try {
                CertificateManager.getInstance().trustManager.checkServerTrusted(chain, authType)
            } catch (exCauseByReject: ValidatorException) {
                log().warn("User rejects the untrusted X509 certificates chain $chain for authentication type $authType", exCauseByReject)
                return false
            }

            ApplicationManager.getApplication().invokeLater({
                PluginUtil.displayInfoDialog(AcceptTitle, UserAcceptCAMsg)
            }, ModalityState.any())

            return true
        } catch (err: Exception) {
            log().warn("Recheck untrusted X509 certificates chain $chain for authentication type $authType", err)
            return false
        }
    }
}