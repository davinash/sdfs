package org.opendedup.sdfs.mgmt;

import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.VolumeConfigWriter;
import org.opendedup.util.OSValidator;
import org.opendedup.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CreateVolumeWeb {
    public Element getResult(String options, String file) throws IOException {
        try {
            File f = new File(OSValidator.getConfigPath());
            if (!f.exists())
                f.mkdirs();

            VolumeConfigWriter wr = new VolumeConfigWriter();
            List<String> items = Arrays.asList(options.split("\\s*,\\s*"));
            wr.parseCmdLine(items.toArray(new String[items.size()]));
            wr.writeConfigFile();

            return (Element) this.toXMLDocument("Success").getDocumentElement()
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
