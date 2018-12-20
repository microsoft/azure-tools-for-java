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
import com.intellij.util.net.ssl.CertificateUtil
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.intellij.util.PluginUtil
import org.apache.http.ssl.TrustStrategy
import sun.security.validator.ValidatorException
import java.security.cert.X509Certificate


object IdeaTrustStrategy : TrustStrategy, ILogger {
    @JvmStatic
    val UserRejectCAErrorMsg = "You have rejected the untrusted servers'certificate." +
            "Please click'OK',then accept the untrusted certificate if you want to link to this cluster." +
            "Or you can update the Livy URL to link to a different cluster."

    @JvmStatic
    val AcceptTitle = "Certificate Accepted"

    @JvmStatic
    val UserAcceptCAMsg = "You have successfully linked the cluster." +
            "The cluster certificate has been added into IntelliJ's Server Certificates store. \r\n" +
            "Please access IntelliJ's Server Certificates setting \r\n" +
            "https://www.jetbrains.com/help/idea/settings-tools-server-certificates.html \r\n" +
            "to manage the certificates trusted"

    var isRejected: Boolean = false
    var isAlreadyExist:Boolean = false

    override fun isTrusted(chain: Array<out X509Certificate>?, authType: String?): Boolean {
        return try {
            //check whether cert already exist by using alias
            val cert = chain!![0]
            val alias = CertificateUtil.getCommonName(cert)

            if (CertificateManager.getInstance().customTrustManager.containsCertificate(alias)) {
                log().warn("Already exists untrusted X509 certificates chain $chain for authentication type $authType")
                isAlreadyExist = true
                return true
            }

            //using sysmgr will throw validator exception then customMgr will try to add the cert
            //if user rejects,validator exception will rethrow
            try {
                CertificateManager.getInstance().trustManager.checkServerTrusted(chain, authType)
                return true
            } catch (exCauseByReject: ValidatorException) {
                log().warn("User rejects the untrusted X509 certificates chain $chain for authentication type $authType", exCauseByReject)
                isRejected = true
                throw exCauseByReject
            }
        } catch (err: Exception) {
            log().warn("Untrusted X509 certificates chain $chain for authentication type $authType", err)
            return false
        }
    }

    fun CheckAndResetRejectOption(): Boolean {
        val result = isRejected
        isRejected = false
        return result
    }

    fun CheckAndResetAlreadyExistValue(): Boolean {
        val result = isAlreadyExist
        isAlreadyExist = false
        return result
    }
}