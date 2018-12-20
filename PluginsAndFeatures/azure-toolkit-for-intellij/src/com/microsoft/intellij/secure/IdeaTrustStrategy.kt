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
import com.microsoft.intellij.util.PluginUtil
import org.apache.http.ssl.TrustStrategy
import sun.security.validator.ValidatorException
import java.security.cert.X509Certificate

object IdeaTrustStrategy : TrustStrategy, ILogger {
    private const val AcceptTitle = "Certificate Accepted"
    private const val helpLink = "https://www.jetbrains.com/help/idea/settings-tools-server-certificates.html"

    @JvmStatic
    val UserAcceptCAMsg =
            "<html><body>" +
                    "<br>You have successfully linked the cluster.The cluster certificate has been added into</br>" +
                    "<br>IntelliJ's Server Certificates store.Please access IntelliJ's Server Certificates setting</br>" +
                    "<br><a href='$helpLink'>$helpLink</a></br>" +
                    "<br>to manage the certificates trusted</br>" +
                    "</body></html>"

    override fun isTrusted(chain: Array<out X509Certificate>?, authType: String?): Boolean {
        return try {
            //there exists three scenarios 1.non aris cluster 2.linked aris cluster 3.adding a aris cluster
            //first check without askUser ,this works under case1 and case3
            val sysMgr = CertificateManager.getInstance().trustManager
            try {
                sysMgr.checkServerTrusted(chain, authType, true, false)
                return true
            } catch (err: Exception) {
                log().warn("First check untrusted X509 certificates chain $chain for authentication type $authType", err)
            }

            //as for case3, sysmgr always throws ValidatorException exception then customMgr popups ca accept or not dialog
            //if user accepts,return true
            //if user rejects,ValidatorException will rethrow then return false
            //otherwise return false when exp happens during mgr checking cert
            try {
                CertificateManager.getInstance().trustManager.checkServerTrusted(chain, authType)
            } catch (exCauseByReject: ValidatorException) {
                log().warn("User rejects the untrusted X509 certificates chain $chain for authentication type $authType", exCauseByReject)
                return false
            }

            val showAcceptDialog: () -> Unit = {
                PluginUtil.displayInfoDialog(AcceptTitle, UserAcceptCAMsg)
            }

            ApplicationManager.getApplication().invokeLater(
                    showAcceptDialog
                    , ModalityState.any())

            return true
        } catch (err: Exception) {
            log().warn("Recheck untrusted X509 certificates chain $chain for authentication type $authType", err)
            return false
        }
    }
}