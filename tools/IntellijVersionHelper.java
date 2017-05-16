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
public class IntellijVersionHelper {

	public static void main(String argv[]) {
		if (argv.length < 1) {
			System.out.println("[Failed] Please specify the target intellij version for the plugin.");
			System.exit(1);
		}

		try {
			// Read the content from xml file
			String filepath = "../PluginsAndFeatures/azure-toolkit-for-intellij/resources/META-INF/plugin.xml";
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(filepath);

			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();
			XPathExpression expression =  xpath.compile("/idea-plugin/version/text()");
			Node versionNode = (Node) expression.evaluate(doc, XPathConstants.NODE);
			String oldVersion = versionNode.getNodeValue();
			String newVersion = oldVersion + "-" + argv[0];
			System.out.println("The old version is " + oldVersion);
			System.out.println("The new version is " + newVersion);
			versionNode.setNodeValue(newVersion);

			// Write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filepath));
			transformer.transform(source, result);

			System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
}
