import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

// Run the program via the cli command "java VersionHelper 2016.3"
// This program will append intellij version to the plugin version.
public class XMLHelper {

	public static void main(String argv[]) {
		if (argv.length < 4) {
			System.out.println("[Failed] Please specify the arguments $XML_FILE $XPATH $NEW_VALUE $JOIN_OR_REPLACE.");
			System.exit(1);
		}

		String FILEPATH = argv[0];
		String XPATH = argv[1];
		String NEW_VALUE = argv[2];
		String CHANGETYPE = argv[3];
		boolean DISPLAY_LOG = false;
		if (argv.length > 4 && argv[4].toLowerCase().equals("true")) {
			DISPLAY_LOG = true;
		}

		try {
			System.out.println("Starting to modify XML " + FILEPATH);
			
			// Read the content from xml file
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(FILEPATH);

			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();
			XPathExpression expression =  xpath.compile(XPATH);
			Node targetNode = (Node) expression.evaluate(doc, XPathConstants.NODE);
			String oldValue = targetNode.getNodeValue();
			String newValue = "";
			if (CHANGETYPE.equals("JOIN")) {
				newValue = oldValue + NEW_VALUE;
			}
			if (DISPLAY_LOG) {
				System.out.println("The old value is " + oldValue);
				System.out.println("The new value is " + newValue);
			}
			targetNode.setNodeValue(newValue);

			// Write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(FILEPATH));
			transformer.transform(source, result);

			System.out.println("Modify XML Finished.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
}
