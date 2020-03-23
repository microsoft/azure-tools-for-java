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
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.microsoft.azuretools.core.Activator;


public class AIParserXMLUtility {

    /**
     * Parses XML file and returns XML document.
     *
     * @param fileName
     *            .
     * @return XML document or <B>null</B> if error occurred
     * @throws WindowsAzureInvalidProjectOperationException
     */
    public static Document parseXMLFile(final String fileName)
            throws Exception {
        try {
            DocumentBuilder docBuilder;
            Document doc = null;
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilder = docBuilderFactory.newDocumentBuilder();
            File xmlFile = new File(fileName);
            doc = docBuilder.parse(xmlFile);
            return doc;
        } catch (Exception e) {
            Activator.getDefault().log(e.getMessage(), e);
            throw new Exception(String.format("%s%s", Messages.aiParseErrMsg,
                    e.getMessage()));
        }

    }

    /**
     * save XML file and saves XML document.
     *
     * @param fileName
     * @param doc
     * @return XML document or <B>null</B> if error occurred
     * @throws IOException
     * @throws WindowsAzureInvalidProjectOperationException
     */
    protected static boolean saveXMLFile(String fileName, Document doc)
            throws IOException, Exception {

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
        } catch (Exception excp) {
            Activator.getDefault().log(excp.getMessage(), excp);
            throw new Exception(String.format("%s%s", Messages.saveErrMsg,
                    excp.getMessage()));
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return true;
    }
}
