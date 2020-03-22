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
package com.microsoft.azuretools.core.applicationinsights;

import java.io.File;
import java.io.IOException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.utils.TelemetryUtils;


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
            Activator.getDefault().log(e.getMessage(), e);
            throw new Exception(Messages.aiParseErrMsg);
        }
    }

    public void parseWebXmlPath(String webXmlPath) throws Exception {
        if (webXmlPath != null) {
            this.webXMLPath = webXmlPath;
            File xmlFile = new File(webXmlPath);
            if (xmlFile.exists()) {
                webXMLDoc = AIParserXMLUtility.parseXMLFile(webXmlPath);
            } else {
                throw new Exception(String.format("%s%s", webXmlPath,
                        Messages.fileErrMsg));
            }
        }
    }

    public boolean isAIWebFilterConfigured() throws Exception {
        if (webXMLDoc == null) {
            return false;
        }

        String exprFilter = Messages.exprConst;
        XPath xpath = XPathFactory.newInstance().newXPath();
        Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc,
                XPathConstants.NODE);
        if (eleFilter != null) {
            return true;
        }
        return false;
    }

    public void save() throws IOException, Exception {
        if (webXMLDoc != null) {
            AIParserXMLUtility.saveXMLFile(webXMLPath, webXMLDoc);
        }

        if (aiConfXMLDoc != null) {
            AIParserXMLUtility.saveXMLFile(aiConfXMLPath, aiConfXMLDoc);
        }
    }

    public void parseAIConfXmlPath(String aiConfXMLPath) throws Exception {
        if (aiConfXMLPath != null) {
            this.aiConfXMLPath = aiConfXMLPath;
            File xmlFile = new File(aiConfXMLPath);
            if (xmlFile.exists()) {
                aiConfXMLDoc = AIParserXMLUtility.parseXMLFile(aiConfXMLPath);
            } else {
                throw new Exception(String.format("%s%s", aiConfXMLPath,
                        Messages.fileErrMsg));
            }
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
            Element eleFltMapping = (Element) xpath.evaluate(
                    Messages.exprFltMapping, webXMLDoc, XPathConstants.NODE);
            if (eleFltMapping == null) {
                Element filterMapping = webXMLDoc
                        .createElement(Messages.filterMapTag);
                Element filterName = webXMLDoc
                        .createElement(Messages.filterEle);
                filterName.setTextContent(Messages.aiWebfilter);
                filterMapping.appendChild(filterName);

                Element urlPattern = webXMLDoc
                        .createElement(Messages.urlPatternTag);
                urlPattern.setTextContent("/*");
                filterMapping.appendChild(urlPattern);

                NodeList existingFilterMapNodeList = webXMLDoc
                        .getElementsByTagName(Messages.filterMapTag);
                Node existingFilterMapNode = existingFilterMapNodeList != null
                        & existingFilterMapNodeList.getLength() > 0 ? existingFilterMapNodeList
                        .item(0) : null;

                webXMLDoc.getDocumentElement().insertBefore(filterMapping, existingFilterMapNode);
            }
        } catch (Exception ex) {
            Activator.getDefault().log(ex.getMessage(), ex);
        }

    }

    private void setFilterDef() {
        if (webXMLDoc == null) {
            return;
        }
        try {
            String exprFilter = Messages.exprConst;
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc,
                    XPathConstants.NODE);
            if (eleFilter == null) {
                Element filter = webXMLDoc.createElement(Messages.filterTag);
                Element filterName = webXMLDoc
                        .createElement(Messages.filterEle);
                filterName.setTextContent(Messages.aiWebfilter);
                filter.appendChild(filterName);

                Element fClass = webXMLDoc.createElement("filter-class");
                fClass.setTextContent(Messages.aiWebFilterClassName);
                filter.appendChild(fClass);

                NodeList existingFilterNodeList = webXMLDoc
                        .getElementsByTagName(Messages.filterTag);
                Node existingFilterNode = existingFilterNodeList != null
                        & existingFilterNodeList.getLength() > 0 ? existingFilterNodeList
                        .item(0) : null;

                webXMLDoc.getDocumentElement().insertBefore(filter, existingFilterNode);
            }
        } catch (Exception ex) {
            Activator.getDefault().log(ex.getMessage(), ex);
        }
    }

    public void removeAIFilterDef() throws Exception {
        if (webXMLDoc == null) {
            return;
        }
        try {
            String exprFilter = Messages.exprConst;
            XPath xpath = XPathFactory.newInstance().newXPath();
            Element eleFilter = (Element) xpath.evaluate(exprFilter, webXMLDoc,
                    XPathConstants.NODE);

            if (eleFilter != null) {
                eleFilter.getParentNode().removeChild(eleFilter);
            }

            String exprFltMapping = Messages.exprFltMapping;
            Element eleFilMapping = (Element) xpath.evaluate(exprFltMapping,
                    webXMLDoc, XPathConstants.NODE);
            if (eleFilMapping != null) {
                eleFilMapping.getParentNode().removeChild(eleFilMapping);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Activator.getDefault().log(ex.getMessage(), ex);
            throw new Exception(String.format("%s%s", Messages.aiRemoveErr,
                    ex.getMessage()));
        }
    }

    /**
     * Workaround for issue https://github.com/Microsoft/azure-tools-for-java/issues/2122
     */
    public void setAIServletContextListener() {
        try {
            TelemetryUtils.setAIServletContextListener(webXMLDoc);
        } catch (Exception e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
    }


    /**
     * Workaround for issue https://github.com/Microsoft/azure-tools-for-java/issues/2122
     */
    public void removeAIServletContextListener() {
        try {
            TelemetryUtils.removeAIServletContextListener(webXMLDoc);
        } catch (Exception e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
    }


    public void disableAIFilterConfiguration(boolean disable) {
        if (aiConfXMLDoc != null) {
            NodeList nodeList = aiConfXMLDoc.getElementsByTagName(DISABLE_TELEMETRY_NODE_NAME);

            if (nodeList != null && nodeList.getLength() > 0) {
                nodeList.item(0).setTextContent(disable+"");
            } else {
                Element disableTelemetry = aiConfXMLDoc.createElement(DISABLE_TELEMETRY_NODE_NAME);
                disableTelemetry.setTextContent(disable+"");
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
