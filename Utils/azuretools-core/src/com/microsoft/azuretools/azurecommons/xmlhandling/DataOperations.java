/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.xmlhandling;

import java.util.HashMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import com.microsoft.azure.toolkit.ide.common.util.ParserXMLUtility;
import org.w3c.dom.Document;

public class DataOperations {
    public static final String PROPERTY = "/data/property[@name='%s']";
    public static final String PROPERTY_VAL = "/data/property[@name='%s']/@value";

    /**
     * Method updates or creates property element.
     * @param doc
     * @param propertyName
     * @param value
     */
    public static void updatePropertyValue(Document doc, String propertyName, String value) {
        try {
            String nodeExpr = String.format(PROPERTY, propertyName);
            HashMap<String, String> nodeAttribites = new HashMap<String, String>();
            nodeAttribites.put("name", propertyName);
            nodeAttribites.put("value", value);
            ParserXMLUtility.updateOrCreateElement(doc, nodeExpr, "/data", "property", true, nodeAttribites);
        } catch (Exception ex) {
            // ignore
        }
    }

    /**
     * Method returns property value of particular property element.
     * @param dataFile
     * @param propName
     * @return
     */
    public static String getProperty(String dataFile, String propName) {
        String propVal = null;
        try {
            Document doc = ParserXMLUtility.parseXMLFile(dataFile);
            if (doc != null) {
                String nodeExpr = String.format(PROPERTY_VAL, propName);
                XPath xPath = XPathFactory.newInstance().newXPath();
                propVal = xPath.evaluate(nodeExpr, doc);
            }
        } catch (Exception ex) {
            // ignore
        }
        return propVal;
    }
}
