package org.opendedup.sdfs.mgmt;

import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.io.AbstractStreamMatcher;
import org.opendedup.sdfs.io.Volume;
import org.opendedup.sdfs.mgmt.websocket.DDBUpdate;
import org.opendedup.sdfs.mgmt.websocket.MetaDataUpdate;
import org.opendedup.sdfs.mgmt.websocket.MetaDataUpload;
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

import javax.net.ssl.SSLContext;
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

    public static void main(String[] args) throws IOException {
        new NVMgmtWebServer().start();
    }

    Map<String, Volume> volumeMap =  new HashMap<>();

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

    private void start() throws IOException {
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
        Container container = new MgmtWebServer();
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
        System.out.println("###################### NetVault SSL Management WebServer Started at "
                + address.toString() + " #########################");
    }
}
