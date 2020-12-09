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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public final class ParserXMLUtility {

    private static final int BUFF_SIZE = 1024;
    public static final String INVALID_ARG = "Invalid argument.";

    /**
     * Parses XML file and returns XML document.
     *
     * @param fileName .
     * @return XML document or <B>null</B> if error occurred
     * @throws Exception
     */
    public static Document parseXMLFile(final String fileName)
            throws Exception {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ParserXMLUtility.class.getClassLoader());
            // fixes https://dev.azure.com/mseng/VSJava/_workitems/edit/1796447
            // refers https://jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_class_loaders.html
            DocumentBuilder docBuilder;
            Document doc = null;
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilder = docBuilderFactory.newDocumentBuilder();
            File xmlFile = new File(fileName);
            doc = docBuilder.parse(xmlFile);
            return doc;
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    /**
     * Parses Input Stream and returns XML document.
     *
     * @param inputStream .
     * @return XML document or <B>null</B> if error occurred
     * @throws Exception
     */
    protected static Document parseXMLResource(final InputStream inputStream)
            throws Exception {
        DocumentBuilder docBuilder;
        Document doc = null;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilder = docBuilderFactory.newDocumentBuilder();
        doc = docBuilder.parse(inputStream);
        return doc;
    }

    /**
     * save XML file and saves XML document.
     *
     * @param fileName
     * @param doc
     * @return XML document or <B>null</B> if error occurred
     * @throws IOException
     * @throws Exception
     */

    public static boolean saveXMLFile(String fileName, Document doc)
            throws Exception {
        File xmlFile = null;
        FileOutputStream fos = null;
        Transformer transformer;
        try {
            xmlFile = new File(fileName);
            fos = new FileOutputStream(xmlFile);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            transformer = transFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult destination = new StreamResult(fos);
            // transform source into result will do save
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, destination);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return true;
    }

    public static void copyDir(File source, final File destination)
            throws Exception {

        InputStream instream = null;
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] kid = source.list();
            for (int i = 0; i < kid.length; i++) {
                copyDir(new File(source, kid[i]), new File(destination, kid[i]));
            }
        } else {
            // InputStream instream = null;
            OutputStream out = null;
            try {
                instream = new FileInputStream(source);
                out = new FileOutputStream(destination);
                byte[] buf = new byte[BUFF_SIZE];
                int len = instream.read(buf);

                while (len > 0) {
                    out.write(buf, 0, len);
                    len = instream.read(buf);
                }
            } finally {
                if (instream != null) {
                    instream.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    public static void writeFile(InputStream inStream, OutputStream out)
            throws IOException {
        try {
            byte[] buf = new byte[BUFF_SIZE];
            int len = inStream.read(buf);
            while (len > 0) {
                out.write(buf, 0, len);
                len = inStream.read(buf);
            }
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (out != null) {
                out.close();
            }
        }

    }

    /**
     * Generic API to delete elements from DOM
     */
    public static void deleteElement(Document doc, String expr)
            throws XPathExpressionException {
        if (doc == null) {
            throw new IllegalArgumentException(INVALID_ARG);
        } else {
            XPath path = XPathFactory.newInstance().newXPath();
            Element element = (Element) path.evaluate(expr, doc,
                    XPathConstants.NODE);

            if (element != null) {
                Node parentNode = element.getParentNode();
                parentNode.removeChild(element);
            }

        }
    }

    /**
     * This API evaluates XPath expression and return the result as a String.
     */
    public static String getExpressionValue(Document doc, String expr)
            throws XPathExpressionException {
        if (doc == null || expr == null || expr.isEmpty()) {
            throw new IllegalArgumentException(INVALID_ARG);
        }

        XPath path = XPathFactory.newInstance().newXPath();
        return path.evaluate(expr, doc);
    }

    /**
     * This API evaluates XPath expression and sets the value.
     */
    public static void setExpressionValue(Document doc, String expr,
                                          String value) throws XPathExpressionException {
        if (doc == null || expr == null || expr.isEmpty()) {
            throw new IllegalArgumentException(INVALID_ARG);
        }

        XPath path = XPathFactory.newInstance().newXPath();
        Node node = (Node) path.evaluate(expr, doc, XPathConstants.NODE);
        node.setNodeValue(value);
    }

    protected static boolean isEpPortEqualOrInRange(String oldPort,
                                                    String newPort) {
        boolean isEqual = false;
        try {

            int oldMin = 0;
            int oldMax = 0;
            int newMin = 0;
            int newMax = 0;
            if (oldPort.contains("-")) {
                String[] rang = oldPort.split("-");
                oldMin = Integer.valueOf(rang[0]);
                oldMax = Integer.valueOf(rang[1]);
            } else {
                oldMax = Integer.valueOf(oldPort);
                oldMin = Integer.valueOf(oldPort);
            }

            if (newPort.contains("-")) {
                String[] rang = newPort.split("-");
                newMin = Integer.valueOf(rang[0]);
                newMax = Integer.valueOf(rang[1]);
            } else {
                newMax = Integer.valueOf(newPort);
                newMin = Integer.valueOf(newPort);
            }

            // check for newmin range is in between old range
            if ((newMin == oldMin) || newMin == oldMax
                    || (newMin > oldMin && newMin < oldMax)) {
                isEqual = true;
            } else if ((newMax == oldMin) || newMax == oldMax
                    || (newMax > oldMin && newMax < oldMax)) {
                // check for newmax range is in between old range
                isEqual = true;
            } else if ((oldMin > newMin && oldMin < newMax)) {
                // check for oldnim should not be in new range i.e. check for
                // overlapping
                isEqual = true;
            } else if (oldMax > newMin && oldMax < newMax) {
                isEqual = true;
            }

        } catch (Exception e) {
            isEqual = false;
        }
        return isEqual;
    }

    protected static boolean isValidRange(String range) {
        boolean isValid = true;
        try {
            String[] ports = range.split("-");
            int min = Integer.parseInt(ports[0]);
            int max = Integer.parseInt(ports[1]);
            if (min > max) {
                isValid = false;
            }
        } catch (Exception e) {
            isValid = false;
        }
        return isValid;
    }

    /**
     * Generic API to update or create DOM elements
     */
    public static Element updateOrCreateElement(Document doc, String expr,
                                                String parentNodeExpr, String elementName, boolean firstChild,
                                                Map<String, String> attributes)
            throws Exception {

        if (doc == null) {
            throw new IllegalArgumentException(INVALID_ARG);
        } else {
            XPath path = XPathFactory.newInstance().newXPath();
            Element element = null;
            if (expr != null) {
                element = (Element) path.evaluate(expr, doc,
                        XPathConstants.NODE);
            }

            // If element doesn't exist create one
            if (element == null) {
                element = doc.createElement(elementName);
                Element parentElement = (Element) path.evaluate(
                        parentNodeExpr, doc, XPathConstants.NODE);
                if (firstChild) {
                    parentElement.insertBefore(
                            element,
                            parentElement != null ? parentElement
                                    .getFirstChild() : null);
                } else {
                    parentElement.appendChild(element);
                }
            }

            if (attributes != null && !attributes.isEmpty()) {
                for (Map.Entry<String, String> attribute : attributes
                        .entrySet()) {
                    element.setAttribute(attribute.getKey(),
                            attribute.getValue());
                }
            }
            return element;
        }
    }

    /**
     * Generic API to update or create DOM elements
     */
    public static Element createElement(Document doc, String expr,
                                        Element parentElement, String elementName, boolean firstChild,
                                        Map<String, String> attributes)
            throws Exception {

        if (doc == null) {
            throw new IllegalArgumentException(INVALID_ARG);
        } else {
            XPath path = XPathFactory.newInstance().newXPath();
            Element element = null;
            if (expr != null) {
                element = (Element) path.evaluate(expr, doc,
                        XPathConstants.NODE);
            }

            // If element doesn't exist create one
            if (element == null) {
                element = doc.createElement(elementName);
                if (firstChild) {
                    parentElement.insertBefore(
                            element,
                            parentElement != null ? parentElement
                                    .getFirstChild() : null);
                } else {
                    parentElement.appendChild(element);
                }
            }

            if (attributes != null && !attributes.isEmpty()) {
                for (Map.Entry<String, String> attribute : attributes
                        .entrySet()) {
                    element.setAttribute(attribute.getKey(),
                            attribute.getValue());
                }
            }
            return element;
        }
    }

    /**
     * API checks if a node is already present in the XML document
     *
     * @param doc
     * @param nodeExpression
     * @throws Exception
     */
    public static boolean doesNodeExists(Document doc, String nodeExpression) throws Exception {
        if (nodeExpression == null) {
            throw new IllegalArgumentException(INVALID_ARG);
        } else {
            XPath path = XPathFactory.newInstance().newXPath();
            Element element = (Element) path.evaluate(nodeExpression, doc, XPathConstants.NODE);
            return element != null;
        }
    }
}
