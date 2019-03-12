package org.opendedup.sdfs.mgmt;

import org.opendedup.sdfs.io.Volume;

public class VolumnInfo {
    private String version;
    private Volume sdfsVolume;
    public VolumnInfo() {
    }

    public Volume getSdfsVolume() {
        return sdfsVolume;
    }

    public void setSdfsVolume(Volume sdfsVolume) {
        this.sdfsVolume = sdfsVolume;
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
                ", sdfsVolume=" + sdfsVolume +
                '}';
    }
}
