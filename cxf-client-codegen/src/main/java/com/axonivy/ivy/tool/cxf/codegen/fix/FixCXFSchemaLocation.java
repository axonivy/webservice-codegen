package com.axonivy.ivy.tool.cxf.codegen.fix;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FixCXFSchemaLocation {
  private static final Logger LOGGER = LoggerFactory.getLogger(FixCXFSchemaLocation.class);

  /**
   * ISSUE CXF-7706 (https://issues.apache.org/jira/browse/CXF-7706)
   * <ol>
   * <li>Searches for the service definition WSDL in the generated client
   * directory.</li>
   * <li>If the WSDL contains an import where the schema location is an empty
   * string it removes the schema location attribute.
   * (e.g. &lt;import namespace="http://schemas.xmlsoap.org/soap/encoding/"
   * schemaLocation=""/ &gt;)</li>
   * <li>Writes the modified WSDL (only if a modification was performed)</li>
   * </ol>
   *
   * @param tmpGenDir
   * @throws IOException
   */
  public static void fixLocalWsdlIfNecessary(Path tmpGenDir) throws IOException {
    try (Stream<Path> walker = Files.walk(tmpGenDir, 1)) {
      walker.filter(FixCXFSchemaLocation::isSchemaFile)
          .forEach(schemaPath -> {
            try {
              Document doc = readWsdl(schemaPath);
              boolean hasModifiedWsdl = modifyEmptySchemaLocation(doc);
              if (hasModifiedWsdl) {
                writeWsdl(schemaPath, doc);
              }
            } catch (Exception ex) {
              LOGGER.warn("Failed to modify schemaLocation in service definition files", ex);
            }
          });
    }
  }

  private static void writeWsdl(Path schemaPath, Document doc) throws TransformerConfigurationException,
      TransformerFactoryConfigurationError, TransformerException, IOException {
    try (var out = Files.newOutputStream(schemaPath, CREATE, WRITE)) {
      var factory = TransformerFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      var transformer = factory.newTransformer();
      transformer.transform(new DOMSource(doc), new StreamResult(out));
    }
  }

  private static Document readWsdl(Path schemaPath) throws SAXException, IOException, ParserConfigurationException {
    try (var in = Files.newInputStream(schemaPath)) {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      return factory.newDocumentBuilder().parse(in);
    }
  }

  private static boolean modifyEmptySchemaLocation(Document doc) throws XPathExpressionException {
    XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();
    XPathExpression expr = xpath.compile("//*[local-name()='import']"); // XPath: Searches for all :import tags below the root (finds <xs:import, <import, etc.)
    NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

    boolean hasModifiedWsdl = false;
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node importNode = nodeList.item(i);
      Node schemaLocation = importNode.getAttributes().getNamedItem("schemaLocation");
      if (schemaLocation != null && schemaLocation.getNodeValue() != null && schemaLocation.getNodeValue().isBlank()) {
        Element importElement = ((Attr) schemaLocation).getOwnerElement();
        importElement.removeAttribute("schemaLocation");

        hasModifiedWsdl = true;
      }
    }
    return hasModifiedWsdl;
  }

  private static boolean isSchemaFile(Path zipPath) {
    String entry = zipPath.toString().toLowerCase();
    return entry.endsWith(".wsdl") || entry.endsWith(".xsd");
  }

}
