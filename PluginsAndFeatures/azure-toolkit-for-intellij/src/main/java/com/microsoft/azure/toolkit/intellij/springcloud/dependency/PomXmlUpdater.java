/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.dependency;

import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.intellij.util.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.*;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultText;
import org.xml.sax.Locator;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PomXmlUpdater {
    private static String formatElements(String originalXml, LocationAwareElement parent, List<Element> newNodes) {
        if (newNodes.isEmpty()) {
            return originalXml;
        }
        final String[] originXmlLines = TextUtils.splitLines(originalXml);
        final String baseIndent = Utils.calcXmlIndent(originXmlLines, parent.getLineNumber() - 1,
                parent.getColumnNumber() - 2);
        final String placeHolder = String.format("@PLACEHOLDER_RANDOM_%s@", RandomUtils.nextLong());
        final Text placeHolderNode = new DefaultText("\n" + placeHolder);
        // replace target node to placeholder
        Element newNode = newNodes.get(0);
        parent.content().replaceAll(t -> t == newNode ? placeHolderNode : t);
        newNode.setParent(null);
        // remove all spaces before target node
        XmlUtils.trimTextBeforeEnd(parent, placeHolderNode);
        final String xmlWithPlaceholder = parent.getDocument().asXML();

        final List<String> newXmlLines = new ArrayList(
                Arrays.asList(TextUtils.splitLines(XmlUtils.prettyPrintElementNoNamespace(newNode))));
        for (int i = 1; i < newNodes.size(); i++) {
            newXmlLines.addAll(Arrays.asList(TextUtils.splitLines(XmlUtils.prettyPrintElementNoNamespace(newNodes.get(i)))));
        }

        final String replacement = newXmlLines.stream().map(t -> baseIndent + "    " + t)
                .collect(Collectors.joining("\n")) + "\n" + baseIndent;
        return xmlWithPlaceholder.replace(placeHolder, replacement);
    }

    private static String formatElement(String originalXml, LocationAwareElement parent, Element newNode) {
        return formatElements(originalXml, parent, Arrays.asList(newNode));
    }

    private Element createOrUpdateDependency(Element dependenciesNode, String groupId, String artifactId,
                                             String version) {
        Element targetNode = null;
        for (final Element element : dependenciesNode.elements()) {
            final String thisGroupId = XmlUtils.getChildValue(element, "groupId");
            final String thisArtifactId = XmlUtils.getChildValue(element, "artifactId");

            if (StringUtils.equals(groupId, thisGroupId) && StringUtils.equals(thisArtifactId, artifactId)) {
                targetNode = element;
            }
        }

        if (targetNode != null) {
            final String thisVersion = XmlUtils.getChildValue(targetNode, "version");
            if (StringUtils.equals(thisVersion, version) || (StringUtils.isEmpty(thisVersion) && StringUtils.isEmpty(version))) {
                // no need to update
                return null;
            }
            if (StringUtils.isNotEmpty(version)) {
                updateText(targetNode, "version", version);
            } else {
                Element versionEle = targetNode.element("version");
                if (versionEle != null) {
                    targetNode.remove(versionEle);
                }
            }
        } else {
            targetNode = addDependency(dependenciesNode, groupId, artifactId, version);
        }
        return targetNode;
    }

    public boolean updateDependencies(final File pom, List<DependencyArtifact> apply)
            throws DocumentException, IOException {
        final SAXReader reader = new CustomSAXReader();
        reader.setDocumentFactory(new LocatorAwareDocumentFactory());
        final Document doc = reader.read(new InputStreamReader(new FileInputStream(pom)));
        final Element dependenciesNode = createToPath(doc.getRootElement(), "dependencies");
        List<Element> newNodes = new ArrayList<>();
        for (DependencyArtifact da : apply) {
            Element newNode = createOrUpdateDependency(dependenciesNode, da.getGroupId(), da.getArtifactId(),
                    da.getCompatibleVersion());
            if (newNode != null) {
                newNodes.add(newNode);
            }
        }
        if (newNodes.isEmpty()) {
            return false;
        }
        if (dependenciesNode instanceof LocationAwareElement) {
            for (int i = 1; i < newNodes.size(); i++) {
                dependenciesNode.remove(newNodes.get(i));
            }
            FileUtils.writeStringToFile(pom, formatElements(FileUtils.readFileToString(pom, "utf-8"),
                    (LocationAwareElement) dependenciesNode, newNodes), "utf-8");
        } else {
            FileUtils.writeStringToFile(pom, formatElement(FileUtils.readFileToString(pom, "utf-8"),
                    (LocationAwareElement) dependenciesNode.getParent(), dependenciesNode), "utf-8");
        }
        return true;
    }

    private static void updateText(Element node, String key, String text) {
        if (node.element(key) != null) {
            node.element(key).setText(text);
        } else {
            addDomWithKeyValue(node, key, text);
        }
    }

    private static Element addDependency(Element rootNode, String groupId, String artifactId, String version) {
        final Element result = new DOMElement("dependency");
        addDomWithKeyValue(result, "groupId", groupId);
        addDomWithKeyValue(result, "artifactId", artifactId);
        if (StringUtils.isNotEmpty(version)) {
            addDomWithKeyValue(result, "version", version);
        }
        rootNode.add(result);
        return result;
    }

    public static void addDomWithKeyValue(Element node, String key, Object value) {
        final DOMElement newNode = new DOMElement(key);
        if (value != null) {
            newNode.setText(value.toString());
        }
        node.add(newNode);
    }

    private static Element createToPath(Element node, String... paths) {
        Element target = node;
        for (final String path : paths) {
            Element newNode = target.element(path);
            if (newNode == null) {
                newNode = new DOMElement(path);
                target.add(newNode);
            }
            target = newNode;
        }
        return target;
    }


    // code copied from https://stackoverflow.com/questions/36006819/use-dom4j-to-locate-the-node-with-line-number
    static class CustomSAXReader extends SAXReader {
        @Override
        protected SAXContentHandler createContentHandler(XMLReader reader) {
            return new CustomSAXContentHandler(getDocumentFactory(), getDispatchHandler());
        }

        @Override
        public void setDocumentFactory(DocumentFactory documentFactory) {
            super.setDocumentFactory(documentFactory);
        }

    }

    static class CustomSAXContentHandler extends SAXContentHandler {
        // this is already in SAXContentHandler, but private
        private DocumentFactory documentFactory;

        public CustomSAXContentHandler(DocumentFactory documentFactory, ElementHandler elementHandler) {
            super(documentFactory, elementHandler);
            this.documentFactory = documentFactory;
        }

        @Override
        public void setDocumentLocator(Locator documentLocator) {
            super.setDocumentLocator(documentLocator);
            if (documentFactory instanceof LocatorAwareDocumentFactory) {
                ((LocatorAwareDocumentFactory) documentFactory).setLocator(documentLocator);
            }

        }
    }

    static class LocatorAwareDocumentFactory extends DocumentFactory {
        private static final long serialVersionUID = 7388661832037675334L;
        private Locator locator;

        public LocatorAwareDocumentFactory() {
            super();
        }

        public void setLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public Element createElement(QName qname) {
            final LocationAwareElement element = new LocationAwareElement(qname);
            if (locator != null) {
                element.setLineNumber(locator.getLineNumber());
                element.setColumnNumber(locator.getColumnNumber());
            }
            return element;
        }

    }

    /**
     * An Element that is aware of it location (line number in) in the source document
     */
    static class LocationAwareElement extends DefaultElement {
        private static final long serialVersionUID = 260126644771458700L;
        private int lineNumber;
        private int columnNumber;

        public LocationAwareElement(QName qname) {
            super(qname);
        }

        public LocationAwareElement(QName qname, int attributeCount) {
            super(qname, attributeCount);

        }

        public LocationAwareElement(String name, Namespace namespace) {
            super(name, namespace);

        }

        public LocationAwareElement(String name) {
            super(name);

        }

        /**
         * @return the lineNumber
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * @param lineNumber the lineNumber to set
         */
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        /**
         * @return the columnNumber
         */
        public int getColumnNumber() {
            return columnNumber;
        }

        /**
         * @param columnNumber the columnNumber to set
         */
        public void setColumnNumber(int columnNumber) {
            this.columnNumber = columnNumber;
        }

    }
}
