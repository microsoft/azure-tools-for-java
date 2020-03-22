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
package com.microsoft.intellij.ui.libraries;

import com.microsoft.intellij.AzurePlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AILibraryHandler {
    Document webXMLDoc = null;
    String webXMLPath = "";

    Document aiConfXMLDoc = null;
    String aiConfXMLPath = "";

    private static final String INSTRUMENTATION_KEY_NODE_NAME = "InstrumentationKey";
    private static final String DISABLE_TELEMETRY_NODE_NAME = "DisableTelemetry";

    public AILibraryHandler() {
    }

    public Document getWebXMLDoc() {
        return webXMLDoc;
    }

    public Document getAIConfXMLDoc() {
        return aiConfXMLDoc;
    }

    public AILibraryHandler(String webXmlPath, String aiConfXMLPath)
            throws Exception {
        try {
            // parse web.xml
            if (webXmlPath != null) {
                parseWebXmlPath(webXmlPath);
            }

            // parse ApplicationInsights.xml
            if (aiConfXMLPath != null) {
                parseAIConfXmlPath(aiConfXMLPath);
            }
        } catch (Exception e) {
            AzurePlugin.log(e.getMessage(), e);
            throw new Exception(message("aiParseErrMsg"));
        }
    }

    public void parseWebXmlPath(String webXmlPath) throws Exception {
        if (webXmlPath != null) {
            this.webXMLPath = webXmlPath;
            File xmlFile = new File(webXmlPath);
            if (xmlFile.exists()) {
                webXMLDoc = ParserXMLUtility.parseXMLFile(webXmlPath, message("aiParseErrMsg"));
            } else {
                throw new Exception(String.format("%s%s", webXmlPath, message("fileErrMsg")));
            }
        }
    }

    public boolean isAIWebFilterConfigured() throws Exception {
        if (webXMLDoc == null) {
            return false;
        }

        String exprFilter = message("aiExprConst");
        XPath xpath = XPathFactory.newInstance().newXPath();
        Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc, XPathConstants.NODE);
        if (eleFilter != null) {
            return true;
        }
        return false;
    }

    public void save() throws IOException, Exception {
        if (webXMLDoc != null) {
            ParserXMLUtility.saveXMLFile(webXMLPath, webXMLDoc);
        }

        if (aiConfXMLDoc != null) {
            ParserXMLUtility.saveXMLFile(aiConfXMLPath, aiConfXMLDoc);
        }
    }

    public void parseAIConfXmlPath(String aiConfXMLPath) throws Exception {
        if (aiConfXMLPath != null) {
            this.aiConfXMLPath = aiConfXMLPath;
            File xmlFile = new File(aiConfXMLPath);
            if (xmlFile.exists()) {
                aiConfXMLDoc = ParserXMLUtility.parseXMLFile(aiConfXMLPath, message("aiParseErrMsg"));
            } else {
                throw new Exception(String.format("%s%s", aiConfXMLPath, message("fileErrMsg")));
            }
        }
    }

    /**
     * This method add a servlet context listener in web.xml.
     */
    public void setAIServletContextListener() {
        if (webXMLDoc == null) {
            return;
        }
        try {
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final Element eleListenerMapping = (Element) xpath.evaluate(message("exprListener"), webXMLDoc, XPathConstants.NODE);
            if (eleListenerMapping == null) {
                final Element listenerMapping = webXMLDoc.createElement(message("listenerTag"));
                final Element listenerName = webXMLDoc.createElement(message("listenerClassEle"));
                listenerName.setTextContent(message("aiServletContextListener"));
                listenerMapping.appendChild(listenerName);

                final NodeList existingListenerNodeList = webXMLDoc.getElementsByTagName(message("listenerTag"));
                final Node existingListenerNode = existingListenerNodeList != null
                    && existingListenerNodeList.getLength() > 0
                    ? existingListenerNodeList.item(0) : null;

                webXMLDoc.getDocumentElement().insertBefore(listenerMapping, existingListenerNode);
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    /**
     * This method adds filter mapping tags in web.xml.
     */
    private void setFilterMapping() {
        if (webXMLDoc == null) {
            return;
        }
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFltMapping = (Element) xpath.evaluate(message("exprFltMapping"), webXMLDoc, XPathConstants.NODE);
            if (eleFltMapping == null) {
                Element filterMapping = webXMLDoc.createElement(message("filterMapTag"));
                Element filterName = webXMLDoc.createElement(message("filterEle"));
                filterName.setTextContent(message("aiWebfilter"));
                filterMapping.appendChild(filterName);

                Element urlPattern = webXMLDoc.createElement(message("urlPatternTag"));
                urlPattern.setTextContent("/*");
                filterMapping.appendChild(urlPattern);

                NodeList existingFilterMapNodeList = webXMLDoc.getElementsByTagName(message("filterMapTag"));
                Node existingFilterMapNode = existingFilterMapNodeList != null
                        & existingFilterMapNodeList.getLength() > 0 ? existingFilterMapNodeList
                        .item(0) : null;

                webXMLDoc.getDocumentElement().insertBefore(filterMapping, existingFilterMapNode);
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }

    }

    private void setFilterDef() {
        if (webXMLDoc == null) {
            return;
        }
        try {
            String exprFilter = message("aiExprConst");
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc,
                    XPathConstants.NODE);
            if (eleFilter == null) {
                Element filter = webXMLDoc.createElement(message("filterTag"));
                Element filterName = webXMLDoc.createElement(message("filterEle"));
                filterName.setTextContent(message("aiWebfilter"));
                filter.appendChild(filterName);

                Element fClass = webXMLDoc.createElement("filter-class");
                fClass.setTextContent(message("aiWebFilterClassName"));
                filter.appendChild(fClass);

                NodeList existingFilterNodeList = webXMLDoc.getElementsByTagName(message("filterTag"));
                Node existingFilterNode = existingFilterNodeList != null
                        & existingFilterNodeList.getLength() > 0 ? existingFilterNodeList
                        .item(0) : null;

                webXMLDoc.getDocumentElement().insertBefore(filter, existingFilterNode);
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    public void removeAIFilterDef() throws Exception {
        if (webXMLDoc == null) {
            return;
        }
        try {
            String exprFilter = message("aiExprConst");
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc, XPathConstants.NODE);

            if (eleFilter != null) {
                eleFilter.getParentNode().removeChild(eleFilter);
            }

            String exprFltMapping = message("exprFltMapping");
            Element eleFilMapping = (Element) xpath.evaluate(exprFltMapping, webXMLDoc, XPathConstants.NODE);
            if (eleFilMapping != null) {
                eleFilMapping.getParentNode().removeChild(eleFilMapping);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            AzurePlugin.log(ex.getMessage(), ex);
            throw new Exception(String.format("%s%s", message("aiRemoveErr"), ex.getMessage()));
        }
    }

    public void removeAIServletContextListener() throws Exception {
        if (webXMLDoc == null) {
            return;
        }
        try {
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final String exprListener = message("exprListener");
            final Element eleListener = (Element) xpath.evaluate(exprListener, webXMLDoc, XPathConstants.NODE);
            if (eleListener != null) {
                eleListener.getParentNode().removeChild(eleListener);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            AzurePlugin.log(ex.getMessage(), ex);
            throw new Exception(String.format("%s%s", message("aiRemoveErr"), ex.getMessage()));
        }
    }

    public void disableAIFilterConfiguration(boolean disable) {
        if (aiConfXMLDoc != null) {
            NodeList nodeList = aiConfXMLDoc.getElementsByTagName(DISABLE_TELEMETRY_NODE_NAME);

            if (nodeList != null && nodeList.getLength() > 0) {
                nodeList.item(0).setTextContent(disable + "");
            } else {
                Element disableTelemetry = aiConfXMLDoc.createElement(DISABLE_TELEMETRY_NODE_NAME);
                disableTelemetry.setTextContent(disable + "");
                aiConfXMLDoc.appendChild(disableTelemetry);
            }
        }
    }

    public void setAIFilterConfig() throws Exception {
        if (isAIWebFilterConfigured()) {
            return;
        }
        setFilterDef();
        setFilterMapping();
    }

    public void setAIInstrumentationKey(String instrumentationKey) {
        if (aiConfXMLDoc != null) {
            NodeList nodeList = aiConfXMLDoc.getElementsByTagName(INSTRUMENTATION_KEY_NODE_NAME);

            if (nodeList != null && nodeList.getLength() > 0) {
                nodeList.item(0).setTextContent(instrumentationKey);
            } else {
                Element instrumentationKeyElement = aiConfXMLDoc.createElement(INSTRUMENTATION_KEY_NODE_NAME);
                instrumentationKeyElement.setTextContent(instrumentationKey);
                aiConfXMLDoc.appendChild(instrumentationKeyElement);
            }
        }
    }

    public String getAIInstrumentationKey() {
        if (aiConfXMLDoc != null) {
            NodeList nodeList = aiConfXMLDoc.getElementsByTagName(INSTRUMENTATION_KEY_NODE_NAME);

            if (nodeList != null && nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        }
        return "";
    }
}
