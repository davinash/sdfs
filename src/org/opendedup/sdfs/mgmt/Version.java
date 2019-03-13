package org.opendedup.sdfs.mgmt;

import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Main;
import org.opendedup.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Version {
    public Element getResult(String cmd, String file) throws IOException {
        System.out.println("Main.version = " + Main.version);
        try {
            return (Element) this.toXMLDocument(Main.version).getDocumentElement()
                    .cloneNode(true);
        } catch (Exception e) {
            e.printStackTrace();
            SDFSLogger.getLog().error(
                    "unable to fulfill request on file " + file, e);
            throw new IOException("request to fetch attributes failed because "
                    + e.toString());
        }
    }
    private Document toXMLDocument(final String version) throws ParserConfigurationException {
        System.out.println("Version::toXMLDocument::version = [" + version + "]");
        Document doc = XMLUtils.getXMLDoc("version");
        Element root = doc.getDocumentElement();
        root.setAttribute("version", version);
        System.out.println("root = " + root);
        System.out.println("doc = " + doc);
        return doc;
    }
}
