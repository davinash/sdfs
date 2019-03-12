package org.opendedup.sdfs.mgmt;

public class VolumnInfo {
    private String version;
    private int port;
    private String listenAddrss;


    public VolumnInfo() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "VolumnInfo{" +
                "version='" + version + '\'' +
                ", port=" + port +
                ", listenAddrss='" + listenAddrss + '\'' +
                '}';
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getListenAddrss() {
        return listenAddrss;
    }

    public void setListenAddrss(String listenAddrss) {
        this.listenAddrss = listenAddrss;
    }
}
