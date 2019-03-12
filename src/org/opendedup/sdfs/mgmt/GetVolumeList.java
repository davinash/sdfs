package org.opendedup.sdfs.mgmt;

import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Main;
import org.opendedup.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetVolumeList {
    public Element getResult(String cmd, String file) throws IOException {
        try {
            File dir = new File("/etc/sdfs");
            File [] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith("-volume-cfg.xml");
                }
            });
            return (Element) this.toXMLDocument(files).getDocumentElement()
                    .cloneNode(true);
        } catch (Exception e) {
            SDFSLogger.getLog().error(
                    "unable to fulfill request on file " + file, e);
            throw new IOException("request to fetch attributes failed because "
                    + e.toString());
        }
    }

    private Document toXMLDocument(File[] files) throws ParserConfigurationException {
        List<String> volumeNames = new ArrayList<>();
        for (File file : files) {
            volumeNames.add(file.getName());
        }
        Document doc = XMLUtils.getXMLDoc("volumes");
        Element root = doc.getDocumentElement();
        root.setAttribute("volume-list", String.join(",", volumeNames));
        return doc;
    }
}
