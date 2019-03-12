package org.opendedup.sdfs.mgmt;

import org.opendedup.logging.SDFSLogger;
import org.opendedup.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class CreateVolumeWeb {
    public Element getResult(String cmd, String file) throws IOException {
        try {
            return (Element) this.toXMLDocument(cmd).getDocumentElement()
                    .cloneNode(true);
        } catch (Exception e) {
            SDFSLogger.getLog().error(
                    "unable to fulfill request on file " + file, e);
            throw new IOException("request to fetch attributes failed because "
                    + e.toString());
        }
    }
    private Document toXMLDocument(String messssage) throws ParserConfigurationException {
        Document doc = XMLUtils.getXMLDoc("create-volume");
        Element root = doc.getDocumentElement();
        root.setAttribute("Status", messssage);
        return doc;
    }
}
