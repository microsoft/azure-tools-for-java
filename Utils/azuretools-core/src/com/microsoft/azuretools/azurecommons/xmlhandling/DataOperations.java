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

package com.microsoft.azuretools.azurecommons.xmlhandling;

import java.util.HashMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import com.microsoft.azuretools.azurecommons.util.ParserXMLUtility;
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
