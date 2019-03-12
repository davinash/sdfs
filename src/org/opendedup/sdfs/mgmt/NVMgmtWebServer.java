package org.opendedup.sdfs.mgmt;

import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.AbstractStreamMatcher;
import org.opendedup.sdfs.io.Volume;
import org.opendedup.sdfs.mgmt.websocket.PingService;
import org.opendedup.util.FindOpenPort;
import org.simpleframework.common.buffer.FileAllocator;
import org.simpleframework.http.Path;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.http.socket.service.PathRouter;
import org.simpleframework.http.socket.service.Router;
import org.simpleframework.http.socket.service.RouterContainer;
import org.simpleframework.http.socket.service.Service;
import org.simpleframework.transport.SocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class NVMgmtWebServer implements Container {
    private static Connection connection = null;
    public static Io io = null;
    public static AbstractStreamMatcher matcher = null;
    public static int sdfsCliPort = 5442;
    public static String sdfsCliListenAddr = "localhost";

    public static int writeThreads = (short) (Runtime.getRuntime()
            .availableProcessors() * 3);

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        new NVMgmtWebServer().start();
    }

    Map<String, VolumnInfo> volumeInfoMap = new HashMap<>();

    private static Map<String, String> splitQuery(String query) {

        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        if (query != null) {
            try {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            } catch (Exception e) {
                SDFSLogger.getLog().error("unable to parse " + query, e);
            }
        }
        return query_pairs;
    }

    @Override
    public void handle(Request request, Response response) {
        try {
            Path reqPath = request.getPath();
            String[] parts = request.getTarget().split("\\?");
            Map<String, String> qry = null;
            if (parts.length > 1) {
                qry = splitQuery(parts[1]);
            } else {
                qry = splitQuery(null);
            }
            boolean cmdReq = reqPath.getPath().trim().equalsIgnoreCase("/");


        } catch (Exception e) {
            SDFSLogger.getLog().error("unable to satify request ", e);
            response.setCode(500);
            try {
                PrintStream body = response.getPrintStream();
                body.println(e.toString());
                body.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                SDFSLogger.getLog().error("unable to satify request ", e1);
            }
            SDFSLogger.getLog().error("unable to satify request ", e);
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                SDFSLogger.getLog().debug("error when closing response", e);
            }
        }
    }

    private void start() throws IOException, ParserConfigurationException, SAXException {
        SSLContext sslContext = null;
        Map<String, Service> routes = new HashMap<String, Service>();
        //routes.put("/metadatasocket", new MetaDataUpdate());
        //routes.put("/ddbsocket", new DDBUpdate());
        if (matcher != null) {
            matcher.start();
            routes.put(matcher.getWSPath(), matcher);
        }
        //routes.put("/uploadsocket", new MetaDataUpload());
        routes.put("/ping", new PingService());
        Router negotiator = new PathRouter(routes, new PingService());
//        if (!Main.blockDev)
//            io = new Io(Main.volume.getPath(), Main.volumeMountPoint);
        Container container = this;
        RouterContainer rn = new RouterContainer(container, negotiator, writeThreads);
        SocketProcessor server = new ContainerSocketProcessor(rn, new FileAllocator(1024 * 1024 * 8), writeThreads, 4);
        connection = new SocketConnection(server);
        sdfsCliPort = FindOpenPort.pickFreePort(sdfsCliPort);
        SocketAddress address = new InetSocketAddress(sdfsCliListenAddr, sdfsCliPort);
        connection = new SocketConnection(server);
        if (sslContext != null)
            connection.connect(address, sslContext);
        else
            connection.connect(address);

        initVolumnMap();
        this.volumeInfoMap.forEach((k,v) -> System.out.println(k + " = " + v ));
        System.out.println("###################### NetVault SSL Management WebServer Started at "
                + address.toString() + " #########################");
    }

    private void initVolumnMap() throws IOException, SAXException, ParserConfigurationException {
        try {
            File dir = new File("/etc/sdfs");
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith("-volume-cfg.xml");
                }
            });
            final String SUFFIX_TO_REMOVE = "-volume-cfg.xml";

            for (File file : files) {
                String fileName = file.getName();
                String volName = fileName.substring(0, fileName.length() - SUFFIX_TO_REMOVE.length());
                volumeInfoMap.put(volName, parseVolumeConfigFile(file));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private VolumnInfo parseVolumeConfigFile(File file) throws ParserConfigurationException, IOException, SAXException {
        System.out.println("file = [" + file + "]");
        VolumnInfo volumnInfo = new VolumnInfo();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();
        String version = "0.8.12";
        SDFSLogger.getLog().info("############ Running SDFS Version " + Main.version);
        if (doc.getDocumentElement().hasAttribute("version")) {
            version = doc.getDocumentElement().getAttribute("version");
            volumnInfo.setVersion(version);
        }
        Element cli = (Element) doc.getElementsByTagName("sdfscli").item(0);
        volumnInfo.setPort(Integer.parseInt(cli.getAttribute("port")));
        return volumnInfo;
    }
}
