/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.microsoft.intellij.ui.messages.AzureBundle;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ParseXML {

    /**
     * Parses XML file and returns XML document.
     *
     * @param fileName XML file to parse
     * @return XML document or <B>null</B> if error occurred
     * @throws Exception object
     */
    static Document parseFile(String fileName) throws Exception {
        DocumentBuilder docBuilder;
        Document doc = null;
        DocumentBuilderFactory docBuilderFactory =
                DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Exception(AzureBundle.message("pXMLParseExcp"));
        }
        File sourceFile = new File(fileName);
        try {
            doc = docBuilder.parse(sourceFile);
        } catch (SAXException e) {
            throw new Exception(AzureBundle.message("pXMLParseExcp"));
        } catch (IOException e) {
            throw new Exception(AzureBundle.message("pXMLParseExcp"));
        }
        return doc;
    }

    /**
     * Save XML file and saves XML document.
     *
     * @param fileName
     * @param doc
     * @return boolean
     * @throws Exception object
     */
    static boolean saveXMLDocument(String fileName, Document doc)
            throws Exception {
        // open output stream where XML Document will be saved
        File xmlOutputFile = new File(fileName);
        FileOutputStream fos = null;
        Transformer transformer;
        try {
            fos = new FileOutputStream(xmlOutputFile);
            // Use a Transformer for output
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(fos);
            // transform source into result will do save
            transformer.transform(source, result);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

        return true;
    }

    /**
     * Replaces old project name with new project name in launch file.
     *
     * @param filePath
     * @param oldName
     * @param newName
     * @throws Exception
     */
    public static void setProjectNameinLaunch(String filePath,
                                              String oldName, String newName) throws Exception {
        Document doc = null;
        doc = parseFile(filePath);

        if (doc != null) {
            Node root = doc.getDocumentElement();
            if (root.hasChildNodes()) {
                for (Node child = root.getFirstChild(); child != null;
                     child = child.getNextSibling()) {
                    NamedNodeMap nMap = child.getAttributes();
                    if (nMap != null) {
                        if (nMap.getNamedItem("key").getNodeValue().equalsIgnoreCase(AzureBundle.message("pXMLProjAttr"))) {
                            nMap.getNamedItem("value").setNodeValue(newName);
                        } else if (nMap.getNamedItem("key").getNodeValue().equalsIgnoreCase(AzureBundle.message("pXMLAttrLoc"))) {
                            String value = nMap.getNamedItem("value").getNodeValue();
                            String workLoc = AzureBundle.message("pXMLWorkLoc");
                            value = value.replaceFirst(workLoc.concat(oldName), workLoc.concat(newName));
                            nMap.getNamedItem("value").setNodeValue(value);
                        } else if (nMap.getNamedItem("key").getNodeValue().equalsIgnoreCase(AzureBundle.message("pXMLAttrDir"))) {
                            String value = nMap.getNamedItem("value").getNodeValue();
                            String workLoc = AzureBundle.message("pXMLWorkLoc");
                            value = value.replaceFirst(workLoc.concat(oldName), workLoc.concat(newName));
                            nMap.getNamedItem("value").setNodeValue(value);
                        }
                    }
                }
            }
            saveXMLDocument(filePath, doc);
        }
    }
}
