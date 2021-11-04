/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.hdinsight.jobs.framework

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.microsoft.azure.hdinsight.spark.jobs.JobViewHttpServer
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefFocusHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

class JobViewPanel(private val rootPath: String, private val clusterName: String) {
    // Set ENV `HDI_JOBVIEW_ROOTDIR=<Job View HTML source dir>` to enable JobView's HTML/JS/CSS development
    private val developingHtmlDir = System.getenv("HDI_JOBVIEW_ROOTDIR")

    private val QUERY_TEMPLATE = "?clusterName=%s&port=%s"
    private val panel = JPanel(BorderLayout())
    val browser: JBCefBrowser by lazy {
        val url = if (ApplicationManager.getApplication().isInternal && !developingHtmlDir.isNullOrBlank()) {
            // In plugin development mode and reading HTML from development source

            "${File(developingHtmlDir).toURI()}hdinsight/job/html/index.html"
        } else {
            "file:///$rootPath/hdinsight/job/html/index.html"
        }

        val queryString = String.format(QUERY_TEMPLATE, clusterName, JobViewHttpServer.getPort())
        val webUrl = url + queryString

        if (!JBCefApp.isSupported()) {
            throw RuntimeException(
                "JCEF is not enabled in current IDE. Please follow the instruction to enable JCEF first. " +
                        "https://youtrack.jetbrains.com/issue/IDEA-231833#focus=streamItem-27-3993099.0-0"
            )
        }

        JBCefBrowser(webUrl)
    }

    val component: JComponent by lazy {
        panel.add(browser.component)

        ApplicationManager.getApplication().messageBus.connect().subscribe(LafManagerListener.TOPIC,
            LafManagerListener { browser.cefBrowser.updateTheme() })

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                browser?.updateTheme()
            }
        }, browser.cefBrowser)

        browser.jbCefClient.addFocusHandler(object : CefFocusHandlerAdapter() {
            override fun onTakeFocus(onBrowser: CefBrowser?, next: Boolean) {
                onBrowser?.setFocus(true)
            }
        }, browser.cefBrowser)

        panel
    }
}

fun CefBrowser.updateTheme() {
    val isDarkTheme = EditorColorsManager.getInstance().isDarkEditor
    val themeName = LafManager.getInstance().currentLookAndFeel.name

    val themeMode = if (themeName.toLowerCase() == "high contrast") {
        "highcontrast"
    } else if (isDarkTheme) {
        "dark"
    } else {
        "light"
    }

    this.executeJavaScript("setTheme('$themeMode')", "about:blank", 0)
}
